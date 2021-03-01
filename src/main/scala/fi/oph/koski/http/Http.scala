package fi.oph.koski.http

import java.net.URLEncoder

import fi.oph.koski.executors.Pools
import fi.oph.koski.http.Http.{Decode, ParameterizedUriWrapper}
import fi.oph.koski.json.JsonSerializer
import fi.oph.koski.log.LogUtils.maskSensitiveInformation
import fi.oph.koski.log.{LoggerWithContext, Logging}

import io.prometheus.client.{Counter, Summary}
import org.http4s._
import org.http4s.client.blaze.BlazeClientConfig
import org.http4s.client.{Client, blaze}
import org.http4s.headers.`Content-Type`
import org.json4s.jackson.JsonMethods.parse

import scala.concurrent.duration.DurationInt
import scala.reflect.runtime.universe.TypeTag
import scala.xml.Elem
import cats._
import cats.effect._
import cats.implicits._

object Http extends Logging {
  private val maxHttpConnections = Pools.jettyThreads + Pools.httpThreads
  def newClient(name: String, defaultConfig: BlazeClientConfig = BlazeClientConfig.defaultConfig): Client[IO] = {
    logger.info(s"Creating new pooled http client with $maxHttpConnections max total connections for $name")
    blaze.Http1Client[IO](config = defaultConfig.copy(
      maxTotalConnections = maxHttpConnections,
      requestTimeout = 10.minutes,
      //customExecutor = Some(Pools.httpPool),
      responseHeaderTimeout = 30.seconds
    ))
  }

  // This guys allows you to make URIs from your Strings as in uri"http://google.com/s=${searchTerm}"
  // Takes care of URI encoding the components. You can prevent encoding a part by wrapping into an Uri using this selfsame method.
  implicit class UriInterpolator(val sc: StringContext) extends AnyVal {
    def uri(args: Any*): ParameterizedUriWrapper = {
      val pairs: Seq[(String, Option[Any])] = sc.parts.zip(args.toList.map(Some(_)) ++ List(None))

      val parameterized: String = pairs.map { case (fixedPart, encodeablePartOption) =>
          fixedPart + (encodeablePartOption match {
            case None => ""
            case Some(encoded: ParameterizedUriWrapper) => encoded.uri.toString
            case Some(encoded: Uri) => encoded.toString
            case Some(encodeable) => URLEncoder.encode(encodeable.toString, "UTF-8")
          })
      }.mkString("")
      val template: String = pairs.map { case (fixedPart, encodeablePartOption) =>
        fixedPart + (encodeablePartOption match {
          case None => ""
          case Some(_) => "_"
        })
      }.mkString("").replaceAll("__", "_")

      ParameterizedUriWrapper(uriFromString(parameterized), template)
    }
  }

  // Warning: does not do any URI encoding
  // For when you don't want encoding but Http.scala wants a ParameterizedUriWrapper
  implicit class StringToUriConverter(s: String) {
    def toUri: ParameterizedUriWrapper = ParameterizedUriWrapper(uriFromString(s), s)
  }

  case class ParameterizedUriWrapper(uri: Uri, template: String)

  // Warning: does not do any URI encoding
  private def uriFromString(uri: String): Uri = {
    Uri.fromString(uri) match {
      case Right(result) => result
      case Left(failure) =>
        throw new IllegalArgumentException("Cannot create URI: " + uri + ": " + failure)
    }
  }

  def expectSuccess(status: Int, text: String, request: Request[IO]): Unit = (status, text) match {
    case (status, text) if status < 300 && status >= 200 =>
    case (status, text) => throw HttpStatusException(status, text, request)
  }

  def parseJson[T : TypeTag](status: Int, text: String, request: Request[IO])(implicit mf : scala.reflect.Manifest[T]): T = {
    (status, text) match {
      case (status, text) if (List(200, 201).contains(status)) => JsonSerializer.extract[T](parse(text), ignoreExtras = true)
      case (status, text) => throw HttpStatusException(status, text, request)
    }
  }

  def parseXml(status: Int, text: String, request: Request[IO]) = {
    (status, text) match {
      case (200, text) => scala.xml.XML.loadString(text)
      case (status, text) => throw HttpStatusException(status, text, request)
    }
  }

  /** Parses as JSON, returns None on 404 result */
  def parseJsonOptional[T](status: Int, text: String, request: Request[IO])(implicit mf : scala.reflect.Manifest[T]): Option[T] = (status, text) match {
    case (404, _) => None
    case (200, text) => Some(JsonSerializer.extract[T](parse(text), ignoreExtras = true))
    case (status, text) => throw HttpStatusException(status, text, request)
  }

  def toString(status: Int, text: String, request: Request[IO]) = (status, text) match {
    case (200, text) => text
    case (status, text) => throw HttpStatusException(status, text, request)
  }

  def statusCode(status: Int, text: String, request: Request[IO]) = (status, text) match {
    case (code, _) => code
  }

  val unitDecoder: Decode[Unit] =  {
    case (status, text, request) if (status >= 300) => throw HttpStatusException(status, text, request)
    case _ =>
  }

  // Http task runner: runs at most 2 minutes. We must avoid using the timeout-less run method, that may block forever.
  def runTask[A](task: IO[A]): A = task.unsafeRunTimed(2.minutes) // TODO: unsafeRunTimed ei ole tarkoitettu tuotantokoodille

  type Decode[ResultType] = (Int, String, Request[IO]) => ResultType

  object Encoders {
    def xml: EntityEncoder[IO, Elem] = EntityEncoder.stringEncoder[IO](implicitly[Applicative[IO]], Charset.`UTF-8`).contramap[Elem](item => item.toString)
      .withContentType(`Content-Type`(MediaType.`text/xml`))
    def formData: EntityEncoder[IO, String] = EntityEncoder.stringEncoder[IO].withContentType(`Content-Type`(MediaType.`application/x-www-form-urlencoded`))
  }

  def apply(root: String, name: String): Http = Http(root, newClient(name))

}

case class Http(root: String, client: Client[IO]) extends Logging {
  import fi.oph.koski.http.Http.UriInterpolator
  private val rootUri = Http.uriFromString(root)

  def get[ResultType](uri: ParameterizedUriWrapper, headers: Headers = Headers.empty)(decode: Decode[ResultType]): IO[ResultType] = {
    processRequest(Request[IO](uri = uri.uri, headers = headers), uri.template)(decode)
  }

  def head[ResultType](uri: ParameterizedUriWrapper, headers: Headers = Headers.empty)(decode: Decode[ResultType]): IO[ResultType] = {
    processRequest(Request[IO](uri = uri.uri, headers = headers, method = Method.HEAD), uri.template)(decode)
  }

  def post[I <: AnyRef, O <: Any](path: ParameterizedUriWrapper, entity: I)(encode: EntityEncoder[IO, I])(decode: Decode[O]): IO[O] = {
    val request = Request[IO](uri = path.uri, method = Method.POST)
    processRequest(requestTask = request.withBody(entity)(implicitly[Monad[IO]], encode), uriTemplate = path.template)(decode)
  }

  def post[ResultType](uri: ParameterizedUriWrapper, headers: Headers)(decode: Decode[ResultType]): IO[ResultType] = {
    processRequest(Request[IO](uri = uri.uri, method = Method.POST, headers = headers), uri.template)(decode)
  }

  def put[I <: AnyRef, O <: Any](path: ParameterizedUriWrapper, entity: I)(encode: EntityEncoder[IO, I])(decode: Decode[O]): IO[O] = {
    val request = Request[IO](uri = path.uri, method = Method.PUT)
    processRequest(request.withBody(entity)(implicitly[Monad[IO]], encode), path.template)(decode)
  }

  private def processRequest[ResultType](requestTask: IO[Request[IO]], uriTemplate: String)(decode: Decode[ResultType]): IO[ResultType] = {
    requestTask.flatMap(request => processRequest(request, uriTemplate)(decode))
  }

  private def processRequest[ResultType](request: Request[IO], uriTemplate: String)(decoder: (Int, String, Request[IO]) => ResultType): IO[ResultType] = {
    val requestWithFullPath: Request[IO] = request.withUri(addRoot(request.uri))
    val logger = HttpResponseLog(requestWithFullPath, root + uriTemplate)
    client.fetch(addCommonHeaders(requestWithFullPath)) { response =>
      logger.log(response)
      response.as[String].map { text => // Might be able to optimize by not turning into String here
        decoder(response.status.code, text, request)
      }
    }.handleErrorWith {
      case e: HttpStatusException =>
        logger.log(e)
        throw e
      case e: Exception =>
        logger.log(e)
        throw HttpConnectionException(e.getClass.getName + ": " + e.getMessage, requestWithFullPath)
    }
  }

  private def addRoot(uri: Uri): Uri = {
    if (!uri.toString.startsWith("http")) {
      uri"${rootUri}${uri}".uri
    } else {
      uri
    }
  }

  private def addCommonHeaders(request: Request[IO]) = request.withHeaders(request.headers.put(
    Header("Caller-Id", OpintopolkuCallerId.koski)
  ))
}

protected object HttpResponseLog {
  val logger = LoggerWithContext(classOf[Http])
}

protected case class HttpResponseLog(request: Request[IO], uriTemplate: String) {
  private val logFailedRequestBodiesForUrisContaining = "___none___"
  private val started = System.currentTimeMillis
  def elapsedMillis = System.currentTimeMillis - started
  def log(response: Response[IO]) {
    log(response.status.code.toString, response.status.code < 400)
    HttpResponseMonitoring.record(request, uriTemplate, response.status.code, elapsedMillis)
  }
  def log(e: HttpStatusException) {
    log(e.status.toString, e.status < 400)
    HttpResponseMonitoring.record(request, uriTemplate, e.status, elapsedMillis)
  }
  def log(e: Exception) {
    log(e.getClass.getSimpleName, false)
    HttpResponseMonitoring.record(request, uriTemplate, 500, elapsedMillis)
  }
  private def log(status: String, ok: Boolean) {
    val requestBody = (
      if (ok) {
        None
      } else {
        Some(request.bodyAsText().compile.toVector.unsafeRunSync())
      }).map("request body " + _).filter(_ => request.uri.toString.contains(logFailedRequestBodiesForUrisContaining)).getOrElse("")

    HttpResponseLog.logger.debug(maskSensitiveInformation(s"${request.method} ${request.uri} status ${status} took ${elapsedMillis} ms ${requestBody}"))
  }
}

protected object HttpResponseMonitoring {
  private val statusCounter = Counter.build().name("fi_oph_koski_http_Http_status").help("Koski HTTP client response status").labelNames("service", "endpoint", "responseclass").register()
  private val durationDummary = Summary.build().name("fi_oph_koski_http_Http_duration").help("Koski HTTP client response duration").labelNames("service", "endpoint").register()

  private val HttpServicePattern = """https?:\/\/([a-z0-9:\.-]+\/[a-z0-9\.-]+).*""".r

  def record(request: Request[IO], uriTemplate: String, status: Int, durationMillis: Long) {
    val responseClass = status / 100 * 100 // 100, 200, 300, 400, 500

    val service = request.uri.toString match {
      case HttpServicePattern(service) => service
      case _ => request.uri.toString
    }
    val endpoint = request.method + " " + uriTemplate

    statusCounter.labels(service, endpoint, responseClass.toString).inc
    durationDummary.labels(service, endpoint).observe(durationMillis.toDouble / 1000)
  }
}
