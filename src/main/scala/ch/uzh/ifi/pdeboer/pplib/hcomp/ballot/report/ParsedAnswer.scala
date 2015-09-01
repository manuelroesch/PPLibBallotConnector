package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.report

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.snippet.SnippetHTMLQueryBuilder

/**
 * Created by mattia on 31.08.15.
 */
object AnswerParser {
  def isPositive(toCheck: Option[String]): Option[Boolean] = {
    if(toCheck.isDefined){
      Some(toCheck.get.equalsIgnoreCase(SnippetHTMLQueryBuilder.POSITIVE))
    }else {
      None
    }
  }

  def isNegative(toCheck: Option[String]): Option[Boolean] = {
    if(toCheck.isDefined){
      Some(toCheck.get.equalsIgnoreCase(SnippetHTMLQueryBuilder.NEGATIVE))
    }else {
      None
    }
  }

  def parseAnswers(answers: List[Map[String, String]]): List[ParsedAnswer] = {
    answers.map(ans => {
      val isRelated = ans.get("isRelated")
      val isCheckedBefore = ans.get("isCheckedBefore")
      val likert = ans.get("confidence")
      val descriptionIsRelated = ans.get("descriptionIsRelated")

      ParsedAnswer(isRelated, isCheckedBefore, likert.get.toInt, descriptionIsRelated.get)
    })
  }
}

case class ParsedAnswer(q1: Option[String], q2: Option[String], likert: Int, feedback: String)

