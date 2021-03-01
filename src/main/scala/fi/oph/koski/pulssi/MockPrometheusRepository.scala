package fi.oph.koski.pulssi

import fi.oph.koski.http.HttpStatusException
import fi.oph.koski.json.JsonFiles
import org.json4s

import cats.effect.IO

object MockPrometheusRepository extends PrometheusRepository {
  override def query(query: String): IO[json4s.JValue] = {
    val Array(_, filename) = query.split("=")
    JsonFiles.readFileIfExists(s"src/main/resources/mockdata/prometheus/$filename.json")
      .map(IO.pure)
      .getOrElse(IO.raiseError(HttpStatusException(400, "Prometheus metric file not found", "GET", query)))
  }
}
