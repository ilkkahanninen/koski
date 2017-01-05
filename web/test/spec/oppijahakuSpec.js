describe('Oppijahaku', function() {
  var page = KoskiPage()
  var opinnot = OpinnotPage()
  var eero = 'Esimerkki, Eero 010101-123N'
  var markkanen = 'Markkanen-Fagerström, Eéro Jorma-Petteri 080154-770R'
  var eerola = 'Eerola, Jouni 081165-793C'
  var teija = 'Tekijä, Teija 251019-039B'

  before(Authentication().login(), resetFixtures, page.openPage)

  it('näytetään, kun käyttäjä on kirjautunut sisään', function() {
    expect(page.isVisible()).to.equal(true)
    expect(page.oppijaHaku.isNoResultsLabelShown()).to.equal(false)
  })
  describe('Hakutulos-lista', function() {
    it('on aluksi tyhjä', function() {
      expect(page.oppijaHaku.getSearchResults().length).to.equal(0)
    })
  })
  describe('Kun haku tuottaa tuloksia', function() {
    before(page.oppijaHaku.search('eero', 2))

    it('Hakutulokset näytetään', function() {
      expect(page.oppijaHaku.getSearchResults()).to.deep.equal([eerola, eero]) // TODO: should find eeros with é, è and ë
    })

    describe('Kun klikataan oppijaa listalla', function() {
      before(page.oppijaHaku.selectOppija('Eerola'))

      it('Oppija valitaan', function() {
        expect(page.getSelectedOppija()).to.equal(eerola)
      })
    })
  })
  describe('Haun tyhjentäminen', function() {
    before(page.openPage, page.oppijaHaku.search('esimerkki', 1))
    before(page.oppijaHaku.search('', 0))

    /*
    it('säilyttää oppijavalinnan', function() {
      expect(page.getSelectedOppija()).to.equal(eero)
    })
    */

    it('tyhjentää hakutulos-listauksen', function() {
      expect(page.oppijaHaku.getSearchResults().length).to.equal(0)
      expect(page.oppijaHaku.isNoResultsLabelShown()).to.equal(false)
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

  describe('Navigointi suoraan oppijan sivulle', function() {
    before(
      Authentication().login(),
      resetFixtures,
      openPage('/koski/oppija/1.2.246.562.24.00000000001', page.isOppijaSelected('Eero'))
    )

    it('Oppijan tiedot näytetään', function() {
      expect(page.getSelectedOppija()).to.equal(eero)
    })

    it('Oppijan tutkinto ja oppilaitos näytetään', function() {
      expect(opinnot.getTutkinto()).to.equal('Autoalan perustutkinto')
      expect(opinnot.getOppilaitos()).to.equal('Stadin ammattiopisto')
    })

    /*
    it('Hakutulos näytetään', function() {
      expect(page.oppijaHaku.getSearchResults()).to.deep.equal([eero])
    })
    */
  })

  describe('Käyttöoikeudet', function() {
    describe('Oppijahaku', function() {
      before(Authentication().login('omnia-palvelukäyttäjä'), page.openPage, page.oppijaHaku.search('eéro', [markkanen]))

      it('Näytetään vain ne oppijat, joiden opinto-oikeuksiin liittyviin organisaatioihin on käyttöoikeudet', function() {

      })
    })

    describe('Navigointi oppijan sivulle', function() {
      before(Authentication().login('omnia-palvelukäyttäjä'), openPage('/koski/oppija/1.2.246.562.24.00000000002', page.is404))

      it('Estetään jos oppijalla ei opinto-oikeutta, joihin käyttäjällä on katseluoikeudet', function() {

      })
    })
  })
})