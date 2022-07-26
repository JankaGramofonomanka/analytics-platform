package io.github.JankaGramofonomanka.analyticsplatform.KV

import io.github.JankaGramofonomanka.analyticsplatform.Data._

abstract class TagTopic[F[_]] {
  def publish(tag: UserTag): F[Unit]
}
