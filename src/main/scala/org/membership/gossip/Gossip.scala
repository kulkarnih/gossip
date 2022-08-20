package org.membership.gossip

import org.membership.gossip.config.GossipConfig
import org.membership.gossip.service.GossipService

import java.net.InetSocketAddress
import java.time.Duration

object Gossip {
  def main(args: Array[String]): Unit = {
    val gossipConfig: GossipConfig = new GossipConfig(
      Duration.ofSeconds(3),
      Duration.ofSeconds(3),
      Duration.ofMillis(500),
      Duration.ofMillis(500),
      3
    )

    val initialNode: GossipService = new GossipService(new InetSocketAddress("127.0.0.1", 9090), gossipConfig)

    initialNode.onNewMember = Some((inetSocketAddress: InetSocketAddress) => {
      println("Connected to " + inetSocketAddress.getHostName + ":" + inetSocketAddress.getPort)
    })

    initialNode.onFailedMember = Some((inetSocketAddress: InetSocketAddress) => {
      println("Node " + inetSocketAddress.getHostName + ":" + inetSocketAddress.getPort + " failed")
    })

    initialNode.onRemovedMember = Some((inetSocketAddress: InetSocketAddress) => {
      println("Node " + inetSocketAddress.getHostName + ":" + inetSocketAddress.getPort + " removed")
    })

    initialNode.onRevivedMember = Some((inetSocketAddress: InetSocketAddress) => {
      println("Node " + inetSocketAddress.getHostName + ":" + inetSocketAddress.getPort + " revived")
    })

    initialNode.start()

    1 to 10 foreach((x) => {
      val gossipService: GossipService = new GossipService(new InetSocketAddress("127.0.0.1", 9090 + x), gossipConfig)
      gossipService.setInitialTarget(new InetSocketAddress("127.0.0.1", 9090 + x - 1))
      gossipService.start()
    })
  }
}
