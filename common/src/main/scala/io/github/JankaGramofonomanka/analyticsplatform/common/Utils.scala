package io.github.JankaGramofonomanka.analyticsplatform.common

object Utils {
  def checkForNull[A](x: A): Option[A] = if (x == null) None else Some(x)
}
