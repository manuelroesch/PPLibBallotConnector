package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompQueryProperties

/**
 * Created by mattia on 06.07.15.
 */
private[ballot]
class BallotProperties(
                        batch1: Batch,
                        asset1: List[Asset],
                        allowedAnswersPerTurker1: Int,
                        paymentCents: Int = 0,
                        hints1: Long) extends HCompQueryProperties(paymentCents) {

  val batch = batch1
  val assets = asset1
  val allowedAnswersPerTurker = allowedAnswersPerTurker1
  val hints = hints1
}

private[ballot]
case class Batch(uuid: UUID = UUID.randomUUID())

private[ballot]
case class Asset(binary: Array[Byte], contentType: String, filename: String)