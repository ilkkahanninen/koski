import javax.servlet.ServletContext

import fi.oph.koski.cache.CacheServlet
import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.db._
import fi.oph.koski.documentation.SchemaDocumentationServlet
import fi.oph.koski.fixture.{FixtureServlet, Fixtures}
import fi.oph.koski.history.KoskiHistoryServlet
import fi.oph.koski.koodisto.KoodistoCreator
import fi.oph.koski.koski.{KoskiFacade, OppijaServlet}
import fi.oph.koski.koskiuser.{KäyttöoikeusRyhmätCreator, LogoutServlet, KäyttöoikeusRepository, UserServlet}
import fi.oph.koski.log.Logging
import fi.oph.koski.oppilaitos.OppilaitosServlet
import fi.oph.koski.servlet.IndexServlet
import fi.oph.koski.suoritusote.SuoritusServlet
import fi.oph.koski.todistus.TodistusServlet
import fi.oph.koski.tutkinto.TutkintoServlet
import fi.oph.koski.util.Pools
import org.scalatra._

class ScalatraBootstrap extends LifeCycle with Logging with GlobalExecutionContext with Futures {
  override def init(context: ServletContext) {
    try {
      Pools.init
      val configOverrides: Map[String, String] = Option(context.getAttribute("tor.overrides").asInstanceOf[Map[String, String]]).getOrElse(Map.empty)
      val application = KoskiApplication(configOverrides)
      if (application.config.getBoolean("koodisto.create")) {
        KoodistoCreator.createKoodistotFromMockData(application.config)
      }
      if (application.config.getBoolean("käyttöoikeusryhmät.create")) {
        KäyttöoikeusRyhmätCreator.luoKäyttöoikeusRyhmät(application.config)
      }
      val facade = new KoskiFacade(application.oppijaRepository, application.opiskeluOikeusRepository)
      context.mount(new OppijaServlet(facade, application.käyttöoikeusRepository, application.directoryClient, application.validator, application.historyRepository), "/api/oppija")
      context.mount(new KoskiHistoryServlet(application.käyttöoikeusRepository, application.directoryClient, application.historyRepository), "/api/opiskeluoikeus/historia")
      context.mount(new UserServlet(application.directoryClient, application.käyttöoikeusRepository), "/user")
      context.mount(new LogoutServlet(application.käyttöoikeusRepository, application.directoryClient), "/user/logout")
      context.mount(new OppilaitosServlet(application.oppilaitosRepository, application.käyttöoikeusRepository, application.directoryClient), "/api/oppilaitos")
      context.mount(new TutkintoServlet(application.tutkintoRepository), "/api/tutkinto")
      context.mount(new SchemaDocumentationServlet(application.koodistoPalvelu), "/documentation")
      context.mount(new TodistusServlet(application.käyttöoikeusRepository, application.directoryClient, facade, application.tutkintoRepository), "/todistus")
      context.mount(new SuoritusServlet(application.käyttöoikeusRepository, application.directoryClient, facade, application.oppijaRepository, facade), "/opintosuoritusote")
      context.mount(new IndexServlet(), "/")
      context.mount(new CacheServlet(application.käyttöoikeusRepository, application.directoryClient, application), "/cache")
      if (Fixtures.shouldUseFixtures(application.config)) {
        context.mount(new FixtureServlet(application.käyttöoikeusRepository, application.directoryClient, application), "/fixtures")
      }
    } catch {
      case e: Throwable =>
        logger.error(e)("Error in server startup")
        System.exit(1)
    }
  }

  override def destroy(context: ServletContext) = {
  }
}