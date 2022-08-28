package io.github.JankaGramofonomanka.analyticsplatform.common

import scala.util.{Try, Success, Failure}

import cats._
import cats.implicits._

import java.time.LocalDateTime

object Utils {
  def checkForNull[A](x: A): Option[A] = if (x == null) None else Some(x)

  def isSortedWith[A](cmp: (A, A) => Boolean)(l: Seq[A]): Boolean = l match {
    case Nil  => true
    case l    => l.init.zip(l.tail).forall { case (x, y) => cmp(x, y) }
  }

  def pure[F[_]: Monad, A](x: A): F[A] = implicitly[Monad[F]].pure(x)

  def getEnvVarOption(varName: String): Option[String] = sys.env.get(varName)
  
  def getEnvVar(varName: String): String
    = sys.env.get(varName).getOrElse(throw new NoEnvironmentVariableException(varName))
  
  def getEnvVarOptionInt(varName: String): Option[Int] = getEnvVarOption(varName).map { str => 
    
    Try(str.toInt) match {
      case Success(i) => i
      case Failure(_) => throw new InvalidEnvironmentVariableException(
                                    s"value `$str` of variable `$varName` is not an integer")
    }
  }

  def getEnvVarInt(varName: String): Int
    = getEnvVarOptionInt(varName).getOrElse(throw new NoEnvironmentVariableException(varName))

  def getEnvVarBoolean(varName: String, default: Boolean): Boolean
    = sys.env.getOrElse(varName, "").toUpperCase match {
      case "TRUE"   => true
      case "FALSE"  => false
      case _ => default
    }

  final case class NoEnvironmentVariableException(varName: String)
  extends Exception(s"Undefined environment variable `$varName`", None.orNull)

  final case class InvalidEnvironmentVariableException(varName: String)
  extends Exception(s"invalid value of environment variable `$varName`", None.orNull)

  def roundToMinutes(dt: LocalDateTime): LocalDateTime = {
    val year    = dt.getYear
    val month   = dt.getMonthValue
    val day     = dt.getDayOfMonth
    val hour    = dt.getHour
    val minute  = dt.getMinute

    LocalDateTime.of(year, month, day, hour, minute)
  }

  def tryTillSuccess[F[_]: Monad](tryOnce: F[Boolean]): F[Unit] = for {
    success <- tryOnce
    _ <- if (success) pure[F, Unit](()) else tryTillSuccess(tryOnce)
  } yield ()

  def curry[A, B, C](f: ((A, B)) => C): (A, B) => C = (a, b) => f((a, b))
  def uncurry[A, B, C](f: (A, B) => C): ((A, B)) => C = t => f(t._1, t._2)

  def updateMap[K, V](f: V => V)(map: Map[K, V])(key: K, default: V): Map[K, V] = {
    map.get(key) match {
      case None => map.updated(key, default)
      case Some(value) => map.updated(key, f(value))
    }
  }
}
