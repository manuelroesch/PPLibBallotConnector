package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao

import java.util.UUID

import org.joda.time.DateTime

/**
 * Created by mattia on 06.07.15.
 */
trait DAO {

  def getAssetIdsByQuestionId(questionId: Long) : List[Long]

  def createAsset(binary: Array[Byte], contentType: String, questionId: Long, filename: String): Long

  def createBatch(allowedAnswerPerTurker: Int, uuid: UUID): Long

  def createQuestion(html: String, outputCode: Long, batchId: Long, uuid: UUID = UUID.randomUUID(), dateTime: DateTime = new DateTime()): Long

  def getAnswer(questionId: Long): List[String]

  def getQuestionUUID(questionId: Long): Option[String]

  def getBatchIdByUUID(uuid: UUID): Option[Long]

}
