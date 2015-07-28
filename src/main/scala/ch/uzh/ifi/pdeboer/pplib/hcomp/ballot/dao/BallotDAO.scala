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

  override def getAnswer(questionId: Long): List[String] = {
    DB readOnly { implicit session =>
      sql"select answer_json from answer where question_id = ${questionId}".map(rs => rs.string("answer_json")).list().apply()
    }
  }

  override def createQuestion(html: String, outputCode: Long, batchId: Long, uuid: UUID = UUID.randomUUID(), dateTime: DateTime = new DateTime()): Long = {
    DB localTx { implicit session =>
      sql"insert into question(batch_id, html, output_code, create_time, uuid) values(${batchId}, ${html}, ${outputCode}, ${dateTime}, ${uuid.toString})".updateAndReturnGeneratedKey().apply()
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

  override def createAsset(binary: Array[Byte], contentType: String, questionId: Long, filename: String): Long = {
    DB localTx { implicit session =>
      sql"insert into assets(byte_array, content_type, question_id, filename) values(${binary}, ${contentType}, ${questionId}, ${filename})"
        .updateAndReturnGeneratedKey().apply()
    }
  }

  override def getAssetIdsByQuestionId(questionId: Long): List[Long] = {
    DB readOnly { implicit session =>
      sql"select * from assets where question_id = ${questionId}".map(rs => rs.long("id")).list().apply()
    }
  }
}
