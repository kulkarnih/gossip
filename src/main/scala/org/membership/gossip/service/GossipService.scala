package org.membership.gossip.service

import org.membership.gossip.config.GossipConfig
import org.membership.gossip.node.Node

import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import scala.collection._
import scala.jdk.CollectionConverters.ConcurrentMapHasAsScala
import scala.util.Random

class GossipService (socketAddress: InetSocketAddress, config: GossipConfig){

  val inetSocketAddress: InetSocketAddress = socketAddress
  private val socketService: SocketService = new SocketService(inetSocketAddress.getPort)
  private val gossipConfig: GossipConfig = config
  private val self: Node = new Node(inetSocketAddress, 0, gossipConfig)
  private val nodes: concurrent.Map[String, Node] = new ConcurrentHashMap[String, Node]().asScala

  nodes.putIfAbsent(self.getUniqueId, self)

  private var stopped: Boolean = false
  var onNewMember: Option[GossipUpdater] = None
  var onFailedMember: Option[GossipUpdater] = None
  var onRemovedMember: Option[GossipUpdater] = None
  var onRevivedMember: Option[GossipUpdater] = None

  def setInitialTarget(targetAddress: InetSocketAddress): Node = {
    val initialTarget: Node = new Node(targetAddress, 0, gossipConfig)
    nodes.putIfAbsent(initialTarget.getUniqueId, initialTarget).get
  }

  def start(): Unit = {
    startSenderThread()
    startReceiverThread()
    startFailureDetectionThread()
    printNodes()
  }

  def stop(): Unit = stopped = true

  private def sendGossipToRandomNode(): Unit = {
    self.incrementSequenceNumber()
    val nodesForSampling: List[String] = nodes.keys.filter(x => x != self.getUniqueId).toList
    val nodesToSendGossip: List[String] = Random.shuffle(nodesForSampling).take(gossipConfig.peersToUpdatePerInterval)

    for(targetAddress: String <- nodesToSendGossip) {
      val node: Node = nodes(targetAddress)
      new Thread(() => socketService.sendGossip(node, self)).start()
    }
  }

  private def startSenderThread(): Unit = {
    new Thread(() => {
      while(!stopped) {
        sendGossipToRandomNode()
        try {
          Thread.sleep(gossipConfig.updateFrequency.toMillis)
        } catch {
          case e: InterruptedException => e.printStackTrace()
        }
      }
    }).start()
  }

  private def receivePeerMessage(): Unit = {
    val newNode: Node = socketService.receiveGossip()
    val existingNode: Option[Node] = nodes.get(newNode.getUniqueId)

    if(existingNode.isEmpty) {
      this.synchronized {
        newNode.setGossipConfig(gossipConfig)
        newNode.setLastUpdatedTime()
        nodes.putIfAbsent(newNode.getUniqueId, newNode)
        if(onNewMember.isDefined) onNewMember.get.update(newNode.address)
      }
    } else {
      println("Updating sequence number for node " + existingNode.get.getUniqueId)
      existingNode.get.updateSequenceNumber(newNode.getSequenceNumber)
    }
  }

  private def startReceiverThread(): Unit = {
    new Thread(() => {
      while(!stopped) {
        receivePeerMessage()
      }
    }).start()
  }

  private def detectFailedNodes(): Unit = {
    nodes.keys.foreach(key => {
      val node: Node = nodes(key)
      val hadFailed: Boolean = node.hasFailed
      node.checkIfFailed()

      if(hadFailed != node.hasFailed) {
        if(node.hasFailed) {
          if(onFailedMember.isDefined) onFailedMember.get.update(node.address)
          else if(onRevivedMember.isDefined) onRevivedMember.get.update(node.address)
        }
      }

      if(node.shouldCleanup) {
        nodes.synchronized {
          nodes.remove(key)
          if(onRemovedMember.isDefined) onRemovedMember.get.update(node.address)
        }
      }
    })
  }

  private def startFailureDetectionThread(): Unit = {
    new Thread(() => {
      while(!stopped) {
        detectFailedNodes()
        try {
          Thread.sleep(gossipConfig.failureDetectionFreq.toMillis)
        } catch {
          case e: InterruptedException => e.printStackTrace()
        }
      }
    }).start()
  }

  private def getLiveMembers: List[InetSocketAddress] = {
    nodes.foldRight(List.empty: List[InetSocketAddress])((entry: (String, Node), acc: List[InetSocketAddress]) => {
      val node: Node = entry._2
      node.checkIfFailed()
      if(!node.hasFailed) {
        new InetSocketAddress(node.getAddress, node.getPort) :: acc
      } else {
        acc
      }
    })
  }

  private def getFailedMembers: List[InetSocketAddress] = {

    nodes.foldRight(List.empty: List[InetSocketAddress])((entry: (String, Node), acc: List[InetSocketAddress]) => {
      val node: Node = entry._2
      node.checkIfFailed()
      if(node.hasFailed) {
        new InetSocketAddress(node.getAddress, node.getPort) :: acc
      } else {
        acc
      }
    })

  }

  private def getAllMembers: List[InetSocketAddress] =
    nodes.toList.map(entry => {
      val node: Node = entry._2
      new InetSocketAddress(node.getAddress, node.getPort)
    })

  private def printNodes(): Unit = {
    new Thread(() => {
      try {
        Thread.sleep(3000)
      } catch {
        case e : InterruptedException => e.printStackTrace()
      }

      getLiveMembers.foreach(node => {
        println("Health status: %s:%d - alive".format(node.getHostName, node.getPort))
      })

      getFailedMembers.foreach(node => {
        println("Health status: %s:%d - failed".format(node.getHostName, node.getPort))
      })

    }).start()
  }

}
