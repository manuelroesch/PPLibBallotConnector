package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.{Answer, Question}
import org.joda.time.DateTime

/**
 * Created by mattia on 06.07.15.
 */
trait DAO {

  def getAssetIdsByQuestionId(questionId: Long) : List[Long]

  def createAsset(binary: Array[Byte], contentType: String, questionId: Long, filename: String): Long

  def createBatch(allowedAnswerPerTurker: Int, uuid: UUID): Long

  def createQuestion(html: String, batchId: Long, uuid: UUID = UUID.randomUUID(), dateTime: DateTime = new DateTime(), permutationId: Long): Long

  def getAnswer(questionId: Long): Option[String]

  def getQuestionUUID(questionId: Long): Option[String]

  def getBatchIdByUUID(uuid: UUID): Option[Long]

  def updateAnswer(answerId: Long, accepted: Boolean)

  def getAnswerIdByOutputCode(insertOutput: String): Option[Long]

  def getExpectedOutputCodeFromAnswerId(ansId: Long) : Option[Long]

  def getQuestionIdByUUID(uuid: String) : Option[Long]

  def countAllAnswers() : Int

  def countAllBatches() : Int

  def countAllQuestions() : Int

  def getAllQuestions : List[Question]

  def getAssetFileNameByQuestionId(qId: Long) : Option[String]

  def allAnswers(): List[Answer]

}
