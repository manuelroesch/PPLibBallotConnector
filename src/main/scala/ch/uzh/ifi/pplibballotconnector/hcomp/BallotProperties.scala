package ch.uzh.ifi.pplibballotconnector.hcomp

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompQueryProperties
import org.joda.time.DateTime

import scala.util.Random

/**
 * Created by mattia on 06.07.15.
 */
class BallotProperties(
                        batch: Batch,
                        allowedAnswersPerTurker: Int,
                        outputCode: Long = new Random(new DateTime().getMillis).nextLong()) extends HCompQueryProperties {

  def getBatch() = batch
  def getAllowedAnswersPerTurker() = allowedAnswersPerTurker
  def getOutputCode() = outputCode
}

case class Batch(uuid: UUID = UUID.randomUUID())
