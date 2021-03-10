import {
  contentEventuallyEquals,
  goToLocation,
  loginAs,
  resetMockData,
  textEventuallyEquals,
} from "../integrationtests-env/browser"
import {
  allowNetworkError,
  FORBIDDEN,
} from "../integrationtests-env/fail-on-console"

const mainHeadingEquals = (expected: string) =>
  textEventuallyEquals("h1.heading--primary", expected)
const secondaryHeadingEquals = (expected: string) =>
  textEventuallyEquals("h2.heading--secondary", expected)
const oppivelvollisuustiedotEquals = (expected: string) =>
  contentEventuallyEquals("#oppivelvollisuustiedot .card__body", expected)
const opiskeluhistoriaEquals = (expected: string) =>
  contentEventuallyEquals("#opiskeluhistoria .card__body", expected)

describe("Oppijakohtainen näkymä", () => {
  it("Näyttää oppijan tiedot, johon käyttäjällä on lukuoikeus", async () => {
    await loginAs(
      "/virkailija/oppijat/1.2.246.562.24.00000000001",
      "valpas-jkl-normaali",
      "valpas-jkl-normaali"
    )
    await mainHeadingEquals(
      "Oppivelvollinen-ysiluokka-kesken-keväällä-2021 Valpas (221105A3023)"
    )
    await secondaryHeadingEquals("Oppija 1.2.246.562.24.00000000001")
    await oppivelvollisuustiedotEquals(`
      Opiskelutilanne:	Opiskelemassa
      Oppivelvollisuus:	22.11.2023 asti
    `)
    await opiskeluhistoriaEquals(`
      school
      Perusopetus 2012 –
      Jyväskylän normaalikoulu
      Ryhmä: 9C
      Tila: Läsnä
    `)
  })

  it("Näyttää oppijan tiedot valmistuneelle ysiluokkalaiselle", async () => {
    await loginAs(
      "/virkailija/oppijat/1.2.246.562.24.00000000011",
      "valpas-jkl-normaali",
      "valpas-jkl-normaali"
    )
    await mainHeadingEquals(
      "Ysiluokka-valmis-keväällä-2021 Valpas (190605A006K)"
    )
    await secondaryHeadingEquals("Oppija 1.2.246.562.24.00000000011")
    await oppivelvollisuustiedotEquals(`
      Opiskelutilanne:	Ei opiskelupaikkaa
      Oppivelvollisuus:	19.6.2023 asti
    `)
    await opiskeluhistoriaEquals(`
      school
      Perusopetus 2012 – 2021
      Jyväskylän normaalikoulu
      Ryhmä: 9C
      Tila: Valmistunut
    `)
  })

  it("Ei näytä oppijan tietoja, johon käyttäjällä ei ole lukuoikeutta", async () => {
    allowNetworkError("/valpas/api/oppija/", FORBIDDEN)
    await loginAs(
      "/virkailija/oppijat/1.2.246.562.24.00000000001",
      "valpas-helsinki",
      "valpas-helsinki"
    )
    await mainHeadingEquals("Oppijan tiedot")
    await secondaryHeadingEquals(
      "Oppijaa ei löydy tunnuksella 1.2.246.562.24.00000000001"
    )
  })

  it("Ei näytä oppijan tietoja, johon käyttäjällä ei ole lukuoikeutta vaihdetun tarkastelupäivän jälkeen", async () => {
    const path = "/virkailija/oppijat/1.2.246.562.24.00000000011"

    allowNetworkError("/valpas/api/oppija/", FORBIDDEN)
    await loginAs(path, "valpas-jkl-normaali", "valpas-jkl-normaali")
    await mainHeadingEquals(
      "Ysiluokka-valmis-keväällä-2021 Valpas (190605A006K)"
    )
    await resetMockData("2021-10-05")
    await goToLocation(path)

    await mainHeadingEquals("Oppijan tiedot")
    await secondaryHeadingEquals(
      "Oppijaa ei löydy tunnuksella 1.2.246.562.24.00000000011"
    )
  })

  it("Näyttää oppijalta, jolla on useampia päällekäisiä opiskeluoikeuksia kaikki opiskeluoikeudet", async () => {
    await loginAs(
      "/virkailija/oppijat/1.2.246.562.24.00000000003",
      "valpas-jkl-normaali",
      "valpas-jkl-normaali"
    )
    await mainHeadingEquals("Päällekkäisiä Oppivelvollisuuksia (060605A083N)")
    await secondaryHeadingEquals("Oppija 1.2.246.562.24.00000000003")
    await oppivelvollisuustiedotEquals(`
      Opiskelutilanne:	Opiskelemassa
      Oppivelvollisuus:	6.6.2023 asti
    `)
    await opiskeluhistoriaEquals(`
      school
      Perusopetus 2012 –
      Jyväskylän normaalikoulu
      Ryhmä: 9B
      Tila: Läsnä
      school
      Perusopetus 2012 –
      Kulosaaren ala-aste
      Ryhmä: 8A
      Tila: Läsnä
    `)
  })

  it("Näyttää oppijalta, jolla on useampia peräkkäisiä opiskeluoikeuksia kaikki opiskeluoikeudet", async () => {
    await loginAs(
      "/virkailija/oppijat/1.2.246.562.24.00000000015",
      "valpas-jkl-normaali",
      "valpas-jkl-normaali"
    )
    await mainHeadingEquals("LukionAloittanut Valpas (290405A871A)")
    await secondaryHeadingEquals("Oppija 1.2.246.562.24.00000000015")
    await oppivelvollisuustiedotEquals(`
      Opiskelutilanne:	Opiskelemassa
      Oppivelvollisuus:	29.4.2023 asti
    `)
    await opiskeluhistoriaEquals(`
      school
      Lukiokoulutus 2021 –
      Jyväskylän normaalikoulu
      Tila: Läsnä
      school
      Perusopetus 2012 – 2021
      Jyväskylän normaalikoulu
      Ryhmä: 9C
      Tila: Valmistunut
    `)
  })
})
