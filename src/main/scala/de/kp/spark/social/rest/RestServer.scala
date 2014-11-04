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

import akka.actor.ActorSystem

import org.apache.spark.streaming._
import org.apache.spark.streaming.StreamingContext._

import org.apache.spark.streaming.StreamingContext

import de.kp.spark.social.{Configuration,SparkService}

object RestServer extends SparkService {
  
  private val sc = createCtxLocal("SocialContext",Configuration.spark)      
  private val ssc = new StreamingContext(sc, Seconds(10))
  
  private def start(args:Array[String],system:ActorSystem) {

    val (host,port) = Configuration.rest
    
    /* Start REST API */
    new RestApi(host,port,system,ssc).start()
      
  }
  
  def main(args: Array[String]) {
    start(args, ActorSystem("RestServer"))
  }
  
}