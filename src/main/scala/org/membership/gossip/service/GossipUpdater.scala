package org.membership.gossip.service

import java.net.InetSocketAddress

abstract class GossipUpdater{
  def update(inetSocketAddress: InetSocketAddress): Unit
}
