package de.kp.spark.social.model
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

import org.json4s._

import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read,write}

import twitter4j.{GeoLocation,Place}

case class TagCount(tag:String,count:Long)

case class Tweet(
  uid:Long,
  user:Long,
  lat:Double,
  lon:Double,
  place:Place,
  hashtags:List[String],
  cashtags:List[String],
  urls:List[String],
  users:List[String],  
  /* The textual descritpion of the body */
  text:String
)

object Serializer {
    
  implicit val formats = Serialization.formats(NoTypeHints)

  def serializeTagCount(tagcount:TagCount):String = write(tagcount)

}
