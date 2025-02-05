import React from "react"
import { Redirect } from "react-router-dom"
import {
  hakeutumisenValvontaAllowed,
  kuntavalvontaAllowed,
  maksuttomuudenValvontaAllowed,
  useKäyttöoikeusroolit,
} from "../state/accessRights"
import { useBasePath } from "../state/basePath"
import {
  createHakutilannePathWithoutOrg,
  createKuntailmoitusPath,
  createMaksuttomuusPath,
} from "../state/paths"
import { AccessRightsView } from "./AccessRightsView"

export const HomeView = () => {
  const redirect = useRedirectPath()
  return redirect ? <Redirect to={redirect} /> : <AccessRightsView />
}

const useRedirectPath = (): string | null => {
  const basePath = useBasePath()
  const roles = useKäyttöoikeusroolit()

  if (kuntavalvontaAllowed(roles)) {
    return createKuntailmoitusPath(basePath)
  }

  if (hakeutumisenValvontaAllowed(roles)) {
    return createHakutilannePathWithoutOrg(basePath)
  }

  if (maksuttomuudenValvontaAllowed(roles)) {
    return createMaksuttomuusPath(basePath)
  }

  return null
}
