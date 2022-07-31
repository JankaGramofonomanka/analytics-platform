package io.github.JankaGramofonomanka.analyticsplatform.common.KV

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._

trait TagTopic[F[_]] {
  def publish(tag: UserTag): F[Unit]
}
