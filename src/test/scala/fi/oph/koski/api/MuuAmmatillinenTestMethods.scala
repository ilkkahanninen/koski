package fi.oph.koski.api

import java.time.LocalDate

import fi.oph.koski.documentation.ExampleData.{longTimeAgo, opiskeluoikeusLäsnä, valtionosuusRahoitteinen}
import fi.oph.koski.json.JsonSerializer
import fi.oph.koski.organisaatio.MockOrganisaatiot
import fi.oph.koski.schema.{AmmatillinenPäätasonSuoritus, _}

import scala.reflect.runtime.universe.TypeTag

trait MuuAmmatillinenTestMethods[T <: AmmatillinenPäätasonSuoritus] extends PutOpiskeluoikeusTestMethods[AmmatillinenOpiskeluoikeus] {
  override def tag: TypeTag[AmmatillinenOpiskeluoikeus] = implicitly[TypeTag[AmmatillinenOpiskeluoikeus]]
  override def defaultOpiskeluoikeus: AmmatillinenOpiskeluoikeus = makeOpiskeluoikeus(alkamispäivä = longTimeAgo)

  protected def putTutkintoSuoritus[A](suoritus: T, henkilö: Henkilö = defaultHenkilö, headers: Headers = authHeaders() ++ jsonContent)(f: => A): A = {
    putAmmatillinenPäätasonSuoritus(suoritus, henkilö, headers)(f)
  }

  protected def putAmmatillinenPäätasonSuoritus[A](suoritus: AmmatillinenPäätasonSuoritus, henkilö: Henkilö = defaultHenkilö, headers: Headers = authHeaders() ++ jsonContent)(f: => A): A = {
    val opiskeluoikeus = defaultOpiskeluoikeus.copy(suoritukset = List(suoritus))
    putOppija(makeOppija(henkilö, List(JsonSerializer.serializeWithRoot(opiskeluoikeus))), headers)(f)
  }

  protected def makeOpiskeluoikeus(alkamispäivä: LocalDate = longTimeAgo) = AmmatillinenOpiskeluoikeus(
    tila = AmmatillinenOpiskeluoikeudenTila(List(AmmatillinenOpiskeluoikeusjakso(alkamispäivä, opiskeluoikeusLäsnä, Some(valtionosuusRahoitteinen)))),
    oppilaitos = Some(Oppilaitos(MockOrganisaatiot.stadinAmmattiopisto)),
    suoritukset = List(defaultPäätasonSuoritus)
  )

  protected def defaultPäätasonSuoritus: T
}
