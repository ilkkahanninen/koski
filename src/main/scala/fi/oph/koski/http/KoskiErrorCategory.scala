package fi.oph.koski.http

import fi.oph.koski.documentation.JsonValidationErrorExample
import fi.oph.koski.oppija.{HenkilönOpiskeluoikeusVersiot, OpiskeluoikeusVersio}
import fi.oph.koski.json.JsonSerializer.serializeWithRoot
import fi.oph.koski.schema.{Koodistokoodiviite, LähdejärjestelmäId, OidHenkilö}

object KoskiErrorCategory {
  val children = List(ok, badRequest, unauthorized, forbidden, notFound, conflict, unsupportedMediaType, internalError)

  object ok extends ErrorCategory("ok", 200, "Ok") {
    val createdOrUpdated = subcategory("createdOrUpdated", "Päivitys/lisäys onnistui.", serializeWithRoot(HenkilönOpiskeluoikeusVersiot(OidHenkilö("1.2.246.562.24.00000000001"), List(OpiskeluoikeusVersio("1.2.246.562.15.20916518804", 3, Some(LähdejärjestelmäId(Some("1"), Koodistokoodiviite("lahdejarjestelma_primus", "lahdejarjestelma"))))))))
    val searchOk = subcategory("searchOk", "Haku onnistui.")
    val maybeEmptyList = subcategory("maybeEmptyList", "Haku onnistui. Myös silloin kun ei löydy yhtään tulosta ja palautetaan tyhjä lista.")
    val maybeValidationErrorsInContent = subcategory("maybeValidationErrorsInContent", "Haku onnistui. Mahdolliset validointivirheet palautetaan json-vastauksessa.")
  }

  object badRequest extends ErrorCategory("badRequest", 400, "Epäkelpo syöte") {
    class Format extends ErrorCategory(badRequest, "format", "Epäkelpo syötteen formaatti.") {
      val number = subcategory("number", "Numeromuotoisen parametrin parsinta epäonnistui.")
      val json = subcategory("json", "JSON-dokumentin parsinta epäonnistui.")
      val xml = subcategory("xml", "XML-dokumentin parsinta epäonnistui.")
      val pvm = subcategory("date", "Päivämäärän parsinta epäonnistui.")
    }
    val format = new Format

    class QueryParam extends ErrorCategory(badRequest, "queryParam", "Epäkelpo kyselyparametri") {
      val unknown = subcategory("unknown", "Annettua kyselyparametria ei tueta.")
      val searchTermTooShort = subcategory("searchTermTooShort", "Hakusanan pituus alle 3 merkkiä.")
      val virheellinenHenkilöOid = subcategory("virheellinenHenkilöOid", "Henkilö-oidin muoto on virheellinen. Esimerkki oikeasta muodosta: 1.2.246.562.24.00000000001.")
      val virheellinenOpiskeluoikeusOid = subcategory("virheellinenOpiskeluoikeusOid", "Opiskeluoikeus-oidin muoto on virheellinen. Esimerkki oikeasta muodosta: 1.2.246.562.15.00000000001.")
      val virheellinenOrganisaatioOid = subcategory("virheellinenOrganisaatioOid", "Organisaatio-oidin muoto on virheellinen. Esimerkki oikeasta muodosta: 1.2.246.562.10.00000000001.")
      val missing = subcategory ("missing", "Vaadittu kyselyparametri puuttuu")
      val invalidXRoadMemberId = subcategory("invalid", "Annettua valtuutuksen kumppani-parametria ei ole olemassa")
      val invalidCallbackParameter = subcategory("callback", "Callback-parametrin arvoa ei ole sallittu, redirect estetään")
    }
    val queryParam = new QueryParam

    class Header extends ErrorCategory(badRequest, "header", "Epäkelpo otsikkokenttä") {
      val missingXRoadHeader = subcategory("missing", "Vaadittu X-ROAD-MEMBER http-otsikkokenttä puuttuu")
      val invalidXRoadHeader = subcategory("invalid", "X-ROAD-MEMBER:n tunnistetta ei ole olemassa")
    }
    val header = new Header

    class Validation extends ErrorCategory(badRequest, "validation", "Syötteen validointi epäonnistui") {
      val jsonSchema = subcategory("jsonSchema", "JSON-schema -validointi epäonnistui. Paluuviestin sisällä virheilmoitukset JSON-muodossa.", JsonValidationErrorExample.example)
      val tyhjäOpiskeluoikeusLista = subcategory("tyhjäOpiskeluoikeusLista", "Annettiin tyhjä lista opiskeluoikeuksia.")

      class Organisaatio extends ErrorCategory(Validation.this, "organisaatio", "Epäkelpo organisaatio") {
        val tuntematon = subcategory("tuntematon", "Tuntematon organisaatio: oid-tunnisteella ei löytynyt organisaatiota.")
        val eiOppilaitos = subcategory("eiOppilaitos", "Toimipisteenä käytetylle organisaatiolle ei löydy oppilaitos-tyyppistä yliorganisaatiota.")
        val vääräKoulutustoimija = subcategory("vääräKoulutustoimija", "Koulutustoimija ei vastaa organisaatiopalvelun mukaista tietoa")
        val järjestämismuoto = subcategory("järjestämismuoto", "Opiskeluoikeutta ei voi tallentaa oman organisaation toimipaikkaan jos järjestämismuotona ostopalvelu tai palveluseteli")
        val koulutustoimijaPakollinen = subcategory("koulutustoimijaPakollinen", "Koulutustoimijaa ei voi yksiselitteisesti päätellä.")
        val oppilaitosPuuttuu = subcategory("oppilaitosPuuttuu", "Oppilaitos puuttuu")
      }
      val organisaatio = new Organisaatio

      class SisältäväOpiskeluoikeus extends ErrorCategory(Validation.this, "sisältäväOpiskeluoikeus", "Sisältävä opiskeluoikeus") {
        val eiLöydy = subcategory("eiLöydy", "Sisältävää opiskeluoikeutta ei löydy id-arvolla")
        val vääräOppilaitos = subcategory("vääräOppilaitos", "Sisältävän opiskeluoikeuden oppilaitos ei täsmää")
        val henkilöTiedot = subcategory("henkilöTiedot", "Sisältävän opiskeluoikeuden henkilö-oid ei vastaa syötettyjä henkilötietoja, tai henkilöä ei löydetty syötetyllä henkilötunnuksella")
      }
      val sisältäväOpiskeluoikeus = new SisältäväOpiskeluoikeus

      class Henkilötiedot extends ErrorCategory(Validation.this, "henkilötiedot", "Epäkelvot henkilötiedot") {
        val virheelliset = subcategory("puutteelliset", "Henkilötiedot virheelliset henkilöpalvelun tekemien tarkistusten perusteella.")
        val hetu = subcategory("hetu", "Henkilötunnus on virheellinen.")
      }
      val henkilötiedot = new Henkilötiedot

      class Date extends ErrorCategory(Validation.this, "date", "Päivämäärä on oikeassa formaatissa, mutta semanttisesti epäkelpo.") {
        val päättymisPäiväEnnenAlkamispäivää = subcategory("päättymisPäiväEnnenAlkamispäivää", "Opiskeluoikeuden päättymispäivä on aiempi kuin alkamispäivä")
        val arvioituPäättymisPäiväEnnenAlkamispäivää = subcategory("arvioituPäättymisPäiväEnnenAlkamispäivää", "Opiskeluoikeuden arvioitu päättymispäivä on aiempi kuin alkamispäivä")
        val opiskeluoikeusjaksojenPäivämäärät = subcategory("opiskeluoikeusjaksojenPäivämäärät", "Opiskeluoikeusjaksojen on oltava päivämääräjärjestyksessä")

        val arviointiEnnenAlkamispäivää = subcategory("arviointiEnnenAlkamispäivää", "Suorituksen arviointipäivä on aiempi kuin sen alkamispäivä")
        val vahvistusEnnenAlkamispäivää = subcategory("vahvistusEnnenAlkamispäivää", "Suorituksen vahvistuksen päivämäärä on aiempi kuin suorituksen alkamispäivä")
        val vahvistusEnnenArviointia = subcategory("vahvistusEnnenArviointia", "Suorituksen vahvistuksen päivämäärä on aiempi kuin sen arviointipäivä")
        val päättymispäiväEnnenVahvistusta = subcategory("päättymispäiväEnnenVahvistusta", "Opiskeluoikeuden päättymispäivä on aiempi kuin suorituksen vahvistuksen päivämäärä")
        val suorituksenVahvistusEnnenSuorituksenOsanVahvistusta = subcategory("suorituksenVahvistusEnnenSuorituksenOsanVahvistusta", "Suorituksen vahvistuksen päivämäärä on aiempi kuin suorituksen osan vahvistuksen päivämäärä")
        val suorituksenVahvistusEnnenSuorituksenOsanArviointia = subcategory("suorituksenVahvistusEnnenSuorituksenOsanArviointia", "Suorituksen vahvistuksen päivämäärä on aiempi kuin suorituksen osan arvioinnin päivämäärä")
        val suorituksenAlkamispäiväEnnenOpiskeluoikeudenAlkamispäivää = subcategory("suorituksenAlkamispäiväEnnenOpiskeluoikeudenAlkamispäivää", "Suorituksen alkamispäivä on aiempi kuin opiskeluoikeuden alkamispäivä")

        val alkamispäivä = subcategory("alkamispäivä", "Opiskeluoikeuden alkamispäivä ei vastaa ensimmäisen opiskeluoikeusjakson alkupäivää")
        val päättymispäivämäärä = subcategory("päättymispäivämäärä", "Opiskeluoikeuden päättymispäivä ei vastaa opiskeluoikeuden päättävän opiskeluoikeusjakson alkupäivää")
      }
      val date = new Date

      class Koodisto extends ErrorCategory(Validation.this, "koodisto", "Koodistoihin liittyvä tarkistusvirhe") {
        val tuntematonKoodi = subcategory("tuntematonKoodi", "Annettua koodia ei löydy koodistosta.")
        val koulutustyyppiPuuttuu = subcategory("koulutustyyppiPuuttuu", "Annetun koulutuksen koulutustyyppiä ei löydy koodistosta.")
        val vääräKoulutuksenTunniste = subcategory("vääräKoulutuksenTunniste", "Koulutuksen tunniste on väärä")
      }
      val koodisto = new Koodisto

      class Rakenne extends ErrorCategory(Validation.this, "rakenne", "Tutkinnon rakenteeseen liittyvä validointivirhe") {
        val tuntematonTutkinnonOsa = subcategory("tuntematonTutkinnonOsa", "Annettua tutkinnon osaa ei löydy rakenteesta.")
        val suoritustapaPuuttuu = subcategory("suoritustapaPuuttuu", "Tutkinnolta puuttuu suoritustapa. Tutkinnon osasuorituksia ei hyväksytä.")
        val suoritustapaaEiLöydyRakenteesta = subcategory("suoritustapaaEiLöydyRakenteesta", "Suoritustapaa ei löydy tutkinnon rakenteesta")
        val diaariPuuttuu = subcategory("diaariPuuttuu", "Annettiin koulutus ilman perusteen diaarinumeroa. Diaarinumero on pakollinen päätason suorituksilla.")
        val tuntematonDiaari = subcategory("tuntematonDiaari", "Tutkinnon perustetta ei löydy diaarinumerolla.")
        val vääräDiaari = subcategory("vääräDiaari", "Väärä diaarinumero")
        val tuntematonOsaamisala = subcategory("tuntematonOsaamisala", "Annettua osaamisalaa ei löydy tutkinnon rakenteesta.")
        val vääräKoulutustyyppi = subcategory("vääräKoulutustyyppi", "Tutkinnon koulutustyyppi on väärä")
        val tutkinnonOsanRyhmäPuuttuu = subcategory("tutkinnonOsanRyhmäPuuttuu", "Tutkinnonosan ryhmä on pakollinen ammatillisen perustutkinnon tutkinnonosille")
        val koulutustyyppiEiSalliTutkinnonOsienRyhmittelyä = subcategory("koulutustyyppiEiSalliTutkinnonOsienRyhmittelyä", "Tutkinnonosan ryhmä voidaan määritellä vain ammatillisen perustutkinnon tutkinnonosille")
        val samaTutkintokoodi = subcategory("samaTutkintokoodi", "Jos tutkinnon osa kuuluu päätason suorituksen tutkintoon, ei tutkinnon osan suorituksessa tarvitse/saa olla erillistä tutkintotietoa")
        val duplikaattiOsasuoritus = subcategory("duplikaattiOsasuoritus", "Duplikaatti osasuoritus päätason suorituksella")
        val kaksiSamaaOppiainettaNumeroarvioinnilla = subcategory("kaksiSamaaOppiainettaNumeroarvioinnilla", "Kahdella saman oppiaineen suorituksella ei molemmilla voi olla numeerista arviointia")
        val luokkaAstePuuttuu = subcategory("luokkaAstePuuttuu", "Luokka-aste vaaditaan kun viimeisin arviointi on muuta kuin 'O'")
        val tunnisteenKoodiarvoaEiLöydyRakenteesta = subcategory("eriTutkintokoodit", "Suorituksen tunnisteen koodiarvo ei löydy tutkinnon rakenteesta")
        val deprekoituOsaamisenHankkimistapa = subcategory("deprekoituOsaamisenHankkimistapa", "Osaamisenhankkimistapaa oppisopimus ei voi tallentaa ilman y-tunnusta")
        val yhteiselläOsuudellaEiOsasuorituksia = subcategory("yhteiselläOsuudellaEiOsasuorituksia", "Tutkinnon yhteisellä osalla tulee olla osasuorituksia")
        val vääränKoodinYhteinenOsasuoritus = subcategory("vääränKoodinYhteinenOsasuoritus", "Tutkinnon yhteisellä osalla on epäsopivan suoritustyypin alaisen suorituksen koodi")
        val epäsopiviaSuorituksia = subcategory("epäsopiviaSuorituksia", "Opiskeluoikeudella on epäsopiva lista suorituksia")
        val erityisenäTutkintonaSuoritettuSisältääOsasuorituksia = subcategory("erityisenäTutkintoaSuoritettuSisältääOsasuorituksia", "Osasuorituksella on osasuorituksia, vaikka se on suoritettu erityisenä tutkintona")
        val epäsopiviaOsasuorituksia = subcategory("epäsopiviaOsasuorituksia", "Suorituksella tai sen osasuorituksella on epäsopiva lista osasuorituksia")
        val deprekoituKielikoodi = subcategory("deprekoituKielikoodi", "Suorituksessa on käytetty deprekoitua kielikoodia")
        val deprekoituOppimäärä = subcategory("deprekoituOppimäärä", "Suorituksessa on käytetty deprekoitua oppimäärää")
        val puuttuvaSuullisenKielitaidonKoe = subcategory("puuttuvaSuullisenKielitaidonKoe", "Suorituksesta puuttuu vaadittava merkintä suullisen kielitaidon kokeesta")
        val lukioonValmistavassaEriLukioOpsienOsasuorituksia = subcategory("lukioonValmistavassaEriLukioOpsienSuorituksia", "Lukion valmistavan koulutuksen suorituksella ei voi olla sekä lukion 2015 että lukion 2019 opetussuunnitelmien mukaisia osasuorituksia")
        val lukioonValmistavassaVanhanOpsinKurssiSuorituksia = subcategory("lukioonValmistavassaVanhanOpsinKurssiSuorituksia", "Lukion valmistavan koulutuksen suorituksella ei voi 1.8.2021 jälkeen olla luva2015-opetussuunnitelman mukaisia suorituksia")
      }
      val rakenne = new Rakenne

      class Tila extends ErrorCategory(Validation.this, "tila", "Suorituksen tilaan liittyvä validointivirhe") {
        val tyhjänOppiaineenVahvistus = subcategory("tyhjänOppiaineenVahvistus", "Tyhjän oppiaineen suoritus merkitty valmiiksi")
        val vahvistusIlmanArviointia = subcategory("vahvistusIlmanArviointia", "Suoritukselta puuttuu arviointi, vaikka sillä on vahvistus")
        val keskeneräinenOsasuoritus = subcategory("keskeneräinenOsasuoritus", "Valmiiksi merkityllä suorituksella on keskeneräinen osasuoritus")
        val vahvistusPuuttuu = subcategory("vahvistusPuuttuu", "Suoritukselta puuttuu vahvistus, vaikka opiskeluoikeus on tilassa Valmistunut")
        val suoritusPuuttuu = subcategory("suoritusPuuttuu", "Opiskeluoikeutta ei voi merkitä valmiiksi kun siitä puuttuu tarvittava suoritus")
        val oppiaineetPuuttuvat = subcategory("oppiaineetPuuttuvat", "Suorituksella ei ole osasuorituksena yhtään oppiainetta, vaikka sillä on vahvistus")
        val oppiaineitaEiSallita = subcategory("oppiaineitaEiSallita", "9.vuosiluokan suoritukseen ei voi syöttää oppiaineita, kun sillä on vahvistus, eikä oppilas jää luokalle")
        val tilaMuuttunutLopullisenTilanJälkeen = subcategory("tilaMuuttunutLopullisenTilanJälkeen", "Opiskeluoikeuden tilojen valmistunut, eronnut jälkeen ei voi esiintyä muita tiloja")
        val montaPäättävääTilaa = subcategory("montaPäättäväätilaa", "Opiskeluoikeudella voi olla vain yksi opiskeluoikeuden päättävä tila")
        val alkamispäiväPuuttuu = subcategory("alkamispäiväPuuttuu", "Suoritukselle ei ole merkitty alkamispäivää")
        val valmiiksiMerkityltäPuuttuuOsasuorituksia = subcategory("valmiiksiMerkitylläEiOsasuorituksia", "Valmiiksi merkityllä päätason suorituksella ei voi olla tyhjä osasuorituslista tai opiskeluoikeuden pitää olla linkitetty")
        val tilaltaPuuttuuRahoitusmuoto = subcategory("tilaltaPuuttuuRahoitusmuoto", "Opiskeluoikeuden tilalta vaaditaan rahoitusmuoto")
        val tilallaEiSaaOllaRahoitusmuotoa = subcategory("tilallaEiSaaOllaRahoitusmuotoa", "Opiskeluoikeuden tilalla ei saa olla rahoitusmuotoa")
        val osasuoritusPuuttuu = subcategory("osasuoritusPuuttuu", "Opiskeluoikeutta ei voi merkitä valmiiksi kun sen suoritukselta puuttuu tarvittava osasuoritus")
        val vapaanSivistystyönVahvistetunPäätasonSuorituksenLaajuus = subcategory("vapaaSivistysTyöLaajuusVäärä", "Vahvistetuksi merkatun vapaan sivistyöstyön koulutuksen laajuuden tulee olla 53")
        val vapaanSivistystyönKOTOVahvistettuPäätasoHylätyilläOsasuorituksilla = subcategory("vapaanSivistystyönKOTOVahvistettuPäätasoHylätyilläOsasuorituksilla", "Vapaan sivistyöstyön maahanmuuttajien kotoutumiskoulutuksen suoritus on vahvistettu, vaikka sillä on osasuorituksia, joita ei ole arvioitu hyväksytyksi")
        val vapaanSivistystyönVapaatavoitteisenKoulutuksenVahvistettuPäätasoArvioimattomillaOsasuorituksilla= subcategory("vapaanSivistystyönVapaatavoitteisenKoulutuksenVahvistettuPäätasoArvioimattomillaOsasuorituksilla", "Vapaatavoitteisen vapaan sivistyöstyön koulutuksen suoritus on vahvistettu, vaikka sillä on osasuorituksia, joilta puuttuu arviointi")
        val nuortenPerusopetuksenValmistunutTilaIlmanVahvistettuaPäättötodistusta = subcategory("nuortenPerusopetuksenValmistunutTilaIlmanVahvistettuaPäättötodistusta", "Nuorten perusopetuksen opiskeluoikeutta ei voida päättää tilalla 'valmistunut', jos opiskeluoikeudelta puuttuu vahvistettu päättötodistus")
      }
      val tila = new Tila

      class Laajuudet extends ErrorCategory(Validation.this, "laajuudet", "Suoritusten laajuuksiin liittyvä validointivirhe") {
        val osasuorituksellaEriLaajuusyksikkö = subcategory("osasuorituksellaEriLaajuusyksikkö", "Osasuorituksella on eri laajuusyksikkö kuin ylemmän tason suorituksella")
        val osasuoritustenLaajuuksienSumma = subcategory("osasuoritustenLaajuuksienSumma", "Osasuoritusten laajuuksien summa ei täsmää")
        val oppiaineenLaajuusPuuttuu = subcategory("oppiaineenLaajuusPuuttuu", "Oppiaineen laajuus puuttuu")
        val lukiodiplominLaajuusEiOle2Opintopistettä = subcategory("lukiodiplominLaajuusVäärä", "Lukiodiplomin laajuuden on oltava aina 2 opintopistettä")
      }
      val laajuudet = new Laajuudet

      class Arviointi extends ErrorCategory(Validation.this, "arviointi", "Suoritusten arviointiin liittyvä validointivirhe") {
        val vääräHyväksyttyArvo = subcategory("vääräHyväksyttyArvo", "hyväksytty-kentän arvo ristiriidassa annetun arvosanan kanssa")
        val useitaArviointiasteikoita = subcategory("useitaArviointiAsteikoita", "Ammatillinen päätason suoritus voi käyttää vain yhtä numeerista arviointiasteikkoa")
        val sallittuVainValinnaiselle = subcategory("sallittuVainValinnaiselle", "Arvioinnit 'S' ja 'O' on sallittuja vain valinnaisille oppiaineille joiden laajuus on alle kaksi vuosiviikkotuntia")
        val eiSallittuSuppealleValinnaiselle = subcategory("eiSallittuSuppealleValinnaiselle", "Vain arvioinnit 'S' ja 'O' on sallittu valinnaiselle valtakunnalliselle oppiaineelle, jonka laajuus on alle kaksi vuosiviikkotuntia")
        val sallittuVainSuppealle = subcategory("sallittuVainSuppealle", "Arvioinnit 'S' ja 'H' on sallittu vain riittävän suppealle oppiaineelle")
        val epäsopivaArvosana = subcategory("epäsopivaArvosana", "Arvosanaa ei ole sallittu suoritukselle")
      }
      val arviointi = new Arviointi

      class OsaAikainenErityisopetus extends ErrorCategory(
        Validation.this,
        "osaaikainenerityisopetus",
        "Osa-aikaisen erityisopetuksen kirjauksiin liittyvä validointivirhe"
      ) {
        val kirjausPuuttuuSuorituksesta = subcategory(
          "kirjausPuuttuuSuorituksesta",
          "Jos osa-aikaisesta erityisopetuksesta on päätös opiskeluoikeuden lisätiedoissa, se pitää kirjata myös suoritukseen"
        )
      }
      val osaAikainenErityisopetus = new OsaAikainenErityisopetus
    }
    val validation = new Validation
  }

  object unauthorized extends ErrorCategory("unauthorized", 401, "Käyttäjä ei ole tunnistautunut.") {
    val notAuthenticated = subcategory("notAuthenticated", "Käyttäjä ei ole tunnistautunut.")
    val loginFail = subcategory("loginFail", "Sisäänkirjautuminen epäonnistui.")
  }

  object forbidden extends ErrorCategory("forbidden", 403, "Käyttäjällä ei ole oikeuksia annetun organisaation tietoihin.") {
    val organisaatio = subcategory("organisaatio", "Käyttäjällä ei oikeuksia annettuun organisaatioon (esimerkiksi oppilaitokseen).")
    val opiskeluoikeudenTyyppi = subcategory("opiskeluoikeudenTyyppi", "Käyttäjälle ei ole oikeuksia annettuun opiskeluoikeuden tyyppiin")
    val kiellettyMuutos = subcategory("kiellettyMuutos", "Yritetty muuttaa opiskeluoikeuden perustietoja (oppilaitos, tyyppi...)")
    val ainoanPäätasonSuorituksenPoisto = subcategory("ainoanPäätasonSuorituksenPoisto", "Yritetty poistaa opiskeluoikeuden ainoaa päätason suoritusta")
    val lähdejärjestelmäIdPuuttuu = subcategory("lähdejärjestelmäIdPuuttuu", "Käyttäjä on palvelukäyttäjä mutta lähdejärjestelmää ei ole määritelty")
    val lähdejärjestelmäIdEiSallittu = subcategory("lähdejärjestelmäIdEiSallittu", "Lähdejärjestelmä määritelty, mutta käyttäjä ei ole palvelukäyttäjä")
    val juuriorganisaatioPuuttuu = subcategory("juuriorganisaatioPuuttuu", "Automaattisen tiedonsiirron palvelukäyttäjällä ei yksiselitteistä juuriorganisaatiota")
    val omienTietojenMuokkaus = subcategory("omienTietojenMuokkaus", "Omien tietojen muokkaus ei ole sallittua.")
    val oppijaOidinMuutos = subcategory("oppijaOidinMuutos", "Oppija OID ei vastaa opiskeluoikeuteen kuuluvaa oppija OID:a")
    val vainVarhaiskasvatuksenJärjestäjä = subcategory("vainVarhaiskasvatuksenJärjestäjä", "Operaatio on sallittu vain käyttäjälle joka on luotu varhaiskasvatusta järjestävälle koulutustoimijalle")
    val vainVirkailija = subcategory("vainVirkailija", "Sallittu vain virkailija-käyttäjille")
    val vainKansalainen = subcategory("vainKansalainen", "Sallittu vain kansalainen-käyttäjille")
    val vainViranomainen = subcategory("vainViranomainen", "Sallittu vain viranomaisille")
    val vainTilastokeskus = subcategory("vainTilastokeskus", "Sallittu vain tilastokeskuskäyttäjälle")
    val kiellettyKäyttöoikeus = subcategory("kiellettyKäyttöoikeus", "Ei sallittu näillä käyttöoikeuksilla")
    val liianMontaSuoritusjakoa = subcategory("liianMontaSuoritusjakoa", "Käyttäjällä on jo maksimimäärä suoritusjakoja")
    val forbiddenXRoadHeader = subcategory("vainSallittuKumppani", "X-ROAD-MEMBER:llä ei ole lupaa hakea opiskelijan tietoja")
  }

  object notFound extends ErrorCategory("notFound", 404, "Not found") {
    val oppijaaEiLöydyTaiEiOikeuksia = subcategory("oppijaaEiLöydyTaiEiOikeuksia", "Oppijaa ei löydy annetulla oidilla tai käyttäjällä ei ole oikeuksia tietojen katseluun.")
    val oppijaaEiLöydy = subcategory("oppijaaEiLöydy", "Oppijaa ei löydy annetulla oidilla.")
    val opiskeluoikeuttaEiLöydyTaiEiOikeuksia = subcategory("opiskeluoikeuttaEiLöydyTaiEiOikeuksia", "Opiskeluoikeutta ei löydy annetulla oid:llä tai käyttäjällä ei ole siihen oikeuksia")
    val opiskeluoikeuttaOppilaitoksessaEiLöydy = subcategory("opiskeluoikeuttaOppilaitoksessaEiLöydy", "Oppijalla ei ole opiskeluoikeutta oppilaitoksessa tai käyttäjällä ei ole siihen oikeuksia")
    val versiotaEiLöydy = subcategory("versiotaEiLöydy", "Haettua versiota ei löydy")
    val koodistoaEiLöydy = subcategory("koodistoaEiLöydy", "Pyydettyä koodistoa ei löydy.")
    val diaarinumeroaEiLöydy = subcategory("diaarinumeroaEiLöydy", "Tutkinnon rakennetta ei löydy annetulla diaarinumerolla.")
    val suoritustapaaEiLöydy = subcategory("suoritustapaaEiLöydy", "Valittua suoritustapaa ei löydy annetulla diaarinumerolla.")
    val todistustaEiLöydy = subcategory("todistustaEiLöydy", "Pyydettyä todistusta ei löydy")
    val suoritustaEiLöydy = subcategory("suoritustaEiLöydy", "Pyydettyä suoritusta ei löydy")
    val oppilaitostaEiLöydy = subcategory("oppilaitostaEiLöydy", "Oppilaitosta ei löydy organisaatiopalvelusta.")
    val tutkintoaEiLöydy = subcategory("tutkintoaEiLöydy", "Tutkintoa ei löydy hakusanalla")
    val ryhmääEiLöydyRakenteesta = subcategory("ryhmääEiLöydyRakenteesta", "Pyydettyä tutkinnon osien ryhmää ei löydy tutkintorakenteesta")
    val myDataMemberEiLöydy = subcategory("myDataMemberEilöydy", "OmaData kumppania ei löydy")
  }

  object conflict extends ErrorCategory("conflict", 409, "Ristiriitainen päivitys")  {
    val versionumero = subcategory("versionumero", "Yritetty päivittää vanhan version päälle; annettu versionumero on erisuuri kuin viimeisin rekisteristä löytyvä.")
    val samanaikainenPäivitys = subcategory("samanaikainenPäivitys", "Toinen käyttäjä on päivittänyt saman opiskeluoikeuden tietoja samanaikaisesti. Yritä myöhemmin uudelleen.")
    val exists = subcategory("exists", "Vastaava opiskeluoikeus on jo olemassa.")
    val hetu = subcategory("hetu", "Henkilö on jo lisätty annetulla hetulla.")
  }

  object unsupportedMediaType extends ErrorCategory("unsupportedMediaType", 415, "Unsupported media type") {
    val jsonOnly = subcategory("jsonOnly", "Wrong content type: only application/json content type with UTF-8 encoding allowed")
  }

  object internalError extends ErrorCategory("internalError", 500, "Internal server error")

  object notImplemented extends ErrorCategory("notImplemented", 501, "Not implemented") {
    val readOnly = subcategory("readOnly", "Opiskeluoikeuden tietoja ei voi muuttaa")
  }

  object unavailable extends ErrorCategory("unavailable", 503, "Service unavailable") {
    val virta = subcategory("virta", "Korkeakoulutuksen opiskeluoikeuksia ei juuri nyt saada haettua. Yritä myöhemmin uudelleen.")
    val ytr = subcategory("ytr", "Ylioppilastutkintojen tietoja ei juuri nyt saada haettua. Yritä myöhemmin uudelleen.")
    val raportit = subcategory("raportit", "Raportit eivät ole juuri nyt käytettävissä. Yritä myöhemmin uudelleen.")
    val huollettavat = subcategory("huollettavat", "Huollettavien opintotietoja ei juuri nyt saada haettua. Yritä myöhemmin uudelleen.")
  }
}
