package services

import core.node.{NodeDirectoryService, Node, NodeSubscriptionService}
import models.{HubNode, DomainConfiguration}
import org.scala_tools.subcut.inject.{BindingModule, Injectable}
import core.{HubModule, DomainServiceLocator}

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
class HubNodeSubscriptionService(implicit val bindingModule: BindingModule) extends NodeSubscriptionService with Injectable {

  private val nodeDirectoryServiceLocator = inject [ DomainServiceLocator[NodeDirectoryService] ]
  private val broadcastingNodeSubscriptionService: NodeSubscriptionService = HubModule.inject[NodeSubscriptionService](name = None)

  def generateSubscriptionRequest(to: Node, from: Node)(implicit configuration: DomainConfiguration) {
    broadcastingNodeSubscriptionService.processSubscriptionRequest(to, from)
  }

  def generateSubscriptionResponse(to: Node, from: Node, accepted: Boolean)(implicit configuration: DomainConfiguration) {
    broadcastingNodeSubscriptionService.processSubscriptionResponse(to, from, accepted)
  }

  def processSubscriptionRequest(to: Node, from: Node)(implicit configuration: DomainConfiguration) {
    // for the moment, accept blindly
    HubNode.dao.findOne(to).foreach { receiver =>
      HubNode.addContact(receiver, from)
      generateSubscriptionResponse(from, to, true)
    }
  }

  def processSubscriptionResponse(to: Node, from: Node, accepted: Boolean)(implicit configuration: DomainConfiguration) {
    if (accepted) {
      HubNode.dao.findOne(to).foreach { receiver =>
        HubNode.addContact(receiver, from)
      }
    }
  }

  def listActiveSubscriptions(node: Node)(implicit configuration: DomainConfiguration): Seq[Node] = {
    HubNode.dao.findOne(node).map { node =>
      node.contacts.flatMap { contact =>
        if (contact == configuration.node.nodeId) {
          Some(configuration.node)
        } else {
          nodeDirectoryServiceLocator.byDomain.findOneById(contact)
        }
      }
    }.getOrElse(Seq.empty)
  }
}