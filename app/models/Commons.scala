package models

import com.novus.salat
import org.bson.types.ObjectId
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import salat.dao.{SalatMongoCursor, SalatDAO}

/**
 * 
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

trait Commons[A <: salat.CaseClass] { self: AnyRef with SalatDAO[A, ObjectId] =>

  RegisterJodaTimeConversionHelpers()

  def findByUser(id: ObjectId) = find(MongoDBObject("user_id" -> id))
  def findAllWithIds(ids: List[ObjectId]) = find(("_id" $in ids))
  def findAll = find(MongoDBObject())
  def findRecent(howMany: Int) = find(MongoDBObject()).sort(MongoDBObject("TS_update" -> -1)).limit(howMany)

}

trait Pager[A <: salat.CaseClass] { self: AnyRef with SalatDAO[A, ObjectId] =>

  import views.context.PAGE_SIZE

  implicit def cursorWithPage(cursor: SalatMongoCursor[A]) = new {

    /**
     * Returns a page and the total object count
     * @param page the page number
     * @param pageSize optional size of the page, defaults to PAGE_SIZE
     */
    def page(page: Int, pageSize: Int = PAGE_SIZE) = {
      val c = cursor.skip((page - 1) * pageSize).limit(PAGE_SIZE)
      (c.toList, c.count)
    }
  }
}