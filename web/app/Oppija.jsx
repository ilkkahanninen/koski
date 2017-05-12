import React from 'baret'
import Bacon from 'baconjs'
import Http from './http'
import {
  modelTitle,
  modelLookup,
  modelData,
  applyChanges,
  getModelFromChange,
  getPathFromChange,
  modelValid
} from './editor/EditorModel'
import {editorMapping} from './editor/Editors.jsx'
import {Editor} from './editor/Editor.jsx'
import R from 'ramda'
import {currentLocation} from './location.js'
import {OppijaHaku} from './OppijaHaku.jsx'
import Link from './Link.jsx'
import {increaseLoading, decreaseLoading} from './loadingFlag'
import delays from './delays'
import {previousLocation, navigateToOppija, navigateWithQueryParams, locationP, showError} from './location'
import {buildClassNames} from './classnames'
import {addExitHook, removeExitHook} from './exitHook'
import {listviewPath} from './Oppijataulukko.jsx'

Bacon.Observable.prototype.flatScan = function(seed, f) {
  let current = seed
  return this.flatMapConcat(next =>
    f(current, next).doAction(updated => current = updated)
  ).toProperty(seed)
}

export const savedBus = Bacon.Bus()

let currentState = null

export const reloadOppija = () => {
  let oppijaOid = currentState.oppijaOid
  currentState = null
  navigateToOppija({oid: oppijaOid})
}

export const oppijaContentP = (oppijaOid) => {
  let version = currentLocation().params['versionumero']
  if (!currentState || currentState.oppijaOid != oppijaOid || currentState.version != version) {
    currentState = {
      oppijaOid, version, state: createState(oppijaOid)
    }
  }
  return stateToContent(currentState.state)
}

const createState = (oppijaOid) => {
  const changeBus = Bacon.Bus()
  const editBus = Bacon.Bus()
  const saveChangesBus = Bacon.Bus()
  const cancelChangesBus = Bacon.Bus()
  const editingP = locationP.map(loc => !!loc.params.edit)

  cancelChangesBus.onValue(() => navigateWithQueryParams({edit: false}))
  editBus.onValue((opiskeluoikeusId) => navigateWithQueryParams({edit: opiskeluoikeusId}))

  const queryString = currentLocation().filterQueryParams(key => ['opiskeluoikeus', 'versionumero'].includes(key)).queryString

  const oppijaEditorUri = `/koski/api/editor/${oppijaOid}${queryString}`

  const loadOppijaE = Bacon.once(!!currentLocation().params.edit).merge(cancelChangesBus.map(false)).map((edit) => () => Http.cachedGet(oppijaEditorUri, { willHandleErrors: true}).map( oppija => R.merge(oppija, { event: edit ? 'edit' : 'view' })))

  let changeBuffer = null

  const shouldThrottle = (batch) => {
    let model = getModelFromChange(batch[0])
    var willThrottle = model && (model.type == 'string' || model.type == 'date' || model.oneOfClass == 'localizedstring')
    return willThrottle
  }

  const localModificationE = changeBus.flatMap(firstBatch => {
    if (changeBuffer) {
      changeBuffer = changeBuffer.concat(firstBatch)
      return Bacon.never()
    } else {
      //console.log('start batch', firstBatch)
      changeBuffer = firstBatch
      return Bacon.once(oppijaBeforeChange => {
        let batchEndE = shouldThrottle(firstBatch) ? Bacon.later(delays().stringInput).merge(saveChangesBus).take(1) : Bacon.once()
        return batchEndE.flatMap(() => {
          let opiskeluoikeusPath = getPathFromChange(firstBatch[0]).slice(0, 6).join('.')
          let opiskeluoikeusId = modelData(oppijaBeforeChange, opiskeluoikeusPath).id

          let batch = changeBuffer
          changeBuffer = null
          //console.log("Apply", batch.length / 2, "changes:", batch)
          let locallyModifiedOppija = applyChanges(oppijaBeforeChange, batch)
          return R.merge(locallyModifiedOppija, {event: 'dirty', opiskeluoikeusId})
        })
      })
    }
  })

  const saveOppijaE = saveChangesBus.map(() => oppijaBeforeSave => {
    var oppijaData = modelData(oppijaBeforeSave)
    let opiskeluoikeusId = oppijaBeforeSave.opiskeluoikeusId
    let opiskeluoikeudet = oppijaData.opiskeluoikeudet.flatMap(x => x.opiskeluoikeudet).flatMap(x => x.opiskeluoikeudet)
    let opiskeluoikeus = opiskeluoikeudet.find(x => x.id == opiskeluoikeusId)

    let oppijaUpdate = {
      henkilö: {oid: oppijaOid},
      opiskeluoikeudet: [opiskeluoikeus]
    }

    return Bacon.once(R.merge(oppijaBeforeSave, { event: 'saving', inProgress: true})).concat(
      Http.put('/koski/api/oppija', oppijaUpdate, { willHandleErrors: true, invalidateCache: ['/koski/api/oppija', '/koski/api/opiskeluoikeus', '/koski/api/editor/' + oppijaOid]})
        .flatMap(() => Http.cachedGet(oppijaEditorUri, { errorHandler: function(e) { e.topLevel=true; showError(e) }})) // loading after save fails -> rare, not easily recoverable error, show full screen
        .map( oppija => R.merge(oppija, { event: 'saved' }))
    )
  })

  const editE = editingP.skipDuplicates().changes().filter(R.identity).map(() => (oppija) => Bacon.once(R.merge(oppija, { event: 'edit' })))

  let allUpdatesE = Bacon.mergeAll(loadOppijaE, localModificationE, saveOppijaE, editE) // :: EventStream [Model -> EventStream[Model]]


  let oppijaP = allUpdatesE.flatScan({ loading: true }, (currentOppija, updateF) => {
    increaseLoading()
    return updateF(currentOppija).doAction((x) => { if (!x.inProgress) decreaseLoading() }).doError(decreaseLoading)
  })
  oppijaP.onValue()
  oppijaP.map('.event').filter(event => event == 'saved').onValue(() => savedBus.push(true))

  const stateP = oppijaP.map('.event').mapError('dirty').flatMapLatest(event => {
    if (event == 'saved') {
      return Bacon.once(event).concat(Bacon.later(5000, 'view'))
    }
    return event
  }).toProperty().doAction((state) => {
    state == 'dirty' ? addExitHook('Haluatko varmasti poistua sivulta? Tallentamattomat muutokset menetetään.') : removeExitHook()
    if (state == 'saved') navigateWithQueryParams({edit: undefined})
  }) // i18n

  return { oppijaP, changeBus, editBus, saveChangesBus, cancelChangesBus, stateP}
}


const stateToContent = ({ oppijaP, changeBus, editBus, saveChangesBus, cancelChangesBus, stateP}) => oppijaP.map(oppija => ({
  content: (<div className='content-area'><div className="main-content oppija">
    <OppijaHaku/>
    <Link className="back-link" href={listviewPath()}>Opiskelijat</Link>
    <ExistingOppija {...{oppija, changeBus, editBus, saveChangesBus, cancelChangesBus, stateP}}/>
  </div></div>),
  title: modelData(oppija, 'henkilö') ? 'Oppijan tiedot' : ''
}))


export const ExistingOppija = React.createClass({
  render() {
    let {oppija, changeBus, editBus, saveChangesBus, cancelChangesBus, stateP} = this.props
    oppija = Editor.setupContext(oppija, {saveChangesBus, editBus, changeBus, editorMapping})
    let henkilö = modelLookup(oppija, 'henkilö')
    let hetu = modelTitle(henkilö, 'hetu')
    return oppija.loading
      ? <div className="loading"/>
      : (
        <div>
          <div className={stateP.map(state => 'oppija-content ' + state)}>
            <h2>{modelTitle(henkilö, 'sukunimi')}, {modelTitle(henkilö, 'etunimet')} <span
              className='hetu'>{hetu && '(' + hetu + ')'}</span>
              <a className="json" href={`/koski/api/oppija/${modelData(henkilö, 'oid')}`}>JSON</a>
            </h2>
            {
              // Set location as key to ensure full re-render when context changes
              oppija
                ? <Editor key={document.location.toString()} model={oppija}/>
                : null
            }
          </div>
          <EditBar {...{saveChangesBus, cancelChangesBus, stateP, oppija}}/>
        </div>
    )
  }
})

const EditBar = ({stateP, saveChangesBus, cancelChangesBus, oppija}) => {
  let saveChanges = (e) => {
    e.preventDefault()
    saveChangesBus.push()
  }

  let dirtyP = stateP.map(state => state == 'dirty')
  let validationErrorP = dirtyP.map(dirty => dirty && !modelValid(oppija))
  let canSaveP = dirtyP.and(validationErrorP.not())
  let messageMap = {
    saved: 'Kaikki muutokset tallennettu.',
    saving: 'Tallennetaan...',
    dirty: validationErrorP.map(error => error ? 'Korjaa virheelliset tiedot.': 'Tallentamattomia muutoksia'),
    edit: 'Ei tallentamattomia muutoksia'
  }
  let messageP = stateP.decode(messageMap)
  let classNameP = Bacon.combineAsArray(stateP, messageP.map(msg => msg ? 'visible' : '')).map(buildClassNames)

  return (<div id="edit-bar" className={classNameP}>
    <a className={stateP.map(state => ['edit', 'dirty'].includes(state) ? 'cancel' : 'cancel disabled')} onClick={ () => cancelChangesBus.push() }>Peruuta</a>
    <button disabled={canSaveP.not()} onClick={saveChanges}>Tallenna</button>
    <span className="state-indicator">{messageP}</span>
  </div>)
}