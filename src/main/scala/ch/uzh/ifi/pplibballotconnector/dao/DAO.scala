package ch.uzh.ifi.pplibballotconnector.dao

/**
 * Created by mattia on 06.07.15.
 */
trait DAO {

  def createBatch(allowedAnswerPerTurker: Int): Long

  def createQuestion(html: String, outputCode: Long, batchId: Long): Long

  def getAnswer(questionId: Long): Option[String]

  def getQuestionUUID(questionId: Long): Option[String]
}
