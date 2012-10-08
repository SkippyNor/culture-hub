package core

/**
 * Node registration service
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
trait NodeRegistrationService {

  /**
   * Registers a new node. The organization doing the request will automatically become the owner.
   *
   * @param node the node to register
   * @param userName the userName of who registered the node
   */
  def registerNode(node: Node, userName: String)

  /**
   * Updates a node information
   *
   * @param node the node to update
   */
  def updateNode(node: Node)

  /**
   * Removes a node
   *
   * @param node the node to update
   */
  def removeNode(node: Node)

  /**
   * Adds a member to a node
   *
   * @param node the node to update
   * @param userName the member to add
   */
  def addMember(node: Node, userName: String)

  /**
   * Removes a member from a node
   *
   * @param node the node to update
   * @param userName the member to remove
   */
  def removeMember(node: Node, userName: String)

}
