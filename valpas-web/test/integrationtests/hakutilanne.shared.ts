import { createHakutilannePathWithoutOrg } from "../../src/state/paths"

export const jklNormaalikouluTableContent = `
  Epäonninen Valpas                                       | 30.10.2005  | 9C | –          | Ei hakemusta         | –                           | –                         | –                                                                          |
  Eroaja-myöhemmin Valpas                                 | 29.9.2005   | 9C | –          | Ei hakemusta         | –                           | –                         | –                                                                          |
  Kahdella-oppija-oidilla Valpas                          | 15.2.2005   | 9C | 30.5.2021  | Hakenut open_in_new  | Varasija: Ressun lukio      | –                         | doneJyväskylän normaalikoulu, Lukiokoulutus                                |
  KasiinAstiToisessaKoulussaOllut Valpas                  | 17.8.2005   | 9C | –          | Ei hakemusta         | –                           | –                         | –                                                                          |
  Kotiopetus-menneisyydessä Valpas                        | 6.2.2005    | 9C | –          | Ei hakemusta         | –                           | –                         | –                                                                          |
  LukionAloittanut Valpas                                 | 29.4.2005   | 9C | 30.5.2021  | Ei hakemusta         | –                           | –                         | doneJyväskylän normaalikoulu, Lukiokoulutus                                |
  LukionLokakuussaAloittanut Valpas                       | 18.4.2005   | 9C | 30.5.2021  | Ei hakemusta         | –                           | –                         | hourglass_empty3.10.2021 alkaen: Jyväskylän normaalikoulu, Lukiokoulutus   |
  LuokallejäänytYsiluokkalainen Valpas                    | 2.8.2005    | 9A | –          | 2 hakua              | –                           | –                         | –                                                                          |
  LuokallejäänytYsiluokkalainenJatkaa Valpas              | 6.2.2005    | 9B | –          | Ei hakemusta         | –                           | –                         | –                                                                          |
  LuokallejäänytYsiluokkalainenKouluvaihtoMuualta Valpas  | 2.11.2005   | 9B | –          | Ei hakemusta         | –                           | –                         | –                                                                          |
  Oppivelvollinen-ysiluokka-kesken-keväällä-2021 Valpas   | 22.11.2005  | 9C | –          | Hakenut open_in_new  | 2. Helsingin medialukio     | doneHelsingin medialukio  | –                                                                          |
  Päällekkäisiä Oppivelvollisuuksia                       | 6.6.2005    | 9B | –          | Hakenut open_in_new  | Hyväksytty (2 hakukohdetta) | doneOmnia                 | –                                                                          |
  Turvakielto Valpas                                      | 29.9.2004   | 9C | –          | Hakenut open_in_new  | warningEi opiskelupaikkaa   | –                         | –                                                                          |
  UseampiYsiluokkaSamassaKoulussa Valpas                  | 25.8.2005   | 9D | –          | Ei hakemusta         | –                           | –                         | –                                                                          |
  UseampiYsiluokkaSamassaKoulussa Valpas                  | 25.8.2005   | 9C | 30.5.2021  | Ei hakemusta         | –                           | –                         | –                                                                          |
  Ysiluokka-valmis-keväällä-2021 Valpas                   | 19.6.2005   | 9C | 30.5.2021  | Ei hakemusta         | –                           | –                         | –                                                                          |
`

export const hakutilannePath = createHakutilannePathWithoutOrg("/virkailija")
