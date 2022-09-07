package io.github.JankaGramofonomanka.analyticsplatform.common.aerospike

object Data {
  final case class Namespace(value: String) extends AnyVal
  final case class BinName  (value: String) extends AnyVal
}