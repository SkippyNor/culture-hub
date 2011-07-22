package models

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoCollection

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
trait AccessControl {

  protected def getCollection: MongoCollection

  protected def getAccessField: String = "access"

  def hasAccess(username: String, node: String, create: Boolean = false, read: Boolean = false, update: Boolean = false, delete: Boolean = false) = {
    val query = ("users.reference.username" -> username, "reference.node" -> node, "create" -> create, "read" -> read, "update" -> update, "delete" -> delete)

  }

  def addUserRight(username: String, node: String, create: Boolean = false, read: Boolean = false, update: Boolean = false, delete: Boolean = false) = {

    true
  }


}

/**Access Rights of an object **/
case class AccessRight(users: List[UserAction], groups: List[Group])

/**A User and his rights **/
case class UserAction(user: UserReference,
                      create: Option[Boolean] = Some(false),
                      read: Option[Boolean] = Some(false),
                      update: Option[Boolean] = Some(false),
                      delete: Option[Boolean] = Some(false),
                      owner: Option[Boolean] = Some(false)
                     )

/**A group and its rights **/
case class Group(
                        user: UserReference,
                        name: String,
                        users: List[UserReference],
                        create: Option[Boolean] = Some(false),
                        read: Option[Boolean] = Some(false),
                        update: Option[Boolean] = Some(false),
                        delete: Option[Boolean] = Some(false),
                        owner: Option[Boolean] = Some(false)
                        )

/**An organization, yet to be defined further **/
case class Organization(name: String,
                        public: Boolean,
                        groups: List[Group]
                        )