package fi.oph.koski.raportit.lukio

import fi.oph.koski.db.DatabaseConverters
import fi.oph.koski.db.PostgresDriverWithJsonSupport.plainAPI._
import fi.oph.koski.raportit.{Column, DataSheet}
import fi.oph.koski.raportointikanta.{RaportointiDatabase, Schema}
import slick.jdbc.GetResult

import java.sql.ResultSet
import java.time.LocalDate

object LukioOppiaineenOppimaaranKurssikertymat extends DatabaseConverters {
  val sheetTitle = "Aineopiskelijat"

  def datasheet(oppilaitosOids: List[String], jaksonAlku: LocalDate, jaksonLoppu: LocalDate, raportointiDatabase: RaportointiDatabase): DataSheet = {
    DataSheet(
      sheetTitle,
      rows = raportointiDatabase.runDbSync(queryAineopiskelija(oppilaitosOids, jaksonAlku, jaksonLoppu)),
      columnSettings
    )
  }

  def createMaterializedView(s: Schema) =
    sqlu"""
      create materialized view #${s.name}.lukion_oppiaineen_oppimaaran_kurssikertyma as select
        oppilaitos_oid,
        arviointi_paiva,
        count(*) yhteensa,
        count(*) filter (where suoritettu) suoritettuja,
        count(*) filter (where tunnustettu) tunnustettuja,
        count(*) filter (where tunnustettu_rahoituksen_piirissa) tunnustettuja_rahoituksen_piirissa,
        count(*) filter (where pakollinen or (valtakunnallinen and syventava)) pakollisia_tai_valtakunnallisia_syventavia,
        count(*) filter (where pakollinen) pakollisia,
        count(*) filter (where valtakunnallinen and syventava) valtakunnallisia_syventavia,
        count(*) filter (where suoritettu and (pakollinen or (valtakunnallinen and syventava))) suoritettuja_pakollisia_ja_valtakunnallisia_syventavia,
        count(*) filter (where pakollinen and suoritettu) suoritettuja_pakollisia,
        count(*) filter (where suoritettu and valtakunnallinen and syventava) suoritettuja_valtakunnallisia_syventavia,
        count(*) filter (where tunnustettu and (pakollinen or (syventava and valtakunnallinen))) tunnustettuja_pakollisia_ja_valtakunnallisia_syventavia,
        count(*) filter (where tunnustettu and pakollinen) tunnustettuja_pakollisia,
        count(*) filter (where tunnustettu and valtakunnallinen and syventava) tunnustettuja_valtakunnallisia_syventavia,
        count(*) filter (where tunnustettu_rahoituksen_piirissa and (pakollinen or (valtakunnallinen and syventava))) tunnustut_pakolliset_ja_valtakunnalliset_syventavat_rahoitus,
        count(*) filter (where tunnustettu_rahoituksen_piirissa and pakollinen) pakollisia_tunnustettuja_rahoituksen_piirissa,
        count(*) filter (where valtakunnallinen and syventava and tunnustettu_rahoituksen_piirissa) valtakunnallisia_syventavia_tunnustettuja_rahoituksen_piirissa,
        count(*) filter (where korotettu_eri_vuonna) eri_vuonna_korotettuja
      from (
        select
          oppilaitos_oid,
          osasuoritus.arviointi_paiva,
          tunnustettu,
          tunnustettu = false as suoritettu,
          tunnustettu_rahoituksen_piirissa,
          koulutusmoduuli_kurssin_tyyppi,
          koulutusmoduuli_kurssin_tyyppi = 'pakollinen' as pakollinen,
          koulutusmoduuli_kurssin_tyyppi = 'syventava' as syventava,
          koulutusmoduuli_paikallinen = false as valtakunnallinen,
          opintojen_rahoitus,
          korotettu_eri_vuonna
        from #${s.name}.r_paatason_suoritus paatason_suoritus
          join #${s.name}.r_opiskeluoikeus opiskeluoikeus
            on paatason_suoritus.opiskeluoikeus_oid = opiskeluoikeus.opiskeluoikeus_oid
          join #${s.name}.r_osasuoritus osasuoritus
            on paatason_suoritus.paatason_suoritus_id = osasuoritus.paatason_suoritus_id
            and osasuoritus.arviointi_arvosana_koodiarvo != 'O'
          left join #${s.name}.r_opiskeluoikeus_aikajakso opiskeluoikeus_aikajakso
            on opiskeluoikeus_aikajakso.opiskeluoikeus_oid = osasuoritus.opiskeluoikeus_oid
              and (osasuoritus.arviointi_paiva between opiskeluoikeus_aikajakso.alku and opiskeluoikeus_aikajakso.loppu)
        where paatason_suoritus.suorituksen_tyyppi = 'lukionoppiaineenoppimaara'
          and osasuoritus.suorituksen_tyyppi = 'lukionkurssi'
      ) kurssit
    group by
      oppilaitos_oid,
      arviointi_paiva
    """

  def createIndex(s: Schema) =
    sqlu"create index on #${s.name}.lukion_oppiaineen_oppimaaran_kurssikertyma(oppilaitos_oid, arviointi_paiva)"


  private def queryAineopiskelija(oppilaitosOids: List[String], aikaisintaan: LocalDate, viimeistaan: LocalDate) = {
    sql"""
      select
        r_organisaatio.nimi oppilaitos,
        oppimaaran_kurssikertymat.*,
        coalesce(muuta_kautta_rahoitetut.yhteensa, 0) as muuta_kautta_rahoitetut,
        coalesce(rahoitusmuoto_ei_tiedossa.yhteensa, 0) as rahoitusmuoto_ei_tiedossa,
        coalesce(opiskeluoikeuden_ulkopuoliset.yhteensa, 0) as opiskeluoikeuden_ulkopuoliset
      from (
        select
          oppilaitos_oid,
          sum(yhteensa) yhteensa,
          sum(suoritettuja) suoritettuja,
          sum(tunnustettuja) tunnustettuja,
          sum(tunnustettuja_rahoituksen_piirissa) tunnustettuja_rahoituksen_piirissa,
          sum(pakollisia_tai_valtakunnallisia_syventavia) pakollisia_tai_valtakunnallisia_syventavia,
          sum(pakollisia) pakollisia,
          sum(valtakunnallisia_syventavia) valtakunnallisia_syventavia,
          sum(suoritettuja_pakollisia_ja_valtakunnallisia_syventavia) suoritettuja_pakollisia_ja_valtakunnallisia_syventavia,
          sum(suoritettuja_pakollisia) suoritettuja_pakollisia,
          sum(suoritettuja_valtakunnallisia_syventavia) suoritettuja_valtakunnallisia_syventavia,
          sum(tunnustettuja_pakollisia_ja_valtakunnallisia_syventavia) tunnustettuja_pakollisia_ja_valtakunnallisia_syventavia,
          sum(tunnustettuja_pakollisia) tunnustettuja_pakollisia,
          sum(tunnustettuja_valtakunnallisia_syventavia) tunnustettuja_valtakunnallisia_syventavia,
          sum(tunnustut_pakolliset_ja_valtakunnalliset_syventavat_rahoitus) tunnustut_pakolliset_ja_valtakunnalliset_syventavat_rahoitus,
          sum(pakollisia_tunnustettuja_rahoituksen_piirissa) pakollisia_tunnustettuja_rahoituksen_piirissa,
          sum(valtakunnallisia_syventavia_tunnustettuja_rahoituksen_piirissa) valtakunnallisia_syventavia_tunnustettuja_rahoituksen_piirissa,
          sum(eri_vuonna_korotettuja) eri_vuonna_korotettuja
        from lukion_oppiaineen_oppimaaran_kurssikertyma
          where oppilaitos_oid = any($oppilaitosOids)
            and arviointi_paiva between $aikaisintaan and $viimeistaan
          group by oppilaitos_oid
      ) oppimaaran_kurssikertymat
      left join (
        select
          oppilaitos_oid,
          count(*) yhteensa
        from osasuoritus_arvioitu_opiskeluoikeuden_ulkopuolella
          join r_osasuoritus on r_osasuoritus.osasuoritus_id = osasuoritus_arvioitu_opiskeluoikeuden_ulkopuolella.osasuoritus_id
          where oppilaitos_oid = any($oppilaitosOids)
            and osasuorituksen_tyyppi = 'lukionkurssi'
            and paatason_suorituksen_tyyppi = 'lukionoppiaineenoppimaara'
            and (osasuorituksen_arviointi_paiva between $aikaisintaan and $viimeistaan)
            and (
              koulutusmoduuli_kurssin_tyyppi = 'pakollinen'
              or (koulutusmoduuli_kurssin_tyyppi = 'syventava' and koulutusmoduuli_paikallinen = false)
            )
            and (
              tunnustettu = false
              or tunnustettu_rahoituksen_piirissa
            )
        group by oppilaitos_oid
      ) opiskeluoikeuden_ulkopuoliset
          on opiskeluoikeuden_ulkopuoliset.oppilaitos_oid = oppimaaran_kurssikertymat.oppilaitos_oid
      left join (
        select
          oppilaitos_oid,
          count(*) yhteensa
        from lukion_oppiaineen_oppimaaran_kurssien_rahoitusmuodot
        where opintojen_rahoitus = '6'
          and oppilaitos_oid = any($oppilaitosOids)
          and (arviointi_paiva between $aikaisintaan and $viimeistaan)
        group by oppilaitos_oid
      ) muuta_kautta_rahoitetut
          on muuta_kautta_rahoitetut.oppilaitos_oid = oppimaaran_kurssikertymat.oppilaitos_oid
      left join (
        select
          oppilaitos_oid,
          count(*) yhteensa
        from lukion_oppiaineen_oppimaaran_kurssien_rahoitusmuodot
        where opintojen_rahoitus is null
          and oppilaitos_oid = any($oppilaitosOids)
          and (arviointi_paiva between $aikaisintaan and $viimeistaan)
        group by oppilaitos_oid
      ) rahoitusmuoto_ei_tiedossa
          on rahoitusmuoto_ei_tiedossa.oppilaitos_oid = oppimaaran_kurssikertymat.oppilaitos_oid
      join r_organisaatio on oppimaaran_kurssikertymat.oppilaitos_oid = r_organisaatio.organisaatio_oid
    """.as[LukioKurssikertymaAineopiskelijaRow]
  }

  implicit private val getResult: GetResult[LukioKurssikertymaAineopiskelijaRow] = GetResult(r => {
    val rs: ResultSet = r.rs
    LukioKurssikertymaAineopiskelijaRow(
      oppilaitosOid = rs.getString("oppilaitos_oid"),
      oppilaitos = rs.getString("oppilaitos"),
      kurssejaYhteensa = rs.getInt("yhteensa"),
      suoritettujaKursseja = rs.getInt("suoritettuja"),
      tunnustettujaKursseja = rs.getInt("tunnustettuja"),
      tunnustettujaKursseja_rahoituksenPiirissa = rs.getInt("tunnustettuja_rahoituksen_piirissa"),
      pakollisia_tai_valtakunnallisiaSyventavia = rs.getInt("pakollisia_tai_valtakunnallisia_syventavia"),
      pakollisiaKursseja = rs.getInt("pakollisia"),
      valtakunnallisestiSyventaviaKursseja = rs.getInt("valtakunnallisia_syventavia"),
      suoritettujaPakollisia_ja_suoritettujaValtakunnallisiaSyventavia = rs.getInt("suoritettuja_pakollisia_ja_valtakunnallisia_syventavia"),
      suoritettujaPakollisiaKursseja = rs.getInt("suoritettuja_pakollisia"),
      suoritettujaValtakunnallisiaSyventaviaKursseja = rs.getInt("suoritettuja_valtakunnallisia_syventavia"),
      tunnustettujaPakollisia_ja_tunnustettujaValtakunnallisiaSyventavia = rs.getInt("tunnustettuja_pakollisia_ja_valtakunnallisia_syventavia"),
      tunnustettujaPakollisiaKursseja = rs.getInt("tunnustettuja_pakollisia"),
      tunnustettujaValtakunnallisiaSyventaviaKursseja = rs.getInt("tunnustettuja_valtakunnallisia_syventavia"),
      tunnustettujaRahoituksenPiirissa_pakollisia_ja_valtakunnallisiaSyventavia = rs.getInt("tunnustut_pakolliset_ja_valtakunnalliset_syventavat_rahoitus"),
      tunnustettuja_rahoituksenPiirissa_pakollisia = rs.getInt("pakollisia_tunnustettuja_rahoituksen_piirissa"),
      tunnustettuja_rahoituksenPiirissa_valtakunnallisiaSyventaiva = rs.getInt("valtakunnallisia_syventavia_tunnustettuja_rahoituksen_piirissa"),
      suoritetutTaiRahoitetut_muutaKauttaRahoitetut = rs.getInt("muuta_kautta_rahoitetut"),
      suoritetutTaiRahoitetut_rahoitusmuotoEiTiedossa = rs.getInt("rahoitusmuoto_ei_tiedossa"),
      suoritetutTaiRahoitetut_eiOpiskeluoikeudenSisalla = rs.getInt("opiskeluoikeuden_ulkopuoliset"),
      eriVuonnaKorotettujaKursseja = rs.getInt("eri_vuonna_korotettuja"),
    )
  })

  val columnSettings: Seq[(String, Column)] = Seq(
    "oppilaitosOid" -> Column("Oppilaitoksen oid-tunniste"),
    "oppilaitos" -> Column("Oppilaitos"),
    "kurssejaYhteensa" -> Column("Kursseja yhteensä", comment = Some("Kaikki sellaiset kurssit, joiden arviointipäivämäärä osuu tulostusparametreissa määritellyn aikajakson sisään")),
    "suoritettujaKursseja" -> Column("Suoritetut kurssit", comment = Some("Kaikki sellaiset kurssit, joiden arviointipäivämäärä osuu tulostusparametreissa määritellyn aikajakson sisään ja joita ei ole merkitty tunnustetuiksi.")),
    "tunnustettujaKursseja" -> Column("Tunnustetut kurssit", comment = Some("Kaikki sellaiset kurssit, joiden arviointipäivämäärä osuu tulostusparametreissa määritellyn aikajakson sisään ja jotka on merkitty tunnustetuiksi.")),
    "tunnustettujaKursseja_rahoituksenPiirissa" -> Column("Tunnustetut kurssit - rahoituksen piirissä", comment = Some("Kaikki sellaiset kurssit, joiden arviointipäivämäärä osuu tulostusparametreissa määritellyn aikajakson sisään, jotka on merkitty tunnustetuiksi ja jotka on merkitty rahoituksen piirissä oleviksi.")),
    "pakollisia_tai_valtakunnallisiaSyventavia" -> Column("Pakollisia ja valtakunnallisia syventäviä kursseja yhteensä", comment = Some("Kaikki pakolliset ja valtakunnalliset syventävät kurssit, joiden arviointipäivämäärä osuu tulostusparametreissa määritellyn aikajakson sisään.")),
    "pakollisiaKursseja" -> Column("Pakollisia kursseja yhteensä", comment = Some("Kaikki pakolliset kurssit, joiden arviointipäivämäärä osuu tulostusparametreissa määritellyn aikajakson sisään.")),
    "valtakunnallisestiSyventaviaKursseja" -> Column("Valtakunnallisia syventäviä kursseja yhteensä", comment = Some("Kaikki valtakunnalliset syventävät kurssit, joiden arviointipäivämäärä osuu tulostusparametreissa määritellyn aikajakson sisään.")),
    "suoritettujaPakollisia_ja_suoritettujaValtakunnallisiaSyventavia" -> Column("Suoritettuja pakollisia ja valtakunnallisia syventäviä kursseja", comment = Some("Kaikki pakolliset ja valtakunnalliset syventävät kurssit, joiden arviointipäivämäärä osuu tulostusparametreissa määritellyn aikajakson sisään ja joita ei ole merkitty tunnustetuiksi.")),
    "suoritettujaPakollisiaKursseja" -> Column("Suoritettuja pakollisia kursseja", comment = Some("Kaikki pakolliset kurssit, joiden arviointipäivämäärä osuu tulostusparametreissa määritellyn aikajakson sisään ja joita ei ole merkitty tunnustetuiksi.")),
    "suoritettujaValtakunnallisiaSyventaviaKursseja" -> Column("Suoritettuja valtakunnallisia syventäviä kursseja", comment = Some("Kaikki valtakunnalliset syventävät kurssit, joiden arviointipäivämäärä osuu tulostusparametreissa määritellyn aikajakson sisään ja joita ei ole merkitty tunnustetuiksi.")),
    "tunnustettujaPakollisia_ja_tunnustettujaValtakunnallisiaSyventavia" -> Column("Tunnustettuja pakollisia ja valtakunnallisia syventäviä kursseja", comment = Some("Kaikki pakolliset ja valtakunnalliset syventävät kurssit, joiden arviointipäivämäärä osuu tulostusparametreissa määritellyn aikajakson sisään ja jotka on merkitty tunnustetuiksi.")),
    "tunnustettujaPakollisiaKursseja" -> Column("Tunnustettuja pakollisia kursseja", comment = Some("Kaikki pakolliset kurssit, joiden arviointipäivämäärä osuu tulostusparametreissa määritellyn aikajakson sisään ja jotka on merkitty tunnustetuiksi.")),
    "tunnustettujaValtakunnallisiaSyventaviaKursseja" -> Column("Tunnustettuja valtakunnallisia syventäviä kursseja", comment = Some("Kaikki valtakunnalliset syventävät kurssit, joiden arviointipäivämäärä osuu tulostusparametreissa määritellyn aikajakson sisään ja jotka on merkitty tunnustetuiksi.")),
    "tunnustettujaRahoituksenPiirissa_pakollisia_ja_valtakunnallisiaSyventavia" -> Column("Tunnustetuista pakollisista ja valtakunnallisista syventävistä kursseista rahoituksen piirissä", comment = Some("Kaikki pakolliset ja valtakunnalliset syventävät kurssit, joiden arviointipäivämäärä osuu tulostusparametreissa määritellyn aikajakson sisään ja jotka on merkitty sekä tunnustetuiksi että rahoituksen piirissä oleviksi.")),
    "tunnustettuja_rahoituksenPiirissa_pakollisia" -> Column("Tunnustetuista pakollisista kursseista rahoituksen piirissä", comment = Some("Kaikki pakolliset kurssit, joiden arviointipäivämäärä osuu tulostusparametreissa määritellyn aikajakson sisään ja jotka on merkitty sekä tunnustetuiksi että rahoituksen piirissä oleviksi.")),
    "tunnustettuja_rahoituksenPiirissa_valtakunnallisiaSyventaiva" -> Column("Tunnustetuista valtakunnallisista syventävistä kursseista rahoituksen piirissä", comment = Some("Kaikki valtakunnalliset syventävät kurssit, joiden arviointipäivämäärä osuu tulostusparametreissa määritellyn aikajakson sisään ja jotka on merkitty sekä tunnustetuiksi että rahoituksen piirissä oleviksi.")),
    "suoritetutTaiRahoitetut_muutaKauttaRahoitetut" -> Column("Suoritetut tai rahoituksen piirissä oleviksi merkityt tunnustetut kurssit - muuta kautta rahoitetut", comment = Some("Aineopintojen suoritetut tai rahoituksen piirissä oleviksi merkityt tunnustetut pakolliset tai valtakunnalliset syventävät kurssit, joiden arviointipäivä osuu muuta kautta rahoitetun läsnäolojakson sisälle. Kurssien tunnistetiedot löytyvät välilehdeltä ”Muuta kautta rah.”")),
    "suoritetutTaiRahoitetut_rahoitusmuotoEiTiedossa" -> Column("Suoritetut tai rahoituksen piirissä oleviksi merkityt tunnustetut kurssit, joilla ei rahoitustietoa", comment = Some("Aineopintojen suoritetut tai rahoituksen piirissä oleviksi merkityt tunnustetut pakolliset tai valtakunnalliset syventävät kurssit, joiden arviointipäivä osuus sellaiselle tilajaksolle, jolta ei löydy tietoa rahoitusmuodosta. Kurssien tunnistetiedot löytyvät välilehdeltä ”Ei rahoitusmuotoa”.")),
    "suoritetutTaiRahoitetut_eiOpiskeluoikeudenSisalla" -> Column("Suoritetut tai rahoituksen piirissä oleviksi merkityt tunnustetut kurssit – arviointipäivä ei opiskeluoikeuden sisällä", comment = Some("Aineopintojen suoritetut tai rahoituksen piirissä oleviksi merkityt tunnustetut pakolliset tai valtakunnalliset syventävät kurssit, joiden arviointipäivä on aikaisemmin kuin opiskeluoikeuden alkamispäivä, joiden arviointipäivä on myöhemmin kuin ”Valmistunut”-tilan päivä. Kurssien tunnistetiedot löytyvät välilehdeltä ”Opiskeluoikeuden ulkop.”.")),
    "eriVuonnaKorotettujaKursseja" -> Column("Suoritetut tai rahoituksen piirissä oleviksi merkityt tunnustetut kurssit - korotettu eri vuonna")
  )
}

case class LukioKurssikertymaAineopiskelijaRow(
  oppilaitosOid: String,
  oppilaitos: String,
  kurssejaYhteensa: Int,
  suoritettujaKursseja: Int,
  tunnustettujaKursseja: Int,
  tunnustettujaKursseja_rahoituksenPiirissa: Int,
  pakollisia_tai_valtakunnallisiaSyventavia: Int,
  pakollisiaKursseja: Int,
  valtakunnallisestiSyventaviaKursseja: Int,
  suoritettujaPakollisia_ja_suoritettujaValtakunnallisiaSyventavia: Int,
  suoritettujaPakollisiaKursseja: Int,
  suoritettujaValtakunnallisiaSyventaviaKursseja: Int,
  tunnustettujaPakollisia_ja_tunnustettujaValtakunnallisiaSyventavia: Int,
  tunnustettujaPakollisiaKursseja: Int,
  tunnustettujaValtakunnallisiaSyventaviaKursseja: Int,
  tunnustettujaRahoituksenPiirissa_pakollisia_ja_valtakunnallisiaSyventavia: Int,
  tunnustettuja_rahoituksenPiirissa_pakollisia: Int,
  tunnustettuja_rahoituksenPiirissa_valtakunnallisiaSyventaiva: Int,
  suoritetutTaiRahoitetut_muutaKauttaRahoitetut: Int,
  suoritetutTaiRahoitetut_rahoitusmuotoEiTiedossa: Int,
  suoritetutTaiRahoitetut_eiOpiskeluoikeudenSisalla: Int,
  eriVuonnaKorotettujaKursseja: Int
)
