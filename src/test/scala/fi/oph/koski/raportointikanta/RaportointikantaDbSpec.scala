package fi.oph.koski.raportointikanta

import fi.oph.koski.db.PostgresDriverWithJsonSupport.api._
import org.postgresql.util.PSQLException
import org.scalatest.{BeforeAndAfterAll, FreeSpec, Matchers}

class RaportointikantaDbSpec extends FreeSpec with Matchers with RaportointikantaTestMethods with BeforeAndAfterAll {
  "Drop and create public schema" in {
    dropsAndCreatesSchemaObjects(mainRaportointiDb)
  }

  "Drop and create temp schema" in {
    dropsAndCreatesSchemaObjects(tempRaportointiDb)
  }

  "Moves schema" in {
    dropAll(mainRaportointiDb)
    schemaIsEmpty(mainRaportointiDb)
    dropsAndCreatesSchemaObjects(tempRaportointiDb)
    schemaExists(tempRaportointiDb)
    tempRaportointiDb.moveTo(Public)
    schemaIsEmpty(tempRaportointiDb)
    schemaExists(mainRaportointiDb)
  }

  private def dropsAndCreatesSchemaObjects(db: RaportointiDatabase) = {
    dropAll(db)
    schemaIsEmpty(db)
    db.dropAndCreateObjects
    schemaExists(db)
  }

  private def schemaExists(db: RaportointiDatabase) {
    db.tables.map(_.baseTableRow.tableName).foreach { tableName =>
      db.runDbSync(sql" SELECT '#${db.schema.name}.#$tableName'::regclass".as[String]).head should endWith(tableName)
    }
  }

  private def schemaIsEmpty(db: RaportointiDatabase) {
    db.tables.map(_.baseTableRow.tableName).foreach { tableName =>
      val thrown = the[PSQLException] thrownBy db.runDbSync(sql" SELECT '#${db.schema.name}.#$tableName'::regclass".as[String])
      thrown.getMessage should include regex s""""${db.schema.name}(.$tableName)?" does not exist"""
    }
  }

  private def dropAll(db: RaportointiDatabase) {
    db.dropAndCreateObjects
    db.runDbSync(RaportointiDatabaseSchema.dropAllIfExists(db.schema))
  }
}
