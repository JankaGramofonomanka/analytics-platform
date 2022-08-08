package io.github.JankaGramofonomanka.analyticsplatform.common

import cats._

object Utils {
  def checkForNull[A](x: A): Option[A] = if (x == null) None else Some(x)

  def isSortedWith[A](cmp: (A, A) => Boolean)(l: Seq[A]): Boolean = l match {
    case Nil  => true
    case l    => l.init.zip(l.tail).forall { case (x, y) => cmp(x, y) }
  }

  def pure[F[_]: Monad, A](x: A): F[A] = implicitly[Monad[F]].pure(x)

  def getEnvVar(varName: String): String
    = sys.env.get(varName).getOrElse(throw new NoEnvironmentVariableException(varName))

  final case class NoEnvironmentVariableException(varName: String)
  extends Exception(s"Undefined environment variable `$varName`", None.orNull)
}
