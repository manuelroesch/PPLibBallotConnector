package ch.uzh.ifi.pplibballotconnector.dao

import java.util.UUID

import scalikejdbc.{DBSession, AutoSession}

/**
 * Created by mattia on 06.07.15.
 */
trait DAO {

  def createBatch(allowedAnswerPerTurker: Int, uuid: String): Long

  def createQuestion(html: String, outputCode: Long, batchId: Long, uuid: String = UUID.randomUUID().toString): Long

  def getAnswer(questionId: Long): Option[String]

  def getQuestionUUID(questionId: Long): Option[String]

  def getBatchIdByUUID(uuid: String): Option[Long]

}
