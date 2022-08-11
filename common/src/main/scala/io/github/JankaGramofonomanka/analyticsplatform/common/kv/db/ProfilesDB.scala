package io.github.JankaGramofonomanka.analyticsplatform.common.kv.db

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._

trait ProfilesDB[F[_]] {

  def getProfile(cookie: Cookie): F[TrackGen[SimpleProfile]]
  def updateProfile(cookie: Cookie, profile: TrackGen[SimpleProfile]): F[Boolean]
  
}




