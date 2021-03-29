import {
  OppijaHakutilanteillaLaajatTiedot,
  OppijaHakutilanteillaPerustiedot,
} from "../state/oppijat"
import { Oid, OrganisaatioJaKayttooikeusrooli, User } from "../state/types"
import { apiGet, apiPost } from "./apiFetch"
import { createCache } from "./cache"

export const healthCheck = async () =>
  apiGet<string>("api/healthcheck/internal")

/**
 * Login
 */
export const fetchLogin = async (username: string, password: string) =>
  apiPost<User>("valpas/login", {
    body: {
      username,
      password,
    },
  })

/**
 * Hae kirjautuneen käyttäjän tiedot
 */
export const fetchCurrentUser = async () => apiGet<User>("valpas/api/user")

/**
 * Hae lista organisaatioista käyttöoikeuksien kanssa
 */
export const fetchYlatasonOrganisaatiotJaKayttooikeusroolit = async () =>
  apiGet<OrganisaatioJaKayttooikeusrooli[]>(
    "valpas/api/organisaatiot-ja-kayttooikeusroolit"
  )

export const fetchYlatasonOrganisaatiotJaKayttooikeusroolitCache = createCache(
  fetchYlatasonOrganisaatiotJaKayttooikeusroolit
)

/**
 * Get oppijat
 */
export const fetchOppijat = (organisaatioOid: Oid) =>
  apiGet<OppijaHakutilanteillaPerustiedot[]>(
    `valpas/api/oppijat/${organisaatioOid}`
  )
export const fetchOppijatCache = createCache(fetchOppijat)

export const fetchOppija = (oppijaOid: Oid) =>
  apiGet<OppijaHakutilanteillaLaajatTiedot>(`valpas/api/oppija/${oppijaOid}`)
export const fetchOppijaCache = createCache(fetchOppija)
