package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompQueryProperties

/**
 * Created by mattia on 06.07.15.
 */
class BallotProperties(
						  val batch: Batch,
						  val assets: List[Asset],
						  val allowedAnswersPerTurker: Int,
						  paymentCents: Int = 0,
						  val permutationId: Long,
						  val propertiesForDecoratedPortal: HCompQueryProperties = new HCompQueryProperties()) extends HCompQueryProperties(paymentCents) {

	assert(paymentCents <= 0 || paymentCents == propertiesForDecoratedPortal.paymentCents, "paymentCents of decorated properties will be used. Please set paymentCents of Ballot properties instance to 0")
}

case class Batch(uuid: UUID = UUID.randomUUID())

case class Asset(binary: Array[Byte], contentType: String, filename: String)