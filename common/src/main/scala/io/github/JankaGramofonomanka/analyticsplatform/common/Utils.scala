package io.github.JankaGramofonomanka.analyticsplatform.common

object Utils {
  def checkForNull[A](x: A): Option[A] = if (x == null) None else Some(x)

  def isSortedWith[A](cmp: (A, A) => Boolean)(l: Seq[A]): Boolean = l match {
    case Nil  => true
    case l    => l.init.zip(l.tail).forall { case (x, y) => cmp(x, y) }
  }
}
