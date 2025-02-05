package fi.oph.koski.servlet

import fi.oph.koski.html.Raamit
import fi.oph.koski.http.HttpStatus
import fi.oph.koski.koskiuser.KoskiSpecificAuthenticationSupport
import fi.oph.koski.util.XML

import scala.xml.Elem

trait KoskiHtmlServlet extends HtmlServlet with KoskiSpecificAuthenticationSupport {

  def renderStatus(status: HttpStatus): Unit = {
    val html = XML.transform(htmlIndex("koski-main.js", piwikHttpStatusCode = Some(status.statusCode), raamit = virkailijaRaamit)) {
      case e: Elem if e.label == "head" =>
        e copy (child = (e.child :+ htmlErrorObjectScript(status)) ++ piwikTrackErrorObject)
    }

    response.setStatus(status.statusCode)
    renderHtml(html)
  }

  protected val virkailijaRaamitSet: Boolean

  protected def virkailijaRaamit: Raamit
}
