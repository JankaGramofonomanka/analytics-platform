package io.github.JankaGramofonomanka.analyticsplatform.common


object ErrorMessages {

  private def inTicks(thing: String) = s"`$thing`"
  
  private def failedToDecode(typeOfThing: String, thing: String): String = s"Failed to decode $typeOfThing ${inTicks(thing)}"
  def failedToDecodeParameter(paramName: String): String = failedToDecode("parameter", paramName)
  def failedToDecodeParameterS(paramName: String): String = failedToDecode("parameter(s)", paramName)

  def unknown(typeOfThing: String, unknownThing: String) = s"unknown $typeOfThing: ${inTicks(unknownThing)}"
  
  def cannotParse(typeOfThing: String): String = s"Cannot parse $typeOfThing"
  def cannotParseWithMsg(typeOfThing: String, msg: String): String = s"${cannotParse(typeOfThing)}: $msg"

}
