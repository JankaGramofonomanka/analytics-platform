package io.github.JankaGramofonomanka.analyticsplatform.common.KV

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._

trait ProfilesDB[F[_]] {

  def getProfile(cookie: Cookie): F[SimpleProfile]
  def updateProfile(cookie: Cookie, profile: SimpleProfile): F[Unit]
  
}




