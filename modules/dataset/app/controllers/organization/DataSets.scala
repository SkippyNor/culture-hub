package controllers.organization

import play.api.mvc.{WebSocket, Action}
import models.DataSet
import play.api.i18n.Messages
import com.mongodb.casbah.commons.MongoDBObject
import controllers.{Token, OrganizationController}
import java.util.regex.Pattern
import play.api.libs.json.{JsString, JsValue}
import play.api.libs.concurrent.Promise
import play.api.libs.iteratee.{Enumerator, Done, Input}
import util.DomainConfigurationHandler

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object DataSets extends OrganizationController {

  def list(orgId: String) = OrgMemberAction(orgId) {
    Action {
      implicit request =>
        Ok(Template('title -> listPageTitle("dataset")))
    }
  }

  def dataSet(orgId: String, spec: String) = OrgMemberAction(orgId) {
    Action {
      implicit request =>
        val maybeDataSet = DataSet.dao.findBySpecAndOrgId(spec, orgId)
        if (maybeDataSet.isEmpty) {
          NotFound(Messages("organization.datasets.dataSetNotFound", spec))
        } else {
          val ds = maybeDataSet.get
          Ok(Template('spec -> ds.spec))
        }
    }
  }

  def feed(orgId: String, clientId: String, spec: Option[String]) = WebSocket.async[JsValue] { implicit request  =>
    if(request.session.get("userName").isDefined) {
      val domainConfiguration = DomainConfigurationHandler.getByDomain(request.domain)
      DataSetEventFeed.subscribe(orgId, clientId, session.get("userName").get, domainConfiguration.name, spec)
    } else {
      // return a fake pair
      // TODO perhaps a better way here ?
      Promise.pure((Done[JsValue, JsValue](JsString(""), Input.Empty), Enumerator.imperative()))
    }
  }

  def listAsTokensBySpec(orgId: String, q: String) = OrgMemberAction(orgId) {
    Action {
      implicit request =>
        val dataSets = DataSet.dao.find(MongoDBObject("orgId" -> orgId, "deleted" -> false, "spec" -> Pattern.compile(q, Pattern.CASE_INSENSITIVE)))
        val asTokens = dataSets.map(ds => Token(ds.spec, ds.spec, Some("dataset")))
        Json(asTokens)
    }
  }

}
