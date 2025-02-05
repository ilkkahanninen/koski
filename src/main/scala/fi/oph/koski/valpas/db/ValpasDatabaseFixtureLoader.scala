package fi.oph.koski.valpas.db

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.log.Logging
import fi.oph.koski.valpas.opiskeluoikeusrepository.MockValpasRajapäivätService
import fi.oph.koski.valpas.valpasrepository.{OppivelvollisuudenKeskeytysRepository, ValpasExampleData}

class ValpasDatabaseFixtureLoader(app: KoskiApplication) extends Logging {
  private val kuntailmoitusRepository = app.valpasKuntailmoitusRepository
  private val lisätiedotRepository = app.valpasOpiskeluoikeusLisätiedotRepository
  private val rajapäivätService = app.valpasRajapäivätService
  private val oppivelvollisuudenKeskeytysRepository = app.valpasOppivelvollisuudenKeskeytysRepository

  def reset(): Unit = {
    logger.info("Resetting Valpas DB fixtures")
    kuntailmoitusRepository.truncate()
    lisätiedotRepository.truncate()
    oppivelvollisuudenKeskeytysRepository.truncate()
    loadIlmoitukset()
    loadOppivelvollisuudenKeskeytykset()
  }

  private def loadIlmoitukset(): Unit = {
    val fixtures = ValpasExampleData.ilmoitukset
    logger.info(s"Inserting ${fixtures.length} ilmoitus fixtures")
    fixtures.foreach { fx => {
      val tallennettuTarkastelupäivä = rajapäivätService.tarkastelupäivä
      rajapäivätService.asInstanceOf[MockValpasRajapäivätService]
        .asetaMockTarkastelupäivä(fx.aikaleimaOverride.getOrElse(rajapäivätService.tarkastelupäivä))

      val kuntailmoitus = fx.kuntailmoitus
      kuntailmoitusRepository.create(kuntailmoitus).left.foreach(e => logger.error(s"Fixture insertion failed: $e"))

      rajapäivätService.asInstanceOf[MockValpasRajapäivätService].asetaMockTarkastelupäivä(tallennettuTarkastelupäivä)
    }}

    ValpasExampleData.ilmoitustenLisätietojenPoistot.foreach(kuntailmoitusRepository.deleteLisätiedot)
  }

  private def loadOppivelvollisuudenKeskeytykset(): Unit = {
    val fixtures = ValpasExampleData.oppivelvollisuudenKeskeytykset
    logger.info(s"Inserting ${fixtures.length} oppivelvollisuuden keskeytys fixtures")
    fixtures.foreach(oppivelvollisuudenKeskeytysRepository.setKeskeytys)
  }
}
