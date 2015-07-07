package ch.uzh.ifi.pplibballotconnector.hcomp

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompQuery, HCompWorker, HCompAnswer}

/**
 * Created by mattia on 06.07.15.
 */

case class BallotAnswer(answers: Map[String, String], query: HCompQuery, responsibleWorkers: List[HCompWorker] = Nil)
  extends HCompAnswer  with Serializable {
  def get(key: String) = answers.get(key)
}