package core.storage

import eu.delving.basex.client.BaseX
import org.basex.server.ClientSession
import xml.Node
import java.io.ByteArrayInputStream

/**
 * BaseX-based Storage engine.
 *
 * One BaseX db == One collection
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object BaseXStorage {

  // TODO use real config, and non-embedded
  val storage = new BaseX("localhost", 1984, "admin", "admin")

  def createCollection(orgId: String, collectionName: String): Collection = {
    val c = Collection(orgId, collectionName)
    storage.createDatabase(c.databaseName)
    c
  }

  def openCollection(orgId: String, collectionName: String): Option[Collection] = {
    val c = Collection(orgId, collectionName)
    try {
      storage.openDatabase(c.databaseName)
      Some(c)
    } catch {
      case _ => None
    }
  }

  def withBulkSession[T](collection: Collection)(block: ClientSession => T) = {
    storage.withSession(collection.databaseName) {
      session =>
        session.execute("set autoflush false")
        block(session)
        session.execute("flush")
    }
  }

  def newRecord(identifier: String, schemaPrefix: String, document: String, index: Int, namespaces: Map[String, String]) = {

    val ns = namespaces.map(ns => """xmlns:%s="%s"""".format(ns._1, ns._2)).mkString(" ")

    new ByteArrayInputStream("""<record id="%s" %s>
      <system>
        <version>0</version>
        <schemaPrefix>%s</schemaPrefix>
        <index>%s</index>
      </system>
      <document>%s</document>
      <links/>
    </record>""".format(identifier, ns, schemaPrefix, index, document).getBytes("utf-8"))

  }

}

case class Collection(orgId: String, name: String) {
  val databaseName = orgId + "____" + name
}



