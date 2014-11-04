package de.kp.spark.social.rest
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

import java.util.Date

import org.apache.spark.streaming.StreamingContext

import akka.actor.{ActorRef,ActorSystem,Props}
import akka.pattern.ask

import akka.util.Timeout

import spray.http.StatusCodes._

import spray.routing.{Directives,HttpService,RequestContext,Route}

import scala.concurrent.{ExecutionContext}
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt

import scala.util.parsing.json._

import de.kp.spark.social.Configuration
import de.kp.spark.social.actor.{SocialMaster}

import de.kp.spark.social.model._

class RestApi(host:String,port:Int,system:ActorSystem,@transient val ssc:StreamingContext) extends HttpService with Directives {

  implicit val ec:ExecutionContext = system.dispatcher  
  import de.kp.spark.social.rest.RestJsonSupport._
  
  override def actorRefFactory:ActorSystem = system
  
  val (duration,retries,time) = Configuration.actor   
  val master = system.actorOf(Props(new SocialMaster(ssc)), name="SocialMaster")
 
  def start() {
    RestService.start(routes,system,host,port)
  }

  private def routes:Route = {

    path("stream" / Segment) {task => 
	  post {
	    respondWithStatus(OK) {
	      ctx => doStream(ctx,task)
	    }
	  }
    }  ~ 
    path("status") {
	  post {
	    respondWithStatus(OK) {
	      ctx => doStatus(ctx)
	    }
	  }
    } 
    
  }

  private def doStream[T](ctx:RequestContext,task:String) = {
	    
	task match {

	  case "start" => doRequest(ctx,"social","start")
	  case "stop"  => doRequest(ctx,"social","stop")
	      
	  case _ => {}

	}
    
  }

  private def doStatus[T](ctx:RequestContext) = doRequest(ctx,"social","status")
  
  private def doRequest[T](ctx:RequestContext,service:String,task:String) = {
     
    val request = new ServiceRequest(service,task,getRequest(ctx))
    implicit val timeout:Timeout = DurationInt(time).second
    
    val response = ask(master,request).mapTo[ServiceResponse] 
    ctx.complete(response)
    
  }

  private def getHeaders(ctx:RequestContext):Map[String,String] = {
    
    val httpRequest = ctx.request
    
    /* HTTP header to Map[String,String] */
    val httpHeaders = httpRequest.headers
    
    Map() ++ httpHeaders.map(
      header => (header.name,header.value)
    )
    
  }
 
  private def getBodyAsMap(ctx:RequestContext):Map[String,String] = {
   
    val httpRequest = ctx.request
    val httpEntity  = httpRequest.entity    

    val body = JSON.parseFull(httpEntity.data.asString) match {
      case Some(map) => map
      case None => Map.empty[String,String]
    }
      
    body.asInstanceOf[Map[String,String]]
    
  }
  
  private def getRequest(ctx:RequestContext):Map[String,String] = {

    val headers = getHeaders(ctx)
    val body = getBodyAsMap(ctx)
    
    headers ++ body
    
  }

}