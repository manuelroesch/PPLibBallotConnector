package ch.uzh.ifi.pplibballotconnector.hcomp

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompQueryProperties

/**
 * Created by mattia on 06.07.15.
 */
case class BallotProperties(
                        id: Long,
                        uuid: UUID,
                        allowedAnswersPerTurker: Int) extends HCompQueryProperties() {
}
