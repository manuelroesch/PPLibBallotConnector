package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.report

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.snippet.SnippetHTMLQueryBuilder

/**
 * Created by mattia on 31.08.15.
 */
case class ParsedAnswer(q1: Option[String], q2: Option[String], likert: Int, feedback: String) {

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

}

