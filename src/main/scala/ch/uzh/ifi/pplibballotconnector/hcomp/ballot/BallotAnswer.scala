package ch.uzh.ifi.pplibballotconnector.hcomp.ballot

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompAnswer, HCompQuery, HCompWorker}

/**
 * Created by mattia on 06.07.15.
 */
private[ballot]
case class BallotAnswer(answers: Map[String, String], query: HCompQuery, responsibleWorkers: List[HCompWorker] = Nil)
  extends HCompAnswer  with Serializable {
  def get(key: String) = answers.get(key)
}