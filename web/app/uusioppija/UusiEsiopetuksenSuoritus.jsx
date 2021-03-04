import React from 'baret'
import Bacon from 'baconjs'
import Atom from 'bacon.atom'
import {makeSuoritus} from './esiopetuksenSuoritus'
import Peruste from './Peruste'

export default ({suoritusAtom, oppilaitosAtom, organisaatiotyypitAtom, suorituskieliAtom}) => {
  const perusteAtom = Atom()

  Bacon.combineWith(oppilaitosAtom, organisaatiotyypitAtom, perusteAtom, suorituskieliAtom, makeSuoritus)
    .onValue(suoritus => suoritusAtom.set(suoritus))

  let suoritusP = Bacon.combineWith(oppilaitosAtom, perusteAtom, suorituskieliAtom, makeSuoritus)
  suoritusP.filter('.koulutusmoduuli.perusteenDiaarinumero').onValue(suoritus => suoritusAtom.set(suoritus))

  return <Peruste {...{suoritusTyyppiP: suoritusP.map('.tyyppi'), perusteAtom}} />
}
