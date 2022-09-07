package io.github.JankaGramofonomanka.analyticsplatform.common.aerospike

import scala.util.Try
import scala.util.{Success, Failure}

import cats.effect.{IO, Async}

import com.aerospike.client.{AerospikeClient, Key, Record, Bin, Operation}

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
import io.github.JankaGramofonomanka.analyticsplatform.common.aerospike.Client
import io.github.JankaGramofonomanka.analyticsplatform.common.aerospike.Data._


class AerospikeClientIO(client: AerospikeClient) extends Client[IO] {

  private def printIfErr[A](msg: String, e: Try[A]): Try[A] = e match {
    case Success(v) => {
      println(s"$msg: success")
      Success(v)
    }
    case Failure(e) => {
      println(s"$msg: $e")
      Failure(e)
    }
  }

  private def getRecord(key: Key): IO[TrackGenT[Option, Record]] = for {
    record <- Async[IO].async_[Record] { cb =>
      cb(printIfErr("get", Try(client.get(client.readPolicyDefault, key))).toEither)
    }
    optRecord = Utils.checkForNull(record)
    generation = optRecord.map(_.generation).getOrElse(Generation.default)
  } yield TrackGenT(TrackGen(optRecord, generation))

  def get(key: Key, binName: BinName): IO[TrackGenT[Option, Array[Byte]]]
    = for {
        record <- getRecord(key)
    } yield for {
      record <- record
      obj <- Utils.checkForNull(record.getValue(binName.value))
    } yield obj.asInstanceOf[Array[Byte]]

  def put(key: Key, binName: BinName, bytes: Array[Byte], expectedGeneration: Generation): IO[Boolean]
    = {
      val policy = client.writePolicyDefault
      policy.generation = expectedGeneration

      val bin = new Bin(binName.value, bytes)
      
      for {
        result <- Async[IO].async_[Boolean] { cb =>
          cb(Right(printIfErr("put", Try(client.operate(policy, key, Operation.put(bin)))).isSuccess))
        }
      } yield result
    }
  
}
