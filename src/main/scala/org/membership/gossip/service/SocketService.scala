package org.membership.gossip.service

import org.membership.gossip.node.Node

import java.io.{ByteArrayOutputStream, ObjectOutput, ObjectOutputStream}
import java.net.{DatagramPacket, DatagramSocket, SocketException}

class SocketService(portToListen: Int) {

  var datagramSocket: DatagramSocket = null
  try {
    datagramSocket = new DatagramSocket(portToListen)
  } catch {
    case e : SocketException =>
      println("Could not create socket connection.")
      println(e)
  }

  val receivedBuffer: Array[Byte] = new Array[Byte](1024)
  val receivePacket: DatagramPacket = new DatagramPacket(receivedBuffer, receivedBuffer.length)

  def sendGossip(node: Node, message: Node): Unit = {
    val bytesToWrite: Array[Byte] = getBytesToWrite(message)
    sendGossipMessage(node, bytesToWrite)
  }

  import java.io.ByteArrayInputStream
  import java.io.IOException
  import java.io.ObjectInputStream

  def receiveGossip(): Node = {
    try {
      datagramSocket.receive(receivePacket)
      val objectInputStream: ObjectInputStream = new ObjectInputStream(new ByteArrayInputStream(receivePacket.getData))
      var message: Node = null
      try {
        message = objectInputStream.readObject.asInstanceOf[Node]
        println("Received gossip message from [" + message.getUniqueId + "]")
      } catch {
        case e: ClassNotFoundException =>
          e.printStackTrace()
      } finally {
        objectInputStream.close()
      }
    } catch {
      case e: IOException =>
        e.printStackTrace()
    }
    null
  }

  private def getBytesToWrite(message: Node): Array[Byte] = {
    val bStream = new ByteArrayOutputStream()
    println("Writing message " + message.getNetworkMessage)
    try {
      val oo: ObjectOutput = new ObjectOutputStream(bStream)
      oo.writeObject(message)
      oo.close()
    } catch {
      case e: IOException =>
      println("Could not send " + message.getNetworkMessage + "] because: " + e.getMessage)
      e.printStackTrace();
    }
    bStream.toByteArray
  }

  private def sendGossipMessage(target: Node, data: Array[Byte]): Unit = {
    val packet: DatagramPacket = new DatagramPacket(data, data.length, target.getInetAddress, target.getPort)
    try {
      println("Sending gossip message to [" + target.getUniqueId + "]")
      datagramSocket.send(packet)
    } catch {
      case e: IOException =>
      println("Fatal error trying to send: " + packet + " to [" + target.address + "]")
      e.printStackTrace()
    }
  }
}
