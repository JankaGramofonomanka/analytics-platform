package io.github.JankaGramofonomanka.analyticsplatform.common.KV

import cats.effect.IO


import com.aerospike.client.{AerospikeClient, Key, Record, Bin}
import com.aerospike.client.policy.{Policy, WritePolicy}

import io.circe.syntax._
import io.circe.parser._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.{ProfilesDB, AggregatesDB}
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.JsonCodec._

object Aerospike {

  final case class Config(
    readPolicy: Policy,
    writePolicy: WritePolicy,
    namespace: String,
    profilesSetName: String,
    aggregatesSetName: String,
    profileBinName: String,
    aggregateBinName: String,
  )
  

  class DB(client: AerospikeClient, config: Config) extends ProfilesDB[IO] with AggregatesDB[IO] {

    private def mkKey(key: String): Key = new Key(config.namespace, config.profilesSetName, key)

    private def checkForNull[A](x: A): Option[A] = if (x == null) None else Some(x)

    private def getRecord(keyS: String): IO[Option[Record]] = {
      val key = mkKey(keyS)
      for {
        record <- IO.delay(client.get(config.readPolicy, key))
      } yield checkForNull(record)
    }

    private def putBin(keyS: String, bin: Bin): IO[Unit] = {
      val key = mkKey(keyS)
      IO.delay(client.put(config.writePolicy, key, bin))
    }
    
    private def decodeProfile(record: Record): Option[SimpleProfile] = {
      val obj = checkForNull(record.bins.get(config.profileBinName))
      
      obj.map {
        
        obj => {
        
          val bytes = obj.asInstanceOf[Array[Byte]]
          val str = new String(bytes)
          val decodeResult = decode[SimpleProfile](str)

          // TODO propagate the error?
          decodeResult match {
            case Right(profile) => profile
            case Left(_)        => SimpleProfile.default
          }
        }
      }
    }

    private def encodeProfile(profile: SimpleProfile): Bin = {
      val name = config.profileBinName
      val value: Array[Byte] = profile.asJson.noSpaces.toString.getBytes
      new Bin(name, value)
    }

    // TODO export to codecs
    private def encodeAggregateInfo(info: AggregateInfo): String = info.asJson.noSpaces.toString

    private def encodeAggregateValue(aggregateValue: AggregateValue): Bin = {
      val name = config.aggregateBinName
      val value: Array[Byte] = aggregateValue.asJson.noSpaces.toString.getBytes
      new Bin(name, value)
    }
    
    private def decodeAggregateValue(record: Record): Option[AggregateValue] = {
      val obj = checkForNull(record.bins.get(config.aggregateBinName))

      obj.map {
        obj => {
        
          val bytes = obj.asInstanceOf[Array[Byte]]
          val str = new String(bytes)
          val decodeResult = decode[AggregateValue](str)

          // TODO propagate the error?
          decodeResult match {
            case Right(value) => value
            case Left(_)      => AggregateValue.default
          }
        }
      }
    }

    def getProfile(cookie: Cookie): IO[SimpleProfile] = for {
      record <- getRecord(cookie.value)
      profile <- IO.delay(record.map(r => decodeProfile(r).getOrElse(SimpleProfile.default)))
    } yield profile.getOrElse(SimpleProfile.default)

    def updateProfile(cookie: Cookie, profile: SimpleProfile): IO[Unit]
      = putBin(cookie.value, encodeProfile(profile))

    def getAggregate(info: AggregateInfo): IO[AggregateValue] = for {
      record <- getRecord(encodeAggregateInfo(info))
      value = record.flatMap(r => decodeAggregateValue(r))

    } yield value.getOrElse(AggregateValue.default)

    def updateAggregate(info: AggregateInfo, value: AggregateValue): IO[Unit]
      = putBin(encodeAggregateInfo(info), encodeAggregateValue(value))

  }
  



}

