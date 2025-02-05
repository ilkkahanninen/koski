package fi.oph.koski.schema

import java.time.{LocalDate, LocalDateTime}

import fi.oph.koski.koskiuser.Rooli
import fi.oph.koski.schema.annotation.{Deprecated, Hidden, KoodistoKoodiarvo, KoodistoUri, OksaUri, SensitiveData, Tooltip}
import fi.oph.koski.schema.TukimuodollisetLisätiedot.tukimuodoissaOsaAikainenErityisopetus
import fi.oph.scalaschema.annotation._

@Description("Perusopetuksen lisäopetuksen opiskeluoikeus")
case class PerusopetuksenLisäopetuksenOpiskeluoikeus(
  oid: Option[String] = None,
  versionumero: Option[Int] = None,
  aikaleima: Option[LocalDateTime] = None,
  lähdejärjestelmänId: Option[LähdejärjestelmäId] = None,
  oppilaitos: Option[Oppilaitos],
  koulutustoimija: Option[Koulutustoimija],
  @Hidden
  sisältyyOpiskeluoikeuteen: Option[SisältäväOpiskeluoikeus] = None,
  tila: NuortenPerusopetuksenOpiskeluoikeudenTila,
  @Description("Perusopetuksen lisäopetuksen opiskeluoikeuden lisätiedot")
  lisätiedot: Option[PerusopetuksenLisäopetuksenOpiskeluoikeudenLisätiedot] = None,
  @MaxItems(1)
  suoritukset: List[PerusopetuksenLisäopetuksenSuoritus],
  @KoodistoKoodiarvo(OpiskeluoikeudenTyyppi.perusopetuksenlisaopetus.koodiarvo)
  tyyppi: Koodistokoodiviite = OpiskeluoikeudenTyyppi.perusopetuksenlisaopetus,
  organisaatiohistoria: Option[List[OpiskeluoikeudenOrganisaatiohistoria]] = None
) extends KoskeenTallennettavaOpiskeluoikeus with TukimuodollinenOpiskeluoikeus {
  @Description("Oppijan oppimäärän päättymispäivä")
  override def päättymispäivä: Option[LocalDate] = super.päättymispäivä
  override def withOppilaitos(oppilaitos: Oppilaitos) = this.copy(oppilaitos = Some(oppilaitos))
  override def withKoulutustoimija(koulutustoimija: Koulutustoimija) = this.copy(koulutustoimija = Some(koulutustoimija))
  override def arvioituPäättymispäivä = None
}

case class PerusopetuksenLisäopetuksenOpiskeluoikeudenLisätiedot(
  @Description("Perusopetuksen aloittamisesta lykkäys (true/false). Oppilas saanut luvan aloittaa perusopetuksen myöhemmin.")
  @Tooltip("Perusopetuksen aloittamisesta lykkäys. Oppilas saanut luvan aloittaa perusopetuksen myöhemmin.")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT))
  @OksaUri("tmpOKSAID242", "koulunkäynnin aloittamisen lykkääminen")
  @DefaultValue(false)
  perusopetuksenAloittamistaLykätty: Boolean = false,
  @Description("Perusopetuksen aloituksen aikaistaminen (true/false). Oppilas aloittanut perusopetuksen ennen oppivelvollisuusikää.")
  @Tooltip("Perusopetuksen aloitusta aikaistettu, eli oppilas aloittanut peruskoulun ennen oppivelvollisuusikää.")
  @DefaultValue(false)
  aloittanutEnnenOppivelvollisuutta: Boolean = false,
  @Description("Pidennetty oppivelvollisuus alkamis- ja päättymispäivineen. Kentän puuttuminen tai null-arvo tulkitaan siten, että oppilaalla ei ole pidennettyä oppivelvollisuutta. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Mahdollisen pidennetyn oppivelvollisuuden alkamis- ja päättymispäivät. Rahoituksen laskennassa käytettävä tieto.")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT))
  @OksaUri("tmpOKSAID517", "pidennetty oppivelvollisuus")
  pidennettyOppivelvollisuus: Option[Aikajakso] = None,
  @KoodistoUri("perusopetuksentukimuoto")
  @Description("Oppilaan saamat laissa säädetyt tukimuodot.")
  @Tooltip("Oppilaan saamat laissa säädetyt tukimuodot. Voi olla useita.")
  @Deprecated("Käytä korvaavia kenttiä Erityisen tuen päätökset, Osa-aikainen erityisopetus lukuvuoden aikana ja Osa-aikainen erityisopetus perusopetuksen lisäopetuksen aikana")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT))
  tukimuodot: Option[List[Koodistokoodiviite]] = None,
  @Description("Erityisen tuen päätös alkamis- ja päättymispäivineen. Kentän puuttuminen tai null-arvo tulkitaan siten, että päätöstä ei ole tehty. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Mahdollisen erityisen tuen päätöksen alkamis- ja päättymispäivät. Rahoituksen laskennassa käytettävä tieto.")
  @OksaUri("tmpOKSAID281", "henkilökohtainen opetuksen järjestämistä koskeva suunnitelma")
  @Deprecated("Käytä korvaavaa kenttää Erityisen tuen päätökset")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT, Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  erityisenTuenPäätös: Option[ErityisenTuenPäätös] = None,
  @Description("Erityisen tuen päätökset alkamis- ja päättymispäivineen. Voi olla useita erillisiä jaksoja. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Mahdollisen erityisen tuen päätösten alkamis- ja päättymispäivät. Voi olla useita erillisiä jaksoja. Rahoituksen laskennassa käytettävä tieto.")
  @OksaUri("tmpOKSAID281", "henkilökohtainen opetuksen järjestämistä koskeva suunnitelma")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT, Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  erityisenTuenPäätökset: Option[List[ErityisenTuenPäätös]] = None,
  @Description("Tehostetun tuen päätös alkamis- ja päättymispäivineen. Kentän puuttuminen tai null-arvo tulkitaan siten, että päätöstä ei ole tehty.")
  @Tooltip("Mahdollisen tehostetun tuen päätös päätöksen alkamis- ja päättymispäivät.")
  @OksaUri("tmpOKSAID511", "tehostettu tuki")
  @Deprecated("Käytä korvaavaa kenttää Tehostetun tuen päätökset")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT, Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  tehostetunTuenPäätös: Option[TehostetunTuenPäätös] = None,
  @Description("Tehostetun tuen päätös. Lista alku-loppu päivämääräpareja.")
  @Tooltip("Mahdollisen tehostetun tuen päätösten alkamis- ja päättymispäivät. Voi olla useita erillisiä jaksoja.")
  @OksaUri("tmpOKSAID511", "tehostettu tuki")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT, Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  tehostetunTuenPäätökset: Option[List[TehostetunTuenPäätös]] = None,
  @Description("Opiskelu joustavassa perusopetuksessa (JOPO) alkamis- ja päättymispäivineen. Kentän puuttuminen tai null-arvo tulkitaan siten, ettei oppilas ole joustavassa perusopetuksessa. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Mahdollisen joustavan perusopetuksen (JOPO) alkamis- ja päättymispäivät. Rahoituksen laskennassa käytettävä tieto.")
  @OksaUri("tmpOKSAID453", "joustava perusopetus")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT, Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  joustavaPerusopetus: Option[Aikajakso] = None,
  @Description("Tieto opiskelusta kotiopetuksessa huoltajan päätöksestä alkamis- ja päättymispäivineen. Kentän puuttuminen tai null-arvo tulkitaan siten, ettei oppilas ole kotiopetuksessa. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Tieto mahdollisesta opiskelusta kotiopetuksessa huoltajan päätöksestä alkamis- ja päättymispäivineen. Rahoituksen laskennassa käytettävä tieto.")
  @Deprecated("Käytä korvaavaa kenttää Kotiopetusjaksot")
  kotiopetus: Option[Aikajakso] = None,
  @Description("Kotiopetusjaksot huoltajan päätöksestä alkamis- ja päättymispäivineen. Kentän puuttuminen tai null-arvo tulkitaan siten, ettei oppilas ole kotiopetuksessa. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Kotiopetusjaksot huoltajan päätöksestä alkamis- ja päättymispäivineen. Rahoituksen laskennassa käytettävä tieto.")
  kotiopetusjaksot: Option[List[Aikajakso]] = None,
  @Description("Tieto opiskelusta ulkomailla huoltajan ilmoituksesta alkamis- ja päättymispäivineen. Kentän puuttuminen tai null-arvo tulkitaan siten, ettei oppilas ole ulkomailla. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Tieto opiskelusta ulkomailla huoltajan ilmoituksesta alkamis- ja päättymispäivineen. Rahoituksen laskennassa käytettävä tieto.")
  @Deprecated("Käytä korvaavaa kenttää Ulkomaanjaksot")
  ulkomailla: Option[Aikajakso] = None,
  @Description("Huoltajan ilmoittamat ulkomaan opintojaksot alkamis- ja päättymispäivineen. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Huoltajan ilmoittamat ulkomaan opintojaksot alkamis- ja päättymispäivineen. Rahoituksen laskennassa käytettävä tieto.")
  ulkomaanjaksot: Option[List[Aikajakso]] = None,
  @Description("Oppilas on vuosiluokkiin sitomattomassa opetuksessa (kyllä/ei).")
  @Tooltip("Onko oppilas vuosiluokkiin sitomattomassa opetuksessa.")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT))
  @DefaultValue(false)
  @Title("Vuosiluokkiin sitomaton opetus")
  vuosiluokkiinSitoutumatonOpetus: Boolean = false,
  @Description("Onko oppija muu kuin vaikeimmin kehitysvammainen. Lista alku-loppu päivämääräpareja. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Tieto siitä, onko oppija muu kuin vaikeimmin kehitysvammainen (alku- ja loppupäivämäärät). Voi olla useita erillisiä jaksoja. Rahoituksen laskennassa käytettävä tieto.")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT))
  vammainen: Option[List[Aikajakso]] = None,
  @Description("Onko oppija vaikeasti kehitysvammainen. Lista alku-loppu päivämääräpareja. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Tieto siitä, onko oppija vaikeasti kehitysvammainen (alku- ja loppupäivämäärät). Voi olla useita erillisiä jaksoja. Rahoituksen laskennassa käytettävä tieto.")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT))
  vaikeastiVammainen: Option[List[Aikajakso]] = None,
  @Description("Oppilaalla on majoitusetu (alku- ja loppupäivämäärä). Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Tieto siitä, jos oppilaalla on majoitusetu (alku- ja loppupäivämäärät). Rahoituksen laskennassa käytettävä tieto.")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT))
  majoitusetu: Option[Aikajakso] = None,
  @Description("Oppilaalla on kuljetusetu (alku- ja loppupäivämäärät).")
  @Tooltip("Tieto siitä, jos oppilaalla on kuljetusetu (alku- ja loppupäivämäärät).")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT))
  kuljetusetu: Option[Aikajakso] = None,
  @Description("Oppilaalla on oikeus maksuttomaan asuntolapaikkaan (alku- ja loppupäivämäärät).")
  @Tooltip("Tieto siitä, jos oppilaalla on oikeus maksuttomaan asuntolapaikkaan (alku- ja loppupäivämäärät).")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT, Rooli.LUOTTAMUKSELLINEN_KELA_SUPPEA, Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  oikeusMaksuttomaanAsuntolapaikkaan: Option[Aikajakso] = None,
  @Description("Sisäoppilaitosmuotoinen majoitus, aloituspäivä ja loppupäivä. Lista alku-loppu päivämääräpareja. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Mahdollisen sisäoppilaitosmuotoisen majoituksen aloituspäivä ja loppupäivä. Voi olla useita erillisiä jaksoja. Rahoituksen laskennassa käytettävä tieto.")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT, Rooli.LUOTTAMUKSELLINEN_KELA_SUPPEA, Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  sisäoppilaitosmainenMajoitus: Option[List[Aikajakso]] = None,
  @Description("Oppija on koulukotikorotuksen piirissä, aloituspäivä ja loppupäivä. Lista alku-loppu päivämääräpareja. Rahoituksen laskennassa käytettävä tieto.")
  @Tooltip("Tieto siitä, jos oppija on koulukotikorotuksen piirissä (aloituspäivä ja loppupäivä). Voi olla useita erillisiä jaksoja. Rahoituksen laskennassa käytettävä tieto.")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT, Rooli.LUOTTAMUKSELLINEN_KELA_LAAJA))
  koulukoti: Option[List[Aikajakso]] = None,
  maksuttomuus: Option[List[Maksuttomuus]] = None,
  oikeuttaMaksuttomuuteenPidennetty: Option[List[OikeuttaMaksuttomuuteenPidennetty]] = None
) extends TukimuodollisetLisätiedot
  with SisäoppilaitosmainenMajoitus
  with OikeusmaksuttomaanAsuntolapaikkaan
  with Majoitusetuinen
  with Kuljetusetuinen
  with Kotiopetuksellinen
  with Vammainen
  with VaikeastiVammainen
  with MaksuttomuusTieto {
  override def sisältääOsaAikaisenErityisopetuksen: Boolean =
    tukimuodoissaOsaAikainenErityisopetus(tehostetunTuenPäätökset) ||
      tukimuodoissaOsaAikainenErityisopetus(erityisenTuenPäätökset)
}

@Description("Perusopetuksen lisäopetuksen suoritustiedot")
case class PerusopetuksenLisäopetuksenSuoritus(
  @Title("Koulutus")
  @Tooltip("Suoritettava koulutus ja koulutuksen opetussuunnitelman perusteiden diaarinumero.")
  koulutusmoduuli: PerusopetuksenLisäopetus,
  @Description("Luokan tunniste, esimerkiksi 9C.")
  @Tooltip("Luokan tunniste, esimerkiksi 9C.")
  luokka: Option[String] = None,
  toimipiste: OrganisaatioWithOid,
  vahvistus: Option[HenkilövahvistusPaikkakunnalla] = None,
  suorituskieli: Koodistokoodiviite,
  @Tooltip("Mahdolliset muut suorituskielet.")
  muutSuorituskielet: Option[List[Koodistokoodiviite]] = None,
  @Description("Tieto siitä, osallistuuko oppilas osa-aikaiseen erityisopetukseen perusopetuksen lisäopetuksen aikana")
  @Tooltip("Osallistuuko oppilas osa-aikaiseen erityisopetukseen perusopetuksen lisäopetuksen aikana")
  @SensitiveData(Set(Rooli.LUOTTAMUKSELLINEN_KAIKKI_TIEDOT))
  @DefaultValue(false)
  @Title("Osa-aikainen erityisopetus perusopetuksen lisäopetuksen aikana")
  @Deprecated("Tätä kenttää ei toistaiseksi käytetä.")
  @Hidden
  osaAikainenErityisopetus: Boolean = false,
  @Description("Oppiaineiden suoritukset")
  @Title("Oppiaineet")
  override val osasuoritukset: Option[List[PerusopetuksenLisäopetuksenAlisuoritus]],
  @Description("Tieto siitä, mikäli osa opiskelijan opinnoista on tapahtunut muussa oppilaitoksessa, työpaikalla tai työpaikkaan rinnastettavassa muussa paikassa.")
  @Tooltip("Todistuksella näkyvät lisätiedot. Esimerkiksi tieto siitä, mikäli osa opiskelijan opinnoista on tapahtunut muussa oppilaitoksessa, työpaikalla tai työpaikkaan rinnastettavassa muussa paikassa.")
  todistuksellaNäkyvätLisätiedot: Option[LocalizedString] = None,
  @KoodistoKoodiarvo("perusopetuksenlisaopetus")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite("perusopetuksenlisaopetus", koodistoUri = "suorituksentyyppi")
) extends KoskeenTallennettavaPäätasonSuoritus
  with Toimipisteellinen
  with Todistus
  with Arvioinniton
  with MonikielinenSuoritus
  with Suorituskielellinen
  with ErityisopetuksellinenPäätasonSuoritus
  with SuoritusVaatiiMahdollisestiMaksuttomuusTiedonOpiskeluoikeudelta {
  def sisältääOsaAikaisenErityisopetuksen: Boolean = osaAikainenErityisopetus
}

trait PerusopetuksenLisäopetuksenAlisuoritus extends Suoritus with MahdollisestiSuorituskielellinen {
  @Description("Tieto siitä, onko kyseessä perusopetuksen oppiaineen arvosanan korotus. Tietoa käytetään todistuksella")
  def korotus: Boolean
}

@Description("Perusopetuksen oppiaineen suoritus osana perusopetuksen lisäopetusta")
case class PerusopetuksenLisäopetuksenOppiaineenSuoritus(
  koulutusmoduuli: NuortenPerusopetuksenOppiaine,
  @DefaultValue(false)
  yksilöllistettyOppimäärä: Boolean = false,
  @Description("Jos opiskelijan lisäopetuksessa saama uusi arvosana perusopetuksen yhteisissä tai valinnaisissa oppiaineissa on korkeampi kuin perusopetuksen päättöarvosana, se merkitään tähän")
  arviointi: Option[List[PerusopetuksenOppiaineenArviointi]] = None,
  @Tooltip("Onko kyseessä peruskoulun päättötodistuksen arvosanan korotus.")
  korotus: Boolean,
  suorituskieli: Option[Koodistokoodiviite],
  @KoodistoKoodiarvo("perusopetuksenlisaopetuksenoppiaine")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite(koodiarvo = "perusopetuksenlisaopetuksenoppiaine", koodistoUri = "suorituksentyyppi")
) extends PerusopetuksenLisäopetuksenAlisuoritus with PerusopetuksenOppiaineenSuoritus with Vahvistukseton with Yksilöllistettävä with MahdollisestiSuorituskielellinen

@Description("Perusopetuksen toiminta-alueen suoritus osana perusopetuksen lisäopetusta")
case class PerusopetuksenLisäopetuksenToiminta_AlueenSuoritus(
  @Title("Toiminta-alue")
  koulutusmoduuli: PerusopetuksenToiminta_Alue,
  @Description("Toiminta-alueet voivat sisältää yksittäisen oppiaineen tavoitteita ja sisältöjä, jos oppilaalla on vahvuuksia jossakin yksittäisessä oppiaineessa. Opetuksen toteuttamisessa eri toiminta-alueiden sisältöjä voidaan yhdistää")
  arviointi: Option[List[PerusopetuksenOppiaineenArviointi]] = None,
  @DefaultValue(false)
  korotus: Boolean = false,
  suorituskieli: Option[Koodistokoodiviite] = None,
  @KoodistoKoodiarvo("perusopetuksenlisaopetuksentoimintaalue")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite(koodiarvo = "perusopetuksenlisaopetuksentoimintaalue", koodistoUri = "suorituksentyyppi")
) extends PerusopetuksenLisäopetuksenAlisuoritus with Vahvistukseton with Toiminta_AlueenSuoritus

@Description("Muu perusopetuksen lisäopetuksessa suoritettu opintokokonaisuus")
case class MuuPerusopetuksenLisäopetuksenSuoritus(
  @Title("Suoritetut opinnot")
  koulutusmoduuli: MuuPerusopetuksenLisäopetuksenKoulutusmoduuli,
  arviointi: Option[List[PerusopetuksenOppiaineenArviointi]] = None,
  suorituskieli: Option[Koodistokoodiviite] = None,
  @KoodistoKoodiarvo("muuperusopetuksenlisaopetuksensuoritus")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite(koodiarvo = "muuperusopetuksenlisaopetuksensuoritus", koodistoUri = "suorituksentyyppi")
) extends PerusopetuksenLisäopetuksenAlisuoritus with Vahvistukseton {
  def korotus = false
}

case class MuuPerusopetuksenLisäopetuksenKoulutusmoduuli(
  tunniste: PaikallinenKoodi,
  @Tooltip("Paikallisen oppiaineen vapaamuotoinen kuvaus.")
  kuvaus: LocalizedString,
  laajuus: Option[LaajuusVuosiviikkotunneissa] = None
) extends PaikallinenKoulutusmoduuliValinnainenLaajuus

@Description("Perusopetuksen lisäopetuksen tunnistetiedot")
case class PerusopetuksenLisäopetus(
  @KoodistoKoodiarvo("020075")
  tunniste: Koodistokoodiviite = Koodistokoodiviite("020075", koodistoUri = "koulutus"),
  perusteenDiaarinumero: Option[String],
  koulutustyyppi: Option[Koodistokoodiviite] = None
) extends DiaarinumerollinenKoulutus with Laajuudeton
