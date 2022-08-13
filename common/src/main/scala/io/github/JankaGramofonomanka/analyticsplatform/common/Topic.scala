package io.github.JankaGramofonomanka.analyticsplatform.common

import fs2.Stream

object Topic {
  trait Publisher[F[_], T] {
    def publish(x: T): F[Unit]
  }

  trait Subscriber[F[_], T] {
    def subscribe: Stream[F, T]
  }
}

