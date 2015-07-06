package ch.uzh.ifi.pplibballotconnector.hcomp

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompQuery, HCompWorker, HCompAnswer}

/**
 * Created by mattia on 06.07.15.
 */
case class BallotAnswer(query: BallotQuery, answer: Long, responsibleWorkers: List[HCompWorker] = Nil) extends HCompAnswer  with Serializable