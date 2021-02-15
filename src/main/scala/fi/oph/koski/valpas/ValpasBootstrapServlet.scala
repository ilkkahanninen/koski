package fi.oph.koski.valpas

import fi.oph.koski.config.{Environment, KoskiApplication}
import fi.oph.koski.json.JsonSerializer
import fi.oph.koski.koskiuser.{KoskiSpecificSession, Unauthenticated}
import fi.oph.koski.servlet.{KoskiSpecificApiServlet, NoCache}

class ValpasBootstrapServlet(implicit val application: KoskiApplication) extends KoskiSpecificApiServlet with NoCache with Unauthenticated {

  get("/set-window-properties.js") {
    contentType = "text/javascript"
    response.writer.print(
      "window.valpasLocalizationMap=" + JsonSerializer.writeWithRoot(application.valpasLocalizationRepository.localizations) + ";" +
      "window.environment='" + Environment.currentEnvironment(application.config) + "';" +
      "window.opintopolkuVirkailijaUrl='" + application.config.getString("opintopolku.virkailija.url") + "'"
    )
  }
}
