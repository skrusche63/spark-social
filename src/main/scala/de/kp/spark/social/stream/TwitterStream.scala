package de.kp.spark.social.stream
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

import org.apache.spark.streaming._
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.streaming.StreamingContext

import org.apache.spark.streaming.dstream.DStream

import org.apache.spark.SparkContext._
import org.apache.spark.storage.StorageLevel

import org.apache.spark.streaming.twitter.TwitterUtils

import twitter4j.auth.OAuthAuthorization
import twitter4j.conf.{Configuration,ConfigurationBuilder}
import twitter4j.Status

import org.apache.log4j.Logger
import org.apache.log4j.Level

import org.apache.commons.pool2.impl.{GenericObjectPool, GenericObjectPoolConfig}

import de.kp.spark.social.model._
import de.kp.spark.social.TwitterParser

class TwitterStream(@transient val ssc:StreamingContext) extends Serializable{

    Logger.getLogger("org").setLevel(Level.ERROR)
    Logger.getLogger("akka").setLevel(Level.ERROR)
    
    
  def run(settings:Map[String,String] = Map.empty[String,String]) {

    try {
   
      val oauth = Some(setupOAuth(settings))
      val stream:DStream[Status] = TwitterUtils.createStream(ssc, oauth, Seq(), StorageLevel.MEMORY_AND_DISK)

      val producerPool = ssc.sparkContext.broadcast(createKafkaContextPool(settings))

      /*
       * STEP #1: Parse every tweet and extract tags, urls and more
       */
      val tweets = stream.map(status => {
      
        /* Tweet identifier */
        val uid = status.getId()

        /* Geo location */
       val geo = status.getGeoLocation()
      
       val lat = (if (geo == null) 0.0 else geo.getLatitude())
       val lon = (if (geo == null) 0.0 else geo.getLongitude())

       /* Message */
       val text = status.getText()
       
       /* Extract additional data from text */
       val (hashtags,cashtags,urls,users) = TwitterParser.parse(text)

       /* User */
       val user = status.getUser().getId()

       /* Place */
       val place = status.getPlace()
      
       Tweet(uid,user,lat,lon,place,hashtags,cashtags,urls,users,text)
       
      })
      
      /*
       * STEP #2: Focus on hashtags and determine count for every tag
       * and send tag count to Apache Kafka for further processing and
       * publishing
       */
      val hashtags = tweets.flatMap(tweet => tweet.hashtags).countByValue()
      hashtags.foreachRDD(rdd => {
      
        val settings = Map.empty[String,String]     
        rdd.foreachPartition(partition => {
        
          val producer = producerPool.value.borrowObject()
          partition.foreach(entry => {
          
            val ser = Serializer.serializeTagCount(new TagCount(entry._1,entry._2))
            val bytes = ser.getBytes()
          
            producer.send(bytes,"hashtag_count")
          
          })
          producerPool.value.returnObject(producer)
        
        })
      
      })
      /*
       * STEP #3: Focus on cashtags and determine count for every tag
       * and send tag count to Apache Kafka for further processing and
       * publishing
       */
      val cashtags = tweets.flatMap(tweet => tweet.cashtags).countByValue()
     cashtags.foreachRDD(rdd => {
      
        val settings = Map.empty[String,String]     
        rdd.foreachPartition(partition => {
        
          val producer = producerPool.value.borrowObject()
          partition.foreach(entry => {
          
            val ser = Serializer.serializeTagCount(new TagCount(entry._1,entry._2))
            val bytes = ser.getBytes()
          
            producer.send(bytes,"cashtag_count")
          
          })
          producerPool.value.returnObject(producer)
        
        })
      
      })
      
    } catch {
      case e:Exception => println(e.getMessage)
    }    
    
    ssc.start()
    ssc.awaitTermination()    

  }

  /**
   * The results of stream processing are sent to Apache Kafka for 
   * later processing or visualization through websocket access
   */
  private def createKafkaContextPool(settings:Map[String,String]):GenericObjectPool[ProducerContext] = {
    
    val ctxFactory = new BaseProducerContextFactory(settings)
    val pooledProducerFactory = new PooledProducerContextFactory(ctxFactory)
    
    val poolConfig = {
    
      val c = new GenericObjectPoolConfig
      val maxNumProducers = 10
      
      c.setMaxTotal(maxNumProducers)
      c.setMaxIdle(maxNumProducers)
      
      c
    
    }
    
    new GenericObjectPool[ProducerContext](pooledProducerFactory, poolConfig)
  
  }
  
  /**
   * Build twitter configuration object (mostly from
   * user credentials)
   */
  private def buildConfiguration(settings:Map[String,String]):Configuration = {

    val builder = new ConfigurationBuilder()

    val consumer_key = settings("twitter.consumer.key")
    builder.setOAuthConsumerKey(consumer_key)
    
    val consumer_secret = settings("twitter.consumer.secret")
    builder.setOAuthConsumerSecret(consumer_secret)
    
    val access_token = settings("twitter.access.token")
    builder.setOAuthAccessToken(access_token)
    
    val access_tokenSecret = settings("twitter.access.token.secret")    
    builder.setOAuthAccessTokenSecret(access_tokenSecret)
    
    /**
     * This is relevant due to the SSL/Traffic limitation 
     */
    builder.setUseSSL(true)
    builder.build()

  }
  
  /**
   * Create an OAuth based authorization handle
   */
  private def setupOAuth(settings:Map[String,String]):OAuthAuthorization = {
   
    val conf = buildConfiguration(settings)
    new OAuthAuthorization(conf)
    
  }
}