package de.kp.spark.social.socket
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

import akka.actor.{ActorSystem,Props}

import de.kp.spark.social.Configuration
import de.kp.spark.social.actor.{HashtagActor}

object WebSocketServer {

  implicit lazy val system = ActorSystem("WebSocketSystem")
  
  def run() {
    
    val port = Configuration.websocket
    val service = new WebSocketService(port)
    
    /*
     * Generate actors and services for different web socket descriptors;
     * note, that the different descriptors refer to specific Kafka topics
     */
    val hashtagActor = system.actorOf(Props[HashtagActor], "HashtagActor")
    val hashtagService = new KafkaService("hashtag_count",hashtagActor)
    
    service.forResource("/hashtag_count/ws", Some(hashtagService))
   
    service.start()
    sys.addShutdownHook({system.shutdown;service.stop})
    
  }

  def main(args:Array[String]) {
    run()    
  }
  
}