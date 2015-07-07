package ch.uzh.ifi.pplibballotconnector.dao

import java.util.UUID
import scalikejdbc._
import scalikejdbc.config.DBs

/**
 * Created by mattia on 06.07.15.
 */
class BallotDAO extends DAO{

  override def createBatch(allowedAnswersPerTurker: Int, uuid: String): Long = {
    DB readOnly { implicit session =>
      sql"insert into batch(allowedAnswersPerTurker, uuid) values(${allowedAnswersPerTurker}, ${uuid})".update.apply()
    }
  }

  override def getAnswer(questionId: Long): Option[String] = {
    DB readOnly { implicit session =>
      sql"select from answer where question_id == ${questionId})".map(rs => rs.string("answer_json")).single().apply()
    }
  }

  override def createQuestion(html: String, outputCode: Long, batchId: Long): Long = {
    val uuid = UUID.randomUUID().toString

    DB readOnly { implicit session =>
      sql"insert into question(html, output_code, batch_id, create_time, uuid) values(${html}, ${outputCode}, ${batchId}, CURRENT, ${uuid})".update.apply()
    }
  }

  override def getQuestionUUID(questionId: Long): Option[String] = {
    DB readOnly { implicit session =>
      sql"select from answer where question_id == ${questionId})".map(rs => rs.string("uuid")).single().apply()
    }
  }

  override def getBatchIdByUUID(uuid: String): Option[Long] = {
    DB readOnly { implicit session =>
      sql"select from batch where uuid == ${uuid})".map(rs => rs.long("id")).single().apply()
    }
  }
}
