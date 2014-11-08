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

import org.apache.spark.streaming.StreamingContext
import akka.actor.{ActorRef,Props}

import akka.pattern.ask
import akka.util.Timeout

import akka.actor.{OneForOneStrategy, SupervisorStrategy}

import de.kp.spark.social.{Configuration,Credentials}
import de.kp.spark.social.model._

import de.kp.spark.social.stream.TwitterStream

import scala.concurrent.duration.DurationInt
import scala.concurrent.Future

class SocialMaster(@transient val ssc:StreamingContext) extends BaseActor {
  
  val (duration,retries,time) = Configuration.actor   

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries=retries,withinTimeRange = DurationInt(time).minutes) {
    case _ : Exception => SupervisorStrategy.Restart
  }
  /* 
   * Reference to the streaming source: actually this is twitter,
   * but the datasift connector may also be used here.
   */
  private val twitterStream = new TwitterStream(ssc)
  private var started = false
  
  def receive = {
    
    case req:String => {
      
      implicit val ec = context.dispatcher
      implicit val timeout:Timeout = DurationInt(time).second
	  	    
	  val origin = sender

	  val deser = Serializer.deserializeRequest(req)
	  val response = deser.task.split(":")(0) match {

	    case "start" => {
	      
	      if (started == false) {
	      
	        val settings = Credentials.twitter
	        if (deser.data.contains("filter")) {
	          settings += "filter" -> deser.data("filter")
	        }
	        
	        twitterStream.run(settings)
	        
	        started = true
	        
	        val uid = deser.data("uid")
	        val data = Map("uid" -> uid, "message" -> Messages.STREAMING_STARTED(uid))
	      
	        new ServiceResponse(deser.service,deser.task,data,ResponseStatus.SUCCESS)
	        
	      } else {

	        val uid = deser.data("uid")
	        val data = Map("uid" -> uid, "message" -> Messages.STREAMING_IS_RUNNING(uid))
	      
	        new ServiceResponse(deser.service,deser.task,data,ResponseStatus.SUCCESS)
	        
	      }
	      
	    }
        case "stop"  => {
	      
	      twitterStream.shutdown()
	      started = false
	      
	      val uid = deser.data("uid")
	      val data = Map("uid" -> uid,"messages" -> Messages.STREAMING_STOPPED(uid))
	      
	      new ServiceResponse(deser.service,deser.task,data,ResponseStatus.SUCCESS)
          
        }

        case "status" => null
       
        case _ => failure(deser,Messages.TASK_IS_UNKNOWN(deser.data("uid"),deser.task))
      
      }

      origin ! Serializer.serializeResponse(response)

    }
  
    case _ => {

      val origin = sender               
      val msg = Messages.REQUEST_IS_UNKNOWN()          
          
      origin ! Serializer.serializeResponse(failure(null,msg))

    }
    
  }

}