package de.kp.spark.social.actor
/* Copyright (c) 2014 Dr. Krusche & Partner PartG
* 
* This file is part of the Spark-Social project
* (https://github.com/skrusche63/spark-social).
* 
* Spark-Social is free software: you can redistribute it and/or modify it under the
* terms of the GNU General Public License as published by the Free Software
* Foundation, either version 3 of the License, or (at your option) any later
* version.
* 
* Spark-Social is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with
* Spark-Social. 
* 
* If not, see <http://www.gnu.org/licenses/>.
*/

import akka.actor.{Actor,ActorLogging}
import scala.collection._

import org.java_websocket.WebSocket

import de.kp.spark.social.Configuration
import de.kp.spark.social.socket.WebSocketService

object HashtagActor {

  sealed trait HashtagMessage
  case class Unregister(ws:WebSocket) extends HashtagMessage

}

class HashtagActor extends Actor with ActorLogging {

  import HashtagActor._
  import WebSocketService._
  
  implicit val ec = context.dispatcher

  val clients = mutable.ListBuffer[WebSocket]()
  
  def receive = {

    case Close(ws,code,reason,ext) => self ! Unregister(ws)

    case Error(ws,ex) => self ! Unregister(ws)

    case Message(ws,msg) => {
      log.debug("url {} received msg '{}'", ws.getResourceDescriptor, msg)
    }
    
    case Open(ws,hs) => {

      clients += ws
      log.debug("registered monitor for url {}", ws.getResourceDescriptor)
    
    }

    case Unregister(ws) => {
      
      if (null != ws) {
        log.debug("unregister monitor")
        clients -= ws
      }
    
    }
    
    case message:String => {
      
      for (client <- clients) {
        client.send(message)
      }
    
    }
    
    case _ => {/* do nothing */}
    
  }

}
