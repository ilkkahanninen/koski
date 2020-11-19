import React from 'baret'
import Text from '../i18n/Text'
import Bacon from 'baconjs'
import Atom from 'bacon.atom'
import {showError} from '../util/location'
import {formatISODate} from '../date/date'
import {generateRandomPassword} from '../util/password'
import {downloadExcel} from './downloadExcel'
import { AikajaksoValinta, Listavalinta, LyhytKuvaus, RaportinLataus, Vinkit } from './raporttiComponents'

export const osasuoritusTypes = {
  TUTKINNON_OSA: 'tutkinnon osat',
  KURSSI: 'kurssisuoritukset'
}

const KaikkiSuorituksetLabel = ({ osasuoritusType }) => {
  switch (osasuoritusType) {
    case osasuoritusTypes.TUTKINNON_OSA:
      return <Text name='Raportille valitaan kaikki tutkinnon osat riippumatta niiden suoritusajankohdasta' />
    case osasuoritusTypes.KURSSI:
      return <Text name='Raportille valitaan kaikki kurssisuoritukset riippumatta niiden suoritusajankohdasta' />
  }
}

const AikarajatutSuorituksetLabel = ({ osasuoritusType }) => {
  switch (osasuoritusType) {
    case osasuoritusTypes.TUTKINNON_OSA:
      return <Text name='Raportille valitaan vain sellaiset tutkinnon osat, joiden arviointipäivä osuu yllä määritellylle aikajaksolle' />
    case osasuoritusTypes.KURSSI:
      return <Text name='Raportille valitaan vain sellaiset kurssisuoritukset, joiden arviointipäivä osuu yllä määritellylle aikajaksolle' />
  }
}

export const AikajaksoRaporttiAikarajauksella = ({
  organisaatioP,
  apiEndpoint,
  description,
  osasuoritusType = osasuoritusTypes.TUTKINNON_OSA
}) => {
  const alkuAtom = Atom()
  const loppuAtom = Atom()
  const osasuoritustenAikarajausAtom = Atom(false)
  const submitBus = Bacon.Bus()

  const password = generateRandomPassword()

  const downloadExcelP = Bacon.combineWith(
    organisaatioP, alkuAtom, loppuAtom, osasuoritustenAikarajausAtom,
    (o, a, l, r) => o && a && l && (l.valueOf() >= a.valueOf()) && {
      oppilaitosOid: o.oid,
      alku: formatISODate(a),
      loppu: formatISODate(l),
      osasuoritustenAikarajaus: r,
      password,
      baseUrl: `/koski/api/raportit${apiEndpoint}`
    })

  const downloadExcelE = submitBus.map(downloadExcelP).flatMapLatest(downloadExcel)

  downloadExcelE.onError(e => showError(e))

  const inProgressP = submitBus.awaiting(downloadExcelE.mapError())
  const submitEnabledP = downloadExcelP.map(x => !!x).and(inProgressP.not())

  return (
    <section>
      <LyhytKuvaus>
        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam porttitor libero dictum sem rhoncus, at euismod ex finibus. Morbi tortor purus, vehicula ut purus eget, blandit laoreet eros. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Proin tellus ipsum, mattis non purus sed, mattis rutrum arcu.
      </LyhytKuvaus>

      <AikajaksoValinta alkuAtom={alkuAtom} loppuAtom={loppuAtom} />

      <Listavalinta
        label="Valitse osasuoritusten aikarajaus"
        atom={osasuoritustenAikarajausAtom}
        options={[
          { key: false, value: <KaikkiSuorituksetLabel osasuoritusType={osasuoritusType} /> },
          { key: true, value: <AikarajatutSuorituksetLabel osasuoritusType={osasuoritusType} /> }
        ]}
      />

      <RaportinLataus
        password={password}
        inProgressP={inProgressP}
        submitEnabledP={submitEnabledP}
        submitBus={submitBus}
      />

      <Vinkit>
        <p>{description}</p>
      </Vinkit>
    </section>
  )
}
