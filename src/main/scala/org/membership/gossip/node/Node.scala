package org.membership.gossip.node

import org.membership.gossip.config.GossipConfig

import java.net.{InetAddress, InetSocketAddress}
import java.time.{Duration, LocalDateTime}
import scala.runtime.VolatileBooleanRef

class Node(initAddress: InetSocketAddress,
           initSeqNumber: Long,
           config: GossipConfig) extends Serializable {

  val address: InetSocketAddress = initAddress
  private var heartBeatSeqNumber: Long = initSeqNumber
  private val failed: VolatileBooleanRef = new VolatileBooleanRef(false)
  private var lastUpdateTime: Option[LocalDateTime] = None
  private var gossipConfig: GossipConfig = config

  def getAddress: String = address.getHostName

  def getInetAddress: InetAddress = address.getAddress

  def getPort: Int = address.getPort

  def getUniqueId: String = address.toString

  def setGossipConfig(config: GossipConfig): Unit = gossipConfig = config

  def setLastUpdatedTime(): Unit = {
    lastUpdateTime = Some(LocalDateTime.now())
    println("Node " + getUniqueId + " updated at " + lastUpdateTime.toString)
  }

  def getSequenceNumber: Long = heartBeatSeqNumber

  def updateSequenceNumber(newSeqNumber: Long):Unit = {
    if(newSeqNumber > heartBeatSeqNumber) {
      heartBeatSeqNumber = newSeqNumber
      println("Updated seq number of node " + getUniqueId + " to " + heartBeatSeqNumber.toString)
    }
    setLastUpdatedTime()
  }

  def incrementSequenceNumber(): Unit = {
    heartBeatSeqNumber += 1
    setLastUpdatedTime()
  }

  def setFailed(toSet: Boolean): Unit = failed.elem = toSet

  def hasFailed: Boolean = failed.elem

  def checkIfFailed(): Unit = {
    val failureTime: LocalDateTime = lastUpdateTime.get.plus(gossipConfig.cleanupTimeout)
    failed.elem = LocalDateTime.now().isAfter(failureTime)
  }

  def shouldCleanup: Boolean = {
    if(failed.elem) {
      val cleanupTimeout: Duration = gossipConfig.failureTimeout.plus(gossipConfig.cleanupTimeout)
      val cleanupTime: LocalDateTime = lastUpdateTime.get.plus(cleanupTimeout)
      LocalDateTime.now().isAfter(cleanupTime)
    } else {
      false
    }
  }

  def getNetworkMessage: String = "[" + address.getHostName + ":" + address.getPort + "-" + heartBeatSeqNumber + "]"

}


