function OpinnotPage() {

  function oppija() { return findSingle('.oppija') }
  function opiskeluoikeus() { return findSingle('.opiskeluoikeus')}

  var api = {
    getTutkinto: function(index) {
      index = typeof index !== 'undefined' ? index : 0
      var nth = S('.opiskeluoikeus .suoritus .property.koulutusmoduuli .koulutusmoduuli .tunniste')[index]
      return S(nth).text()
    },
    getOppilaitos: function(index) {
      index = typeof index !== 'undefined' ? index : 0
      return S(S('.opiskeluoikeus > h3 > .oppilaitos')[index]).text().slice(0, -1)
    },
    valitseSuoritus: function(nimi) {
      return function() {
        var tab = findSingle('.suoritus-tabs li:contains(' + nimi + ')')
        if (!tab.hasClass('selected')) {
          triggerEvent(findSingle('a', tab), 'click')
        }
      }
    },
    avaaOpintosuoritusote: function (index) {
      return function() {
        triggerEvent(findSingle('.opiskeluoikeuksientiedot li:nth-child('+index+') a.opintosuoritusote'), 'click')
        return wait.until(OpintosuoritusotePage().isVisible)()
      }
    },
    avaaTodistus: function(index) {
      index = typeof index !== 'undefined' ? index : 0
      return function() {
        triggerEvent(S(S('a.todistus')[index]), 'click')
        return wait.until(TodistusPage().isVisible)()
      }
    },
    avaaLisaysDialogi: function() {
      triggerEvent(S('.opiskeluoikeuden-tiedot .add-item a'), 'click')
      return wait.forAjax()
    },
    valitseOpiskeluoikeudenTyyppi: function(tyyppi) {
      return function() {
        triggerEvent(findSingle('.opiskeluoikeustyypit .' + tyyppi + ' a'), 'click')
        return wait.forAjax()
      }
    },
    suoritusEditor: function() {
      return Editor(function() { return findSingle('.suoritus') })
    },
    opiskeluoikeusEditor: function() {
      return Editor(function() { return findSingle('.opiskeluoikeuden-tiedot') })
    },
    anythingEditable: function() {
      return Editor(function() { return findSingle('.content-area') } ).isEditable()
    },
    expandAll: function() {
      var checkAndExpand = function() {
        if (expanders().is(':visible')) {
          triggerEvent(expanders(), 'click')
          return wait.forMilliseconds(10)().then(wait.forAjax).then(checkAndExpand)
        }
      }
      return checkAndExpand()
      function expanders() {
        return S('.foldable.collapsed>.toggle-expand:not(.disabled), tbody:not(.expanded) > tr > td > .toggle-expand:not(.disabled), a.expandable:not(.open)')
      }
    },
    collapseAll: function() {
      var checkAndCollapse = function() {
        if (collapsers().is(':visible')) {
          triggerEvent(collapsers(), 'click')
          return wait.forMilliseconds(10)().then(wait.forAjax).then(checkAndCollapse)
        }
      }
      return checkAndCollapse()
      function collapsers() {
        return S('.foldable:not(.collapsed)>.toggle-expand:not(.disabled), tbody.expanded .toggle-expand:not(.disabled), a.expandable.open')
      }
    }
  }

  return api
}

function OpiskeluoikeusDialog() {
  return {
    tila: function() {
      return Property(function() {return S('.lisaa-opiskeluoikeusjakso')})
    },
    tallenna: function() {
      triggerEvent(findSingle('button.opiskeluoikeuden-tila'), 'click')
      return wait.forAjax()
    }
  }
}

function Editor(elem) {
  return {
    edit: function() {
      triggerEvent(findSingle('.toggle-edit:not(.editing)', elem()), 'click')
      return KoskiPage().verifyNoError()
    },
    doneEditing: function() {
      triggerEvent(findSingle('.toggle-edit.editing', elem()), 'click')
      return KoskiPage().verifyNoError()
    },
    isEditable: function() {
      return elem().find('.toggle-edit').is(':visible')
    },
    property: function(key) {
      return Property(function() {return findSingle('.property.'+key+':eq(0)', elem())})
    },
    propertyBySelector: function(selector) {
      return Property(function() {return findSingle(selector, elem())})
    },
    subEditor: function(selector) {
      return Editor(function() { return findSingle(selector, elem()) })
    }
  }
}

function Property(elem) {
  return _.merge({
    addValue: function() {
      triggerEvent(findSingle('.add-value', elem()), 'click')
      return KoskiPage().verifyNoError()
    },
    addItem: function() {
      var link = findSingle('.add-item a', elem())
      triggerEvent(link, 'click')
      return KoskiPage().verifyNoError()
    },
    removeValue: function() {
      triggerEvent(findSingle('.remove-value', elem()), 'click')
      return KoskiPage().verifyNoError()
    },
    removeItem: function(index) {
      return function() {
        triggerEvent(findSingle('li:eq(' + index + ') .remove-item', elem()), 'click')
        return KoskiPage().verifyNoError()
      }
    },
    waitUntilLoaded: function() {
      return wait.until(function(){
        return elem().is(':visible') && !elem().find('.loading').is(':visible')
      })()
    },
    setValue: function(value) {
      return function() {
        return Page(elem).setInputValue('select, input', value)()
      }
    },
    click: function(selector) {
      return function() {
        triggerEvent(findSingle(selector, elem()), 'click')
        return KoskiPage().verifyNoError()
      }
    },
    getValue: function() {
      return findSingle('.value', elem()).text()
    },
    getText: function() {
      return extractAsText(elem())
    },
    itemEditor: function(index) {
      return this.propertyBySelector('.array li:nth-child(' + (index + 1) +')')
    },
    getItems: function() {
      return toArray(elem().find('.value .array li')).map(function(elem) { return Property(function() { return S(elem) })})
    },
    isVisible: function() {
      try{
        return findSingle('.value', elem()).is(":visible")
      } catch (e) {
        if (e.message.indexOf('not found') > 0) return false
        throw e
      }
    }
  }, Editor(elem))
}