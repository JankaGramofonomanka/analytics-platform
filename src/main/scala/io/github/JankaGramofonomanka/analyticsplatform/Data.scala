package io.github.JankaGramofonomanka.analyticsplatform

import java.time.Duration

object Data {
    final case class Cookie(value: String) extends AnyVal
    type TimeRange = Duration
    type Limit = Int

    sealed trait Action
    object VIEW extends Action
    object BUY extends Action

    sealed trait Aggregate
    object COUNT extends Aggregate
    object SUM_PRICE extends Aggregate

    final case class Origin(value: String) extends AnyVal
    final case class BrandId(value: String) extends AnyVal
    final case class CategoryId(value: String) extends AnyVal
}
