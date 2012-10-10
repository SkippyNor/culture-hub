package core.node

/**
 * The NodeConnectionService makes it possible for Nodes to connect to each-other following a basic request / response
 * mechanism, inspired by the XMPP protocol (RFC 6121).
 *
 * @see http://xmpp.org/rfcs/rfc6121.html#sub
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
trait NodeConnectionService {

  /**
   * Requests to be connected with another node
   * @param to the target node to request connection with
   * @param from this node
   */
  def requestConnection(to: Node, from: Node)

  /**
   * Handles a connection request
   * @param to the target node of the request
   * @param from the node the request came from
   */
  def receiveConnectionRequest(to: Node, from: Node)

  /**
   * Replies to a connection request
   * @param to the node that originally requested the connection
   * @param from the node replying to the connection request
   * @param accepted whether the connection request was accepted or rejected
   */
  def respondToConnectionRequeset(to: Node, from: Node, accepted: Boolean)

  /**
   * Handles a connection request response
   * @param to the node that originally requested the connection
   * @param from the node replying to the connection request
   * @param accepted whether the connection request was accepted or rejected
   */
  def handleConnectionResponse(to: Node, from: Node, accepted: Boolean)

}
