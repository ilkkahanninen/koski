package fi.oph.koski.tiedonsiirto

import java.sql.Timestamp
import java.time.LocalDateTime

import fi.oph.koski.db.KoskiDatabase.DB
import fi.oph.koski.db.Tables.{Tiedonsiirto, TiedonsiirtoRow, TiedonsiirtoWithAccessCheck}
import fi.oph.koski.db.{GlobalExecutionContext, KoskiDatabaseMethods}
import fi.oph.koski.koskiuser.KoskiUser
import org.json4s.JsonAST.JValue
import fi.oph.koski.db.PostgresDriverWithJsonSupport.api._
import fi.oph.koski.util.Timing

class TiedonsiirtoRepository(val db: DB) extends GlobalExecutionContext with KoskiDatabaseMethods with Timing {
  val maxResults = 1000

  def create(kayttajaOid: String, tallentajaOrganisaatioOid: String, oppija: Option[JValue], oppilaitos: Option[JValue], error: Option[TiedonsiirtoError]) {
    val (data, virheet) = error.map(e => (Some(e.data), Some(e.virheet))).getOrElse((None, None))

    db.run {
      Tiedonsiirto.map { row => (row.kayttajaOid, row.tallentajaOrganisaatioOid, row.oppija, row.oppilaitos, row.data, row.virheet) } += (kayttajaOid, tallentajaOrganisaatioOid, oppija, oppilaitos, data, virheet)
    }
  }

  def findByOrganisaatio(koskiUser: KoskiUser): Seq[TiedonsiirtoRow] = {
    val dayAgo = Timestamp.valueOf(LocalDateTime.now.minusHours(24))
    timed("findByOrganisaatio") {
      runDbSync(TiedonsiirtoWithAccessCheck(koskiUser).filter(_.aikaleima > dayAgo).sortBy(_.id.desc).take(10000).result)
    }
  }
}

case class TiedonsiirtoError(data: JValue, virheet: JValue)