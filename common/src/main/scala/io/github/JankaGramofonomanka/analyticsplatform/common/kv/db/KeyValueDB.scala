package io.github.JankaGramofonomanka.analyticsplatform.common.kv.db

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._

trait KeyValueDB[F[_], K, V] {

  def get(key: K): F[TrackGen[V]]
  def update(key: K, value: TrackGen[V]): F[Boolean]
  
}




