package ch.uzh.ifi.pplibballotconnector.hcomp.ballot

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompQueryProperties
import org.joda.time.DateTime

import scala.util.Random

/**
 * Created by mattia on 06.07.15.
 */
private[ballot]
case class BallotProperties(
                        batch: Batch,
                        allowedAnswersPerTurker: Int,
                        outputCode: Long = Math.abs(new Random(new DateTime().getMillis).nextLong())) extends HCompQueryProperties {

}

private[ballot]
case class Batch(uuid: UUID = UUID.randomUUID())
