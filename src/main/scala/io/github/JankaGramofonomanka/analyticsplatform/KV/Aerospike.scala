package io.github.JankaGramofonomanka.analyticsplatform.KV

import cats.effect.IO
import cats.implicits._

import com.aerospike.client.{AerospikeClient, Key, Record, Bin}
import com.aerospike.client.policy.{Policy, WritePolicy}

import io.circe.syntax._
import io.circe.parser._

import io.github.JankaGramofonomanka.analyticsplatform.Data._
import io.github.JankaGramofonomanka.analyticsplatform.KV.{ProfilesDB, AggregatesDB}
import io.github.JankaGramofonomanka.analyticsplatform.codecs.JsonCodec.simpleProfileDecoder
import io.github.JankaGramofonomanka.analyticsplatform.codecs.JsonCodec.simpleProfileEncoder
import io.github.JankaGramofonomanka.analyticsplatform.codecs.JsonCodec.aggregateInfoEncoder
import io.github.JankaGramofonomanka.analyticsplatform.codecs.JsonCodec.aggregateValueDecoder

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
    
    private def decodeProfile(record: Record): Option[SimpleProfile] = {
      println(s"$record")
      val obj = checkForNull(record.bins.get(config.profileBinName))
      
      obj.map {
        
        obj => {
        
          val bytes = obj.asInstanceOf[Array[Byte]]
          val str = new String(bytes)
          val decodeResult = decode[SimpleProfile](str)
          println(s"$decodeResult")

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
      //val value: Array[Byte] = profile.asJson.noSpaces.toString.getBytes
      val str = profile.asJson.noSpaces.toString
      println(s"profile.asJson: $str")
      val value: Array[Byte] = str.getBytes
      new Bin(name, value)
    }

    // TODO probably need to export because the other module will use theese
    private def encodeAggregateInfo(info: AggregateInfo): String = info.asJson.noSpaces.toString
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

    def updateProfile(cookie: Cookie, profile: SimpleProfile): IO[Unit] = {
      val key = mkKey(cookie.value)
      val bin = encodeProfile(profile)
      IO.delay(client.put(config.writePolicy, key, bin))
    }

    def getAggregates(
        timeRange:  TimeRange,
        action:     Action,
        count:      Boolean,
        sumPrice:   Boolean,
        origin:     Option[Origin],
        brandId:    Option[BrandId],
        categoryId: Option[CategoryId],
    ): IO[Aggregates] = {
      val buckets = timeRange.getBuckets
      val infos = buckets.map(bucket => AggregateInfo(bucket, action, origin, brandId, categoryId))
      val fields = AggregateFields(action, count, sumPrice, origin, brandId, categoryId)
      for {
        records <- infos.traverse { info => getRecord(encodeAggregateInfo(info)) }

        aggregateValues = records
          .map(_.flatMap(r => decodeAggregateValue(r))
                .getOrElse(AggregateValue.default)
              )
        values = buckets.zip(aggregateValues)
        
      } yield Aggregates(fields, values)
    }
  }



}

