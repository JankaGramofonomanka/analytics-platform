package io.github.JankaGramofonomanka.analyticsplatform.KV

import io.github.JankaGramofonomanka.analyticsplatform.Data._

trait TagTopic[F[_]] {
  def publish(tag: UserTag): F[Unit]
}
