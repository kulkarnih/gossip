package org.membership.gossip.config

import java.time.Duration

case class GossipConfig(failureTimeout: Duration,
                        cleanupTimeout: Duration,
                        updateFrequency: Duration,
                        failureDetectionFreq: Duration,
                        peersToUpdatePerInterval: Int) extends Serializable
