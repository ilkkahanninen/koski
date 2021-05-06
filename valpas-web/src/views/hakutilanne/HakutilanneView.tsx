import bem from "bem-ts"
import * as A from "fp-ts/Array"
import * as Eq from "fp-ts/Eq"
import { pipe } from "fp-ts/lib/function"
import React, { useMemo, useState } from "react"
import { Redirect, useHistory } from "react-router"
import { fetchOppijat, fetchOppijatCache } from "../../api/api"
import { useApiWithParams } from "../../api/apiHooks"
import { isLoading, isSuccess } from "../../api/apiUtils"
import { RaisedButton } from "../../components/buttons/RaisedButton"
import { BottomDrawer } from "../../components/containers/BottomDrawer"
import { Card, CardBody, CardHeader } from "../../components/containers/cards"
import { Dropdown } from "../../components/forms/Dropdown"
import { Spinner } from "../../components/icons/Spinner"
import { DataTableCountChangeEvent } from "../../components/tables/DataTable"
import { Counter } from "../../components/typography/Counter"
import { getLocalized, t, T } from "../../i18n/i18n"
import { useBasePath } from "../../state/basePath"
import {
  Oid,
  onHakeutumisVelvollisuudenValvonnanOikeuksia,
  OrganisaatioHierarkia,
  OrganisaatioJaKayttooikeusrooli,
} from "../../state/common"
import { isFeatureFlagEnabled } from "../../state/featureFlags"
import {
  createHakutilannePathWithOrg,
  HakutilanneViewRouteProps,
} from "../../state/paths"
import { nonNull } from "../../utils/arrays"
import { ErrorView } from "../ErrorView"
import { HakutilanneTable } from "./HakutilanneTable"
import "./HakutilanneView.less"
import { Ilmoituslomake } from "./ilmoituslomake/Ilmoituslomake"
import { VirkailijaNavigation } from "./VirkailijaNavigation"

const b = bem("hakutilanneview")

export type HakutilanneViewProps = HakutilanneViewRouteProps & {
  kayttooikeusroolit: OrganisaatioJaKayttooikeusrooli[]
  redirectJosKäyttäjälläEiOleOikeuksiaTo: string
}

export const HakutilanneViewWithoutOrgOid = (props: HakutilanneViewProps) => {
  return onHakeutumisVelvollisuudenValvonnanOikeuksia(
    props.kayttooikeusroolit
  ) ? (
    <HakutilanneViewWithoutOrgOidContent {...props} />
  ) : (
    <Redirect to={props.redirectJosKäyttäjälläEiOleOikeuksiaTo} />
  )
}

const HakutilanneViewWithoutOrgOidContent = (props: HakutilanneViewProps) => {
  const basePath = useBasePath()
  const organisaatio = getOrganisaatiot(props.kayttooikeusroolit)[0]

  return organisaatio ? (
    <Redirect
      to={createHakutilannePathWithOrg(basePath, {
        organisaatioOid: organisaatio.oid,
      })}
    />
  ) : (
    <OrganisaatioMissingView />
  )
}

export const HakutilanneView = (props: HakutilanneViewProps) => {
  return onHakeutumisVelvollisuudenValvonnanOikeuksia(
    props.kayttooikeusroolit
  ) ? (
    <HakutilanneViewContent {...props} />
  ) : (
    <Redirect to={props.redirectJosKäyttäjälläEiOleOikeuksiaTo} />
  )
}

const HakutilanneViewContent = (props: HakutilanneViewProps) => {
  const history = useHistory()
  const organisaatiot = getOrganisaatiot(props.kayttooikeusroolit)
  const organisaatioOid =
    props.match.params.organisaatioOid || organisaatiot[0]?.oid
  const oppijatFetch = useApiWithParams(
    fetchOppijat,
    organisaatioOid ? [organisaatioOid] : undefined,
    fetchOppijatCache
  )
  const [counters, setCounters] = useState<DataTableCountChangeEvent>({
    filteredRowCount: 0,
    unfilteredRowCount: 0,
  })
  const [selectedOppijaOids, setSelectedOppijaOids] = useState<Oid[]>([])
  const [ilmoituslomakeVisible, showIlmoituslomake] = useState(false)

  const orgOptions = getOrgOptions(organisaatiot)

  const changeOrganisaatio = (oid?: Oid) => {
    if (oid) {
      history.push(oid)
    }
  }

  const selectedOppijat = useMemo(
    () =>
      isSuccess(oppijatFetch)
        ? selectedOppijaOids
            .map((oid) =>
              oppijatFetch.data.find((o) => o.oppija.henkilö.oid === oid)
            )
            .filter(nonNull)
        : [],
    [oppijatFetch, selectedOppijaOids]
  )

  return organisaatioOid ? (
    <div className={b("view")}>
      <Dropdown
        selectorId="organisaatiovalitsin"
        containerClassName={b("organisaatiovalitsin")}
        label={t("Oppilaitos")}
        options={orgOptions}
        value={organisaatioOid || ""}
        onChange={changeOrganisaatio}
      />
      <VirkailijaNavigation />
      <Card>
        <CardHeader>
          <T id="hakutilannenäkymä__otsikko" />
          {isSuccess(oppijatFetch) && (
            <Counter>
              {counters.filteredRowCount === counters.unfilteredRowCount
                ? counters.filteredRowCount
                : `${counters.filteredRowCount} / ${counters.unfilteredRowCount}`}
            </Counter>
          )}
        </CardHeader>
        <CardBody>
          {isLoading(oppijatFetch) && <Spinner />}
          {isSuccess(oppijatFetch) && (
            <HakutilanneTable
              data={oppijatFetch.data}
              organisaatioOid={organisaatioOid}
              onCountChange={setCounters}
              onSelect={setSelectedOppijaOids}
            />
          )}
        </CardBody>
      </Card>
      {isFeatureFlagEnabled("ilmoittaminen") ? (
        <>
          <BottomDrawer>
            <div className={b("ilmoittaminen")}>
              <h4 className={b("ilmoittaminentitle")}>
                <T id="ilmoittaminen_drawer__title" />
              </h4>
              <div className={b("ilmoittamisenalarivi")}>
                <span className={b("valittujaoppilaita")}>
                  <T
                    id="ilmoittaminen_drawer__valittuja_oppilaita"
                    params={{ määrä: selectedOppijaOids.length }}
                  />
                </span>
                <RaisedButton
                  disabled={A.isEmpty(selectedOppijaOids)}
                  onClick={() => showIlmoituslomake(true)}
                >
                  <T id="ilmoittaminen_drawer__siirry_ilmoittamiseen" />
                </RaisedButton>
              </div>
            </div>
          </BottomDrawer>
          {ilmoituslomakeVisible && isSuccess(oppijatFetch) ? (
            <Ilmoituslomake
              oppijat={selectedOppijat}
              onClose={() => showIlmoituslomake(false)}
            />
          ) : null}
        </>
      ) : null}
    </div>
  ) : (
    <OrganisaatioMissingView />
  )
}

const getOrganisaatiot = (
  kayttooikeusroolit: OrganisaatioJaKayttooikeusrooli[]
) =>
  kayttooikeusroolit.map((kayttooikeus) => kayttooikeus.organisaatioHierarkia)

const eqOrgs = Eq.fromEquals(
  (a: OrganisaatioHierarkia, b: OrganisaatioHierarkia) => a.oid === b.oid
)

const getOrgOptions = (orgs: OrganisaatioHierarkia[]) =>
  pipe(
    orgs,
    A.uniq(eqOrgs),
    A.map((org: OrganisaatioHierarkia) => ({
      value: org.oid,
      display: `${getLocalized(org.nimi)} (${org.oid})`,
    }))
  )

const OrganisaatioMissingView = () => (
  <ErrorView
    title={t("hakutilanne__ei_oikeuksia_title")}
    message={t("hakutilanne__ei_oikeuksia_teksti")}
  />
)
