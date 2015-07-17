package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao

import java.util.UUID

import org.joda.time.DateTime
import scalikejdbc._

/**
 * Created by mattia on 06.07.15.
 */
class BallotDAO extends DAO{

  def countAllAnswers() = {
    DB readOnly { implicit session =>
      sql"select count(*) as count from answer".map(rs => rs.int("count")).single().apply()
    }
  }

  def countAllBatches() = {
    DB readOnly { implicit session =>
      sql"select count(*) as count from batch".map(rs => rs.int("count")).single().apply()
    }
  }

  def countAllQuestions() = {
    DB readOnly { implicit session =>
      sql"select count(*) as count from question".map(rs => rs.int("count")).single().apply()
    }
  }


  override def createBatch(allowedAnswersPerTurker: Int, uuid: UUID): Long = {
    DB localTx { implicit session =>
      sql"insert into batch(allowed_answers_per_turker, uuid) values(${allowedAnswersPerTurker}, ${uuid.toString})".updateAndReturnGeneratedKey().apply()
    }
  }

  override def getAnswer(questionId: Long): Option[String] = {
    DB readOnly { implicit session =>
      sql"select answer_json from answer where question_id = ${questionId}".map(rs => rs.string("answer_json")).single().apply()
    }
  }

  override def createQuestion(html: String, outputCode: Long, batchId: Long, uuid: String = UUID.randomUUID().toString, dateTime: DateTime = new DateTime()): Long = {
    DB localTx { implicit session =>
      sql"insert into question(html, output_code, batch_id, create_time, uuid) values(${html}, ${outputCode}, ${batchId}, ${dateTime}, ${uuid})".updateAndReturnGeneratedKey().apply()
    }
  }

  override def getQuestionUUID(questionId: Long): Option[String] = {
    DB readOnly { implicit session =>
      sql"select uuid from answer where question_id = ${questionId}".map(rs => rs.string("uuid")).single().apply()
    }
  }

  override def getBatchIdByUUID(uuid: UUID): Option[Long] = {
    DB readOnly { implicit session =>
      sql"select id from batch where uuid = ${uuid.toString}".map(rs => rs.long("id")).single().apply()
    }
  }
}
