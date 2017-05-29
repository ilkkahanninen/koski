package fi.oph.koski.pulssi

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.http.{HttpStatus, KoskiErrorCategory}
import fi.oph.koski.servlet.HtmlServlet
import org.scalatra.ScalatraServlet

class PulssiHtmlServlet(val application: KoskiApplication) extends ScalatraServlet with HtmlServlet {
  get("/") {
    htmlIndex("koski-pulssi.js")
  }

  get("/raportti") {
    if (!isAuthenticated) {
      redirectToLogin
    }
    if (koskiSessionOption.exists(_.hasGlobalReadAccess)) {
      <html>
        {htmlHead()}
        <body>
          <h3>Oppijat ja opiskeluoikeudet</h3>
          <ul>
            <li>
              Oppijoiden määrä: {pulssi.oppijoidenMäärä}
            </li>
            <li>
              Opiskeluoikeuksien määrä: {pulssi.opiskeluoikeusTilasto.opiskeluoikeuksienMäärä}
            </li>
            <li>
              Opiskeluoikeuksien määrät koulutusmuodoittain:
              <ul>
                {
                pulssi.opiskeluoikeusTilasto.koulutusmuotoTilastot.map { tilasto =>
                  <li>
                    {tilasto.koulutusmuoto}: {tilasto.opiskeluoikeuksienMäärä}
                  </li>
                }}
              </ul>
            </li>
          </ul>
          <h3>Koski tiedonsiirrot</h3>
          <ul>
            <li>
            </li>
          </ul>
          <h3>Koski käyttöoikeudet</h3>
          <ul>
            <li>
              Käyttöoikeuksien määrä: {pulssi.käyttöoikeudet.kokonaismäärä}
            </li>
            <li>
              Käyttöoikeuksien määrät ryhmittäin:
              <ul>
              {pulssi.käyttöoikeudet.ryhmienMäärät.map { case (ryhmä, määrä) =>
                <li>
                  {ryhmä}: {määrä}
                </li>
              }}
              </ul>
            </li>
          </ul>
          <h3>Metriikka viimeisen 30 päivän ajalta</h3>
          <ul>
            <li>
              Tiedonsiirtovirheet: {pulssi.metrics.epäonnistuneetSiirrot}
            </li>
            <li>
              Käyttökatkojen määrä: {pulssi.metrics.katkot}
            </li>
            <li>
              Hälytysten määrä: {pulssi.metrics.hälytykset}
            </li>
            <li>
              Lokitettujen virheiden määrä: {pulssi.metrics.virheet}
            </li>
          </ul>
        </body>
      </html>
    } else {
      renderStatus(KoskiErrorCategory.notFound())
    }
  }

  private def pulssi = application.koskiPulssi
}
