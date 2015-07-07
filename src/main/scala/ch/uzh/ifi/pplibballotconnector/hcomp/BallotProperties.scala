package ch.uzh.ifi.pplibballotconnector.hcomp

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompQueryProperties

/**
 * Created by mattia on 06.07.15.
 */
class BallotProperties(
                        batch: Batch,
                        allowedAnswersPerTurker: Int) extends HCompQueryProperties {

  def getBatch() = batch
  def getAllowedAnswersPerTurker() = allowedAnswersPerTurker
}

case class Batch(uuid: UUID = UUID.randomUUID())
