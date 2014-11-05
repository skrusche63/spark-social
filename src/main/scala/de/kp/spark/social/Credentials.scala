package de.kp.spark.social
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

import com.typesafe.config.ConfigFactory
import scala.collection.mutable.HashMap

object Credentials {

  private val path = "credentials.conf"
  private val config = ConfigFactory.load(path)

  def twitter():Map[String,String] = {
    
    val cfg = config.getConfig("twitter")

    val consumer_key = cfg.getString("consumer_key")
    val consumer_secret = cfg.getString("consumer_secret")  
    
    val access_token = cfg.getString("access_token")    
    val access_tokenSecret = cfg.getString("access_tokenSecret")

    Map(
        
      "twitter.consumer.key"    -> consumer_key,
      "twitter.consumer.secret" -> consumer_secret,
      
      "twitter.access.token" -> access_token,
      "twitter.access.token.secret" -> access_tokenSecret,
      
      "twitter.filters" -> ""
    
    )

  }

}