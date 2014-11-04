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

import com.twitter.Extractor

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

object TwitterParser {

  private val extractor = new Extractor()
  
  def parse(text:String):(List[String],List[String],List[String],List[String]) = {
    
    /* Extract HashTags */
    val hashtags = extractor.extractHashtags(text).toList     
    
    /* Extract CashTags */
    val cashtags = extractor.extractCashtags(text).toList
    
    /* Extract URLs */
    val urls = extractor.extractURLs(text).toList

    /* Extract mentioned Users */
    val users = extractor.extractMentionedScreennames(text).toList
    
    (hashtags,cashtags,urls,users)
    
  }


}