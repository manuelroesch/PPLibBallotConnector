package ch.uzh.ifi.pplibballotconnector.hcomp

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompQuery, HCompWorker, HCompAnswer}

/**
 * Created by mattia on 06.07.15.
 */
case class BallotAnswer(
                    override val query: HCompQuery,
                    val answer: Long,
                    override val responsibleWorkers: List[HCompWorker] = {}
                    ) extends HCompAnswer(query, responsibleWorkers)