describe('TOR', function() {
  var page = TorPage()
  var login = LoginPage()
  var authentication = Authentication()
  var opinnot = OpinnotPage()
  var addOppija = AddOppijaPage()

  var eero = 'esimerkki, eero 010101-123N'
  var markkanen = 'markkanen, eero '
  var eerola = 'eerola, jouni '
  var teija = 'tekijä, teija 150995-914X'

  describe('Login-sivu', function() {
    before(login.openPage)
    it('näytetään, kun käyttäjä ei ole kirjautunut sisään', function() {
      expect(login.isVisible()).to.equal(true)
    })
    describe('Väärällä käyttäjätunnuksella', function() {
      before(login.login('fail', 'fail'))
      before(wait.until(login.isLoginErrorVisible))
      it('näytetään virheilmoitus', function() {})
    })
    describe('Väärällä salasanalla', function() {
      before(login.openPage)
      before(login.login('kalle', 'fail'))
      before(wait.until(login.isLoginErrorVisible))
      it('näytetään virheilmoitus', function() {})
    })
    describe('Onnistuneen loginin jälkeen', function() {
      before(login.openPage)
      before(login.login('kalle', 'kalle'))
      before(wait.until(page.isVisible))
      it('siirrytään TOR-etusivulle', function() {
        expect(page.isVisible()).to.equal(true)
      })
      it('näytetään kirjautuneen käyttäjän nimi', function() {
        expect(page.getUserName()).to.equal('kalle käyttäjä')
      })
    })
  })

  describe('Oppijahaku', function() {
    before(authentication.login(), resetMocks, page.openPage)
    it('näytetään, kun käyttäjä on kirjautunut sisään', function() {
      expect(page.isVisible()).to.equal(true)
      expect(page.oppijaHaku.isNoResultsLabelShown()).to.equal(false)
    })
    describe('Hakutulos-lista', function() {
      it('on aluksi tyhjä', function() {
        expect(page.oppijaHaku.getSearchResults().length).to.equal(0)
      })
    })
    describe('Kun haku tuottaa yhden tuloksen', function() {
      before(page.oppijaHaku.search('esimerkki', 1))
      it('ensimmäinen tulos näytetään', function() {
        expect(page.oppijaHaku.getSearchResults()).to.deep.equal([eero])
        expect(page.oppijaHaku.isNoResultsLabelShown()).to.equal(false)
      })

      it('ensimmäinen tulos valitaan automaattisesti', wait.until(function() { return page.getSelectedOppija() == eero }))

      describe('Kun haku tuottaa uudestaan yhden tuloksen', function() {
        before(page.oppijaHaku.search('teija', 1))
        it('tulosta ei valita automaattisesti', function() {
          expect(page.getSelectedOppija()).to.equal(eero)
        })
      })
    })
    describe('Haun tyhjentäminen', function() {
      before(page.oppijaHaku.search('esimerkki', 1))
      before(page.oppijaHaku.search('', 0))

      it('säilyttää oppijavalinnan', function() {
        expect(page.getSelectedOppija()).to.equal(eero)
      })

      it('tyhjentää hakutulos-listauksen', function() {
        expect(page.oppijaHaku.getSearchResults().length).to.equal(0)
        expect(page.oppijaHaku.isNoResultsLabelShown()).to.equal(false)
      })
    })
    describe('Kun haku tuottaa useamman tuloksen', function() {
      before(page.oppijaHaku.search('eero', 3))

      it('Hakutulokset näytetään', function() {
        expect(page.oppijaHaku.getSearchResults()).to.deep.equal([eero, eerola, markkanen])
      })

      describe('Kun klikataan oppijaa listalla', function() {
        before(page.oppijaHaku.selectOppija('markkanen'))

        it('Oppija valitaan', function() {
          expect(page.getSelectedOppija()).to.equal(markkanen)
        })
      })
    })

    describe('Kun haku ei tuota tuloksia', function() {
      before(page.oppijaHaku.search('asdf', page.oppijaHaku.isNoResultsLabelShown))

      it('Näytetään kuvaava teksti', function() {
        expect(page.oppijaHaku.isNoResultsLabelShown()).to.equal(true)
      })
    })

    describe('Kun haetaan olemassa olevaa henkilöä, jolla ei ole opinto-oikeuksia', function() {
      before(page.oppijaHaku.search('Presidentti', page.oppijaHaku.isNoResultsLabelShown))

      it('Tuloksia ei näytetä', function() {

      })
    })

    describe('Hakutavat', function() {
      it ('Hetulla, case-insensitive', function() {
        return page.oppijaHaku.search('010101-123n', [eero])()
      })
      it ('Nimen osalla, case-insensitive', function() {
        return page.oppijaHaku.search('JoU', [eerola])()
      })
      it ('Oidilla', function() {
        return page.oppijaHaku.search('1.2.246.562.24.00000000003', [markkanen])()
      })
    })
  })

  function prepareForNewOppija(username, searchString) {
    return function() {
      return resetMocks()
        .then(authentication.login(username))
        .then(page.openPage)
        .then(page.oppijaHaku.search(searchString, page.oppijaHaku.isNoResultsLabelShown))
        .then(page.oppijaHaku.addNewOppija)
    }
  }

  function addNewOppija(username, searchString, oppijaData) {
    return function() {
      return prepareForNewOppija(username, searchString)()
        .then(addOppija.enterValidData(oppijaData))
        .then(addOppija.submitAndExpectSuccess(oppijaData.hetu))
    }
  }

  describe('Opinto-oikeuden lisääminen', function() {
    describe('Olemassa olevalle henkilölle', function() {

      describe('Kun lisätään uusi opinto-oikeus', function() {
        before(addNewOppija('kalle', 'Tunkkila', { etunimet: 'Tero Terde', kutsumanimi: 'Terde', sukunimi: 'Tunkkila', hetu: '091095-9833', oppilaitos: 'Helsingin', tutkinto: 'auto'}))

        it('Onnistuu, näyttää henkilöpalvelussa olevat nimitiedot', function() {
          expect(page.getSelectedOppija()).to.equal('tunkkila-fagerlund, tero petteri gustaf 091095-9833')
        })
      })

      describe('Kun lisätään opinto-oikeus, joka henkilöllä on jo olemassa', function() {
        before(addNewOppija('kalle', 'kalle', { etunimet: 'Eero Adolf', kutsumanimi: 'Eero', sukunimi: 'Esimerkki', hetu: '010101-123N', oppilaitos: 'Helsingin', tutkinto: 'auto'}))

        it('Näytetään olemassa oleva tutkinto', function() {
          expect(page.getSelectedOppija()).to.equal(eero)
          expect(opinnot.getTutkinto()).to.equal('Autoalan perustutkinto')
          expect(opinnot.getOppilaitos()).to.equal('Helsingin Ammattioppilaitos')
        })
      })
    })

    describe('Uudelle henkilölle', function() {
      before(prepareForNewOppija('kalle', 'kalle'))

      describe('Aluksi', function() {
        it('Lisää-nappi on disabloitu', function() {
          expect(addOppija.isEnabled()).to.equal(false)
        })
        it('Tutkinto-kenttä on disabloitu', function() {
          expect(addOppija.tutkintoIsEnabled()).to.equal(false)
        })
      })

      describe('Kun syötetään validit tiedot', function() {
        before(addOppija.enterValidData())

        describe('Käyttöliittymän tila', function() {
          it('Lisää-nappi on enabloitu', function() {
            expect(addOppija.isEnabled()).to.equal(true)
          })
        })

        describe('Kun painetaan Lisää-nappia', function() {
          before(addOppija.submitAndExpectSuccess('Oppija, Ossi Olavi 300994-9694'))

          it('lisätty oppija näytetään', function() {})

          it('Lisätty opiskeluoikeus näytetään', function() {
            expect(opinnot.getTutkinto()).to.equal('Autoalan perustutkinto')
            expect(opinnot.getOppilaitos()).to.equal('Helsingin Ammattioppilaitos')
          })
        })
      })

      describe('Kun sessio on vanhentunut', function() {
        before( openPage('/tor/uusioppija', function() {return addOppija.isVisible()}),
          addOppija.enterValidData(),
          authentication.logout,
          addOppija.submit)

        it('Siirrytään login-sivulle', wait.until(login.isVisible))
      })

      describe('Kun hetu on virheellinen', function() {
        before(
          authentication.login(),
          openPage('/tor/uusioppija'),
          wait.until(function() {return addOppija.isVisible()}),
          addOppija.enterValidData({hetu: '123456-1234'})
        )
        it('Lisää-nappi on disabloitu', function() {
          expect(addOppija.isEnabled()).to.equal(false)
        })
      })
      describe('Kun hetu sisältää väärän tarkistusmerkin', function() {
        before(
          addOppija.enterValidData({hetu: '011095-953Z'})
        )
        it('Lisää-nappi on disabloitu', function() {
          expect(addOppija.isEnabled()).to.equal(false)
        })
      })
      describe('Kun hetu sisältää väärän päivämäärän, mutta on muuten validi', function() {
        before(
          addOppija.enterValidData({hetu: '300275-5557'})
        )
        it('Lisää-nappi on disabloitu', function() {
          expect(addOppija.isEnabled()).to.equal(false)
        })
      })
      describe('Kun kutsumanimi ei löydy etunimistä', function() {
        before(
          addOppija.enterValidData({kutsumanimi: 'eiloydy'})
        )
        it('Lisää-nappi on disabloitu', function() {
          expect(addOppija.isEnabled()).to.equal(false)
        })
        it('Näytetään virheilmoitus', function() {
          expect(addOppija.isErrorShown('kutsumanimi')()).to.equal(true)
        })
      })
      describe('Kun kutsumanimi löytyy väliviivallisesta nimestä', function() {
        before(
          addOppija.enterValidData({etunimet: 'Juha-Pekka', kutsumanimi: 'Pekka'})
        )
        it('Lisää-nappi on enabloitu', function() {
          expect(addOppija.isEnabled()).to.equal(true)
        })
      })
      describe('Kun oppilaitos on valittu', function() {
        before(addOppija.enterValidData())
        it('voidaan valita tutkinto', function(){
          expect(addOppija.tutkintoIsEnabled()).to.equal(true)
          expect(addOppija.isEnabled()).to.equal(true)
        })
        describe('Kun oppilaitos-valinta muutetaan', function() {
          before(addOppija.selectOppilaitos('Omnia'))
          it('tutkinto pitää valita uudestaan', function() {
            expect(addOppija.isEnabled()).to.equal(false)
          })
          describe('Tutkinnon valinnan jälkeen', function() {
            before(addOppija.selectTutkinto('auto'))
            it('Lisää nappi on enabloitu', function() {
              expect(addOppija.isEnabled()).to.equal(true)
            })
          })
        })
      })
      describe('Oppilaitosvalinta', function() {
        describe('Näytetään vain käyttäjän organisaatiopuuhun kuuluvat oppilaitokset', function() {
          it('1', function() {
            return prepareForNewOppija('hiiri', 'Tunkkila')()
              .then(addOppija.enterOppilaitos('Helsinki'))
              .then(wait.forMilliseconds(500))
              .then(function() {
                expect(addOppija.oppilaitokset()).to.deep.equal(['Omnia Helsinki'])
              })
          })
          it('2', function() {
            return prepareForNewOppija('kalle', 'Tunkkila')()
              .then(addOppija.enterOppilaitos('Helsinki'))
              .then(wait.forMilliseconds(500))
              .then(function() {
                expect(addOppija.oppilaitokset()).to.deep.equal(['Metropolia Helsinki', 'Omnia Helsinki'])
              })
          })
        })
        describe('Kun oppilaitos on virheellinen', function() {
          before(addOppija.enterValidData(), addOppija.enterOppilaitos('virheellinen'))
          it('Lisää-nappi on disabloitu', function() {
            expect(addOppija.isEnabled()).to.equal(false)
          })
          it('Tutkinnon valinta on estetty', function() {
            expect(addOppija.tutkintoIsEnabled()).to.equal(false)
          })
        })
      })
      describe('Kun tutkinto on virheellinen', function() {
        before(addOppija.enterValidData(), addOppija.enterTutkinto('virheellinen'))
        it('Lisää-nappi on disabloitu', function() {
          expect(addOppija.isEnabled()).to.equal(false)
        })
      })
    })

    describe('Tietojen validointi serverillä', function() {
      before(resetMocks, authentication.login('kalle'), page.openPage)

      describe('Valideilla tiedoilla', function() {
        it('palautetaan HTTP 200', verifyResponseCode(addOppija.putOpiskeluOikeusAjax({}), 200))
      })

      describe('Kun opinto-oikeutta yritetään lisätä oppilaitokseen, johon käyttäjällä ei ole pääsyä', function() {
        it('palautetaan HTTP 403 virhe', verifyResponseCode(addOppija.putOpiskeluOikeusAjax(
          { oppilaitos:{oid: '1.2.246.562.10.346830761110'}}
        ), 403, "Ei oikeuksia organisatioon 1.2.246.562.10.346830761110"))
      })

      describe('Kun opinto-oikeutta yritetään lisätä oppilaitokseen, jota ei löydy organisaatiopalvelusta', function() {
        it('palautetaan HTTP 400 virhe', verifyResponseCode(addOppija.putOpiskeluOikeusAjax(
          { oppilaitos:{oid: 'tuuba'}}
        ), 400, "Organisaatiota tuuba ei löydy organisaatiopalvelusta"))
      })

      describe('Nimenä tyhjä merkkijono', function() {
        it('palautetaan HTTP 400 virhe', verifyResponseCode(addOppija.putOppijaAjax({
          henkilö: {
            'sukunimi':''
          }
        }), 400))
      })

      describe('Nimenä tyhjä merkkijono', function() {
        it('palautetaan HTTP 400 virhe', verifyResponseCode(addOppija.putOppijaAjax({
          henkilö: {
            'sukunimi':''
          }
        }), 400))
      })

      describe('Epäkelpo JSON-dokumentti', function() {
        it('palautetaan HTTP 400 virhe', verifyResponseCode(function() {
          return sendAjax('/tor/api/oppija', 'application/json', 'not json', 'put')
        }, 400, "Invalid JSON"))
      })

      describe('Kun yritetään lisätä opinto-oikeus virheelliseen perusteeseen', function() {
        it('palautetaan HTTP 400 virhe', verifyResponseCode(addOppija.putOpiskeluOikeusAjax({
          suoritus: {
            koulutusmoduulitoteutus: {
              koulutusmoduuli: {
                perusteenDiaarinumero: '39/xxx/2014'
              }
            }
          }
        }), 400, "Tutkinnon peruste on virheellinen: 39/xxx/2014"))
      })

      describe('Kun yritetään lisätä opinto-oikeus ilman perustetta', function() {
        it('palautetaan HTTP 400 virhe', verifyResponseCode(addOppija.putOpiskeluOikeusAjax({
          suoritus: {
            koulutusmoduulitoteutus: {
              koulutusmoduuli: {
                perusteenDiaarinumero: null
              }
            }
          }
        }), 400, "perusteenDiaarinumero"))
      })

      describe('Hetun ollessa', function() {
        describe('muodoltaan virheellinen', function() {
          it('palautetaan HTTP 400 virhe', verifyResponseCode(addOppija.putOppijaAjax({henkilö: {hetu: '010101-123123'}}), 400, 'Virheellinen muoto hetulla: 010101-123123'))
        })
        describe('muodoltaan oikea, mutta väärä tarkistusmerkki', function() {
          it('palautetaan HTTP 400 virhe', verifyResponseCode(addOppija.putOppijaAjax({henkilö: {hetu: '010101-123P'}}), 400, 'Virheellinen tarkistusmerkki hetussa: 010101-123P'))
        })
        describe('päivämäärältään tulevaisuudessa', function() {
          it('palautetaan HTTP 400 virhe', verifyResponseCode(addOppija.putOppijaAjax({henkilö: {hetu: '141299A903C'}}), 400, 'Syntymäpäivä hetussa: 141299A903C on tulevaisuudessa'))
        })
        describe('päivämäärältään virheellinen', function() {
          it('palautetaan HTTP 400 virhe', verifyResponseCode(addOppija.putOppijaAjax({henkilö: {hetu: '300215-123T'}}), 400, 'Virheellinen syntymäpäivä hetulla: 300215-123T'))
        })
        describe('validi', function() {
          it('palautetaan HTTP 200', verifyResponseCode(addOppija.putOppijaAjax({henkilö: {hetu: '010101-123N'}}), 200))
        })
      })
    })

    describe('Virhetilanteet', function() {
      describe('Kun tallennus epäonnistuu', function() {
        before( openPage('/tor/uusioppija', function() {return addOppija.isVisible()}),
          addOppija.enterValidData({sukunimi: "error"}),
          addOppija.submit)

        it('Näytetään virheilmoitus', wait.until(page.isErrorShown))
      })
    })
  })


  describe('Tutkinnon rakenne', function() {
    describe("Ammatillinen perustutkinto", function() {
      before(addNewOppija('kalle', 'Tunkkila', { hetu: '091095-9833'}))

      it('Osaamisala- ja suoritustapavalinnat näytetään', function() {
        expect(opinnot.isSuoritustapaSelectable()).to.equal(true)
        expect(opinnot.isOsaamisalaSelectable()).to.equal(true)
      })
      describe('Kun valitaan osaamisala ja suoritustapa', function() {
        before(opinnot.selectSuoritustapa("ops"), opinnot.selectOsaamisala("1527"))

        it('Näytetään tutkinnon rakenne', function() {
          expect(opinnot.getTutkinnonOsat()[0]).to.equal('Myynti ja tuotetuntemus')
        })
      })
    })

    describe('Erikoisammattitutkinto', function() {
      before(addNewOppija('kalle', 'Tunkkila', { etunimet: 'Tero Terde', kutsumanimi: 'Terde', sukunimi: 'Tunkkila', hetu: '091095-9833', oppilaitos: 'Helsingin', tutkinto: 'erikois'}))

      it('Ei näytetä osaamisala- ja suoritustapavalintoja (koska mitään valittavaa ei ole)', function() {
        expect(opinnot.isSuoritustapaSelectable()).to.equal(false)
        expect(opinnot.isOsaamisalaSelectable()).to.equal(false)
      })

      it('Näytetään tutkinnon rakenne', function() {
        expect(opinnot.getTutkinnonOsat()[0]).to.equal('Johtaminen ja henkilöstön kehittäminen')
      })
    })
  })

  describe('Tutkinnon tietojen muuttaminen', function() {
    before(authentication.login(), resetMocks, page.openPage, addNewOppija('kalle', 'Tunkkila', { hetu: '091095-9833'}))
    it('Aluksi ei näytetä \"Kaikki tiedot tallennettu\" -tekstiä', function() {
      expect(page.isSavedLabelShown()).to.equal(false)
    })

    describe('Kun valitaan osaamisala ja suoritustapa', function() {
      before(opinnot.selectSuoritustapa("ops"), opinnot.selectOsaamisala("1527"))

      describe('Muutosten näyttäminen', function() {
        before(wait.until(page.isSavedLabelShown))
        it('Näytetään "Kaikki tiedot tallennettu" -teksti', function() {
          expect(page.isSavedLabelShown()).to.equal(true)
        })
      })

      describe('Kun sivu ladataan uudelleen', function() {
        before( page.oppijaHaku.search('ero', 4),
                page.oppijaHaku.selectOppija('tunkkila'), opinnot.waitUntilTutkintoVisible())

        it('Muuttuneet tiedot on tallennettu', function() {
          expect(opinnot.getTutkinnonOsat()[0]).to.equal('Myynti ja tuotetuntemus')
        })
      })


      describe('Tietojen validointi serverillä', function() {
        describe('Osaamisala ja suoritustapa ok', function() {
          it('palautetaan HTTP 200', verifyResponseCode(addOppija.putOpiskeluOikeusAjax({"suoritus": {"koulutusmoduulitoteutus": {
              "suoritustapa": {"tunniste": {"koodiarvo": "ops", "koodistoUri": "suoritustapa"}},
              "osaamisala": [{"koodiarvo": "1527", "koodistoUri": "osaamisala"}]
            }}}
          ), 200))
        })
        describe('Suoritustapa virheellinen', function() {
          it('palautetaan HTTP 400', verifyResponseCode(addOppija.putOpiskeluOikeusAjax({"suoritus": {"koulutusmoduulitoteutus": {
              "suoritustapa": {"tunniste": {"koodiarvo": "blahblahtest", "koodistoUri": "suoritustapa"}},
              "osaamisala": [{"koodiarvo": "1527", "koodistoUri": "osaamisala"}]
            }}}
          ), 400, "Koodia suoritustapa/blahblahtest ei löydy koodistosta"))
        })
        describe('Osaamisala ei löydy tutkintorakenteesta', function() {
          it('palautetaan HTTP 400', verifyResponseCode(addOppija.putOpiskeluOikeusAjax({"suoritus": {"koulutusmoduulitoteutus": {
            "suoritustapa": {"tunniste": {"koodiarvo": "ops", "koodistoUri": "suoritustapa"}},
            "osaamisala": [{"koodiarvo": "3053", "koodistoUri": "osaamisala"}]
          }}}), 400, "Osaamisala 3053 ei löydy tutkintorakenteesta perusteelle 39/011/2014"))
        })
        describe('Osaamisala virheellinen', function() {
          it('palautetaan HTTP 400', verifyResponseCode(addOppija.putOpiskeluOikeusAjax({"suoritus": {"koulutusmoduulitoteutus": {
            "suoritustapa": {"tunniste": {"koodiarvo": "ops", "koodistoUri": "suoritustapa"}},
            "osaamisala": [{"koodiarvo": "0", "koodistoUri": "osaamisala"}]
          }}}), 400, "Koodia osaamisala/0 ei löydy koodistosta"))
        })
      })
    })

    describe('Kun annetaan arviointi tutkinnonosalle', function() {
      describe('Arvion antaminen käyttöliittymän kautta', function() {
        describe('OPS-muotoinen, asteikko T1-K3', function() {
          before(opinnot.selectSuoritustapa("ops"), opinnot.selectOsaamisala("1527"))
          var tutkinnonOsa = opinnot.getTutkinnonOsa("Markkinointi ja asiakaspalvelu")
          before(tutkinnonOsa.addArviointi("H2"))
          it('Uusi arviointi näytetään', function() {
            expect(tutkinnonOsa.getArvosana()).to.equal("H2")
          })

          describe('Kun sivu ladataan uudelleen', function() {
            before( page.oppijaHaku.search('ero', 4),
              page.oppijaHaku.selectOppija('tunkkila'), opinnot.waitUntilTutkintoVisible())

            it('Muuttuneet tiedot on tallennettu', function() {
              expect(tutkinnonOsa.getArvosana()).to.equal("H2")
            })
          })
        })
        describe('Näyttömuotoinen, asteikko HYVÄKSYTTY/HYLÄTTY', function() {
          before(opinnot.selectSuoritustapa("naytto"), opinnot.selectOsaamisala("1527"))
          var tutkinnonOsa = opinnot.getTutkinnonOsa("Myynti ja tuotetuntemus")
          before(tutkinnonOsa.addArviointi("Hylätty"))
          it('Uusi arviointi näytetään', function() {
            expect(tutkinnonOsa.getArvosana()).to.equal("Hylätty")
          })

          describe('Kun sivu ladataan uudelleen', function() {
            before( page.oppijaHaku.search('ero', 4),
              page.oppijaHaku.selectOppija('tunkkila'), opinnot.waitUntilTutkintoVisible())

            it('Muuttuneet tiedot on tallennettu', function() {
              expect(tutkinnonOsa.getArvosana()).to.equal("Hylätty")
            })
          })

        })
      })

      describe('Tietojen validointi serverillä', function() {
        describe('Tutkinnon osa ja arviointi ok', function() {
          it('palautetaan HTTP 200', verifyResponseCode(addOppija.putTutkinnonOsaSuoritusAjax({}), 200))
        })
        describe('Tutkinnon osa ei kuulu tutkintorakenteeseen', function() {
          it('palautetaan HTTP 400', verifyResponseCode(addOppija.putTutkinnonOsaSuoritusAjax({
            "koulutusmoduulitoteutus": {
              "koulutusmoduuli": {
                "tunniste": {"koodiarvo": "103135", "nimi": "Kaapelitelevisio- ja antennijärjestelmät", "koodistoUri": "tutkinnonosat", "koodistoVersio": 1}
              }
            }
          }), 400, "Tutkinnon osa tutkinnonosat/103135 ei löydy tutkintorakenteesta perusteelle 39/011/2014 - suoritustapa naytto"))
        })
        describe('Tutkinnon osaa ei ei löydy koodistosta', function() {
          it('palautetaan HTTP 400', verifyResponseCode(addOppija.putTutkinnonOsaSuoritusAjax({
            "koulutusmoduulitoteutus": {
              "koulutusmoduuli": {
                "tunniste": {"koodiarvo": "9923123", "nimi": "Väärää tietoa", "koodistoUri": "tutkinnonosat", "koodistoVersio": 1}
              }
            }
          }), 400, "Koodia tutkinnonosat/9923123 ei löydy koodistosta"))
        })
        describe('Arviointiasteikko on tuntematon', function() {
          it('palautetaan HTTP 400', verifyResponseCode(addOppija.putTutkinnonOsaSuoritusAjax(
            {
              arviointi: [{arvosana: {koodiarvo: "2", koodistoUri: "vääräasteikko"}}]
            }
          ), 400, "not found in enum"))
        })
        describe('Arvosana ei kuulu perusteiden mukaiseen arviointiasteikkoon', function() {
          it('palautetaan HTTP 400', verifyResponseCode(addOppija.putTutkinnonOsaSuoritusAjax(
            {
              arviointi: [{arvosana: {koodiarvo: "x", koodistoUri: "arviointiasteikkoammatillinent1k3"}}]
            }
          ), 400, "Koodia arviointiasteikkoammatillinent1k3/x ei löydy koodistosta"))
        })
      })
    })

    describe('Virhetilanteet', function() {
      describe('Kun tallennus epäonnistuu', function() {
        before(
          mockHttp("/tor/api/oppija", { status: 500 }),
          opinnot.selectOsaamisala("1622"),
          wait.until(page.isErrorShown)
        )

        it('Näytetään virheilmoitus', function() {

        })
      })
    })
  })

  describe('Navigointi suoraan oppijan sivulle', function() {
    before(
      authentication.login(),
      openPage('/tor/oppija/1.2.246.562.24.00000000001', page.isOppijaSelected('eero')),
      opinnot.waitUntilTutkintoVisible()
    )

    it('Oppijan tiedot näytetään', function() {
      expect(page.getSelectedOppija()).to.equal(eero)
    })

    it('Oppijan tutkinto ja oppilaitos näytetään', function() {
      expect(opinnot.getTutkinto()).to.equal('Autoalan perustutkinto')
      expect(opinnot.getOppilaitos()).to.equal('Helsingin Ammattioppilaitos')
    })

    it('Hakutulos näytetään', function() {
      expect(page.oppijaHaku.getSearchResults()).to.deep.equal([eero])
    })
  })

  describe('Virhetilanteet', function() {
    before(
      authentication.login(),
      resetMocks
    )

    describe('Odottamattoman virheen sattuessa', function() {
      before(
        page.openPage,
        page.oppijaHaku.search('error', page.isErrorShown))

      it('näytetään virheilmoitus', function() {})
    })

    describe('Kun palvelimeen ei saada yhteyttä', function() {
      before(
        page.openPage,
        mockHttp('/tor/api/oppija?query=blah', {}),
        page.oppijaHaku.search('blah', page.isErrorShown))

      it('näytetään virheilmoitus', function() {})
    })


    describe('Kun sivua ei löydy', function() {
      before(authentication.login(), openPage('/tor/asdf', page.is404))

      it('näytetään 404-sivu', function() {})
    })
  })

  describe('Käyttöoikeudet', function() {
    describe('Oppijahaku', function() {
      before(authentication.login('hiiri'), page.openPage, page.oppijaHaku.search('eero', [markkanen]))

      it('Näytetään vain ne oppijat, joiden opinto-oikeuksiin liittyviin organisaatioihin on käyttöoikeudet', function() {

      })
    })

    describe('Navigointi oppijan sivulle', function() {
      before(authentication.login('hiiri'), openPage('/tor/oppija/1.2.246.562.24.00000000002', page.is404))

      it('Estetään jos oppijalla ei opinto-oikeutta, joihin käyttäjällä on katseluoikeudet', function() {

      })
    })
  })

  describe('Tietoturva', function() {
    before(login.openPage)

    describe('Oppijarajapinta', function() {
      before(openPage('/tor/api/oppija?query=eero', authenticationErrorIsShown))

      it('vaatii autentikaation', function () {
        expect(authenticationErrorIsShown()).to.equal(true)
      })
    })


    describe('Kun klikataan logout-linkkiä', function() {
      before(authentication.login(), page.openPage, page.logout)

      it('Siirrytään login-sivulle', function() {
        expect(login.isVisible()).to.equal(true)
      })

      describe('Kun ladataan sivu uudelleen', function() {
        before(openPage('/tor', login.isVisible))

        it('Sessio on päättynyt ja login-sivu näytetään', function() {
          expect(login.isVisible()).to.equal(true)
        })
      })

      describe('Kun kirjaudutaan uudelleen sisään', function() {
        before(authentication.login(), page.openPage, page.oppijaHaku.search('jouni', [eerola]), page.logout, login.login('kalle', 'kalle'), wait.until(page.isReady))
        it ('Käyttöliittymä on palautunut alkutilaan', function() {
          expect(page.oppijaHaku.getSearchResults()).to.deep.equal([])
          expect(page.getSelectedOppija()).to.equal('')
        })
      })
    })

    describe('Session vanhennuttua', function() {
      before(authentication.login(), page.openPage, authentication.logout, page.oppijaHaku.search('eero', login.isVisible))

      it('Siirrytään login-sivulle', function() {
        expect(login.isVisible()).to.equal(true)
      })
    })
  })

  function authenticationErrorIsShown() {
    return S('body').text() === 'Not authenticated'
  }

  function resetMocks() {
    return Q($.ajax({ url: '/tor/fixtures/reset', method: 'post'}))
  }

  function mockHttp(url, result) {
    return function() { testFrame().http.mock(url, result) }
  }

  function verifyResponseCode(fn, code, text) {
    if (code == 200) {
      return fn
    } else {
      return function() {
        return fn().then(function() { throw { status: 200} }).catch(function(error) {
          expect(error.status).to.equal(code)
          if (text) {
            var errorObject = JSON.parse(error.responseText);
            if (errorObject.errors) {
              // Json schema validation error
              expect(error.responseText).to.contain(text)
            } else {
              expect(errorObject[0]).to.equal(text)
            }
          }
        })
      }
    }
  }
})