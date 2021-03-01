package fi.oph.koski.http

import cats.data.Kleisli

import java.util.Base64
import org.http4s.client.Client
import org.http4s.{Header, Headers, Request, Service}
import cats.effect._

object BasicAuthentication {
  def basicAuthHeader(user: String, password: String) = {
    val auth: String = "Basic " + Base64.getEncoder.encodeToString((user + ":" + password).getBytes("UTF8"))
    ("Authorization", auth)
  }
}

object ClientWithBasicAuthentication {
  def apply(wrappedClient: Client[IO], username: String, password: String): Client[IO] = {
    val (name, value) = BasicAuthentication.basicAuthHeader(username, password)

    def open(req: Request[IO]) = wrappedClient.run(req.putHeaders(Header(name, value)))

    Client.apply[IO](open)
    // TODO shutdown = wrappedClient.shutdown
  }
}
