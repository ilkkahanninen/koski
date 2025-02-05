package fi.oph.koski.valpas.hakukooste

import com.typesafe.config.Config
import fi.oph.koski.http.HttpStatus
import fi.oph.koski.validation.ValidatingAndResolvingExtractor
import fi.oph.koski.valpas.opiskeluoikeusrepository.ValpasHenkilö


trait ValpasHakukoosteService {
  def getHakukoosteet(oppijaOids: Set[ValpasHenkilö.Oid], ainoastaanAktiivisetHaut: Boolean = false, errorClue: String = ""): Either[HttpStatus, Seq[Hakukooste]]

  def getYhteishakujenHakukoosteet(oppijaOids: Set[ValpasHenkilö.Oid], ainoastaanAktiivisetHaut: Boolean = false, errorClue: String = ""): Either[HttpStatus, Seq[Hakukooste]] = {
    getHakukoosteet(oppijaOids, ainoastaanAktiivisetHaut, errorClue).map(_.filter(hk => hk.hakutapa.koodiarvo == "01"))
  }
}

object ValpasHakukoosteService {
  def apply(config: Config, validatingAndResolvingExtractor: ValidatingAndResolvingExtractor): ValpasHakukoosteService = {
    config.getString("opintopolku.virkailija.url") match {
      case "mock" => new MockHakukoosteService()
      case _ => new SureHakukoosteService(config, validatingAndResolvingExtractor)
    }
  }
}
