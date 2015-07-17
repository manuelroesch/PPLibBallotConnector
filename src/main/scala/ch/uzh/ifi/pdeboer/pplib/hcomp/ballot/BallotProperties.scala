package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompQueryProperties
import org.joda.time.DateTime

import scala.util.Random

/**
 * Created by mattia on 06.07.15.
 */
private[ballot]
class BallotProperties(
                        batch1: Batch,
                        asset1: List[Asset],
                        allowedAnswersPerTurker1: Int,
                        outputCode1: Long = Math.abs(new Random(new DateTime().getMillis).nextLong())) extends HCompQueryProperties {

  val batch = batch1
  val assets = asset1
  val allowedAnswersPerTurker = allowedAnswersPerTurker1
  val outputCode = outputCode1
}

private[ballot]
case class Batch(uuid: UUID = UUID.randomUUID())

private[ballot]
case class Asset(binary: Array[Byte], contentType: String)