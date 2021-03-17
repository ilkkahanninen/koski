package fi.oph.koski.valpas.repository

import fi.oph.koski.schema.annotation.KoodistoUri
import fi.oph.koski.schema.{Koodistokoodiviite, LocalizedString}
import fi.oph.koski.valpas.hakukooste.{Hakukooste, Hakutoive}
import fi.oph.scalaschema.annotation.SyntheticProperty

import java.time.LocalDate

case class ValpasOppija(
  henkilö: ValpasHenkilö,
  oikeutetutOppilaitokset: Set[ValpasOppilaitos.Oid],
  opiskeluoikeudet: Seq[ValpasOpiskeluoikeus]
) {
  @SyntheticProperty
  def opiskelee: Boolean = opiskeluoikeudet.exists(_.isOpiskelu)

  @SyntheticProperty
  def oppivelvollisuusVoimassaAsti: Option[LocalDate] = henkilö.syntymäaika.map(_.plusYears(18))
}

object ValpasHenkilö {
  type Oid = String
}

case class ValpasHenkilö(
  oid: ValpasHenkilö.Oid,
  hetu: Option[String],
  syntymäaika: Option[LocalDate],
  etunimet: String,
  sukunimi: String
)

object ValpasOpiskeluoikeus {
  type Oid = String
}

case class ValpasOpiskeluoikeus(
  oid: ValpasOpiskeluoikeus.Oid,
  @KoodistoUri("opiskeluoikeudentyyppi")
  tyyppi: Koodistokoodiviite,
  oppilaitos: ValpasOppilaitos,
  toimipiste: Option[ValpasToimipiste],
  alkamispäivä: Option[String],
  päättymispäivä: Option[String],
  ryhmä: Option[String],
  @KoodistoUri("koskiopiskeluoikeudentila")
  viimeisinTila: Koodistokoodiviite
) {
  @SyntheticProperty
  def isOpiskelu: Boolean =
    viimeisinTila.koodiarvo == "lasna" || viimeisinTila.koodiarvo == "valiaikaisestikeskeytynyt"
}

object ValpasOppilaitos {
  type Oid = String
}

case class ValpasOppilaitos(
  oid: ValpasOppilaitos.Oid,
  nimi: LocalizedString
)

object ValpasToimipiste {
  type Oid = String
}

case class ValpasToimipiste(
  oid: ValpasToimipiste.Oid,
  nimi: LocalizedString
)

object ValpasHakutilanne {
  type HakuOid = String
  type HakemusOid = String

  def apply(hakukooste: Hakukooste): ValpasHakutilanne =
    ValpasHakutilanne(
      hakuOid = hakukooste.hakuOid,
      hakuNimi = hakukooste.hakuNimi,
      hakemusOid = hakukooste.hakemusOid,
      aktiivinen = hakukooste.hakutoiveet.exists(_.isAktiivinen),
      muokattu = hakukooste.muokattu,
      hakutoiveet = hakukooste.hakutoiveet.map(ValpasHakutoive.apply)
    )
}

case class ValpasHakutilanne(
  hakuOid: String,
  hakuNimi: LocalizedString,
  hakemusOid: String,
  aktiivinen: Boolean,
  muokattu: String,
  hakutoiveet: Seq[ValpasHakutoive]
)

object ValpasHakutoive {
  type KoulutusOid = String

  def apply(hakutoive: Hakutoive): ValpasHakutoive = {
    ValpasHakutoive(
      hakukohdeNimi = hakutoive.hakukohdeNimi,
      hakutoivenumero = Some(hakutoive.hakutoivenumero),
      pisteet = hakutoive.pisteet,
      hyväksytty = None // TODO
    )
  }
}

case class ValpasHakutoive(
  hakukohdeNimi: LocalizedString,
  hakutoivenumero: Option[Int],
  pisteet: BigDecimal,
  hyväksytty: Option[Boolean]
)
