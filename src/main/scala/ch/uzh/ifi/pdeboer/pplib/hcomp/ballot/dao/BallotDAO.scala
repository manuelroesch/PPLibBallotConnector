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

  override def getAnswerIdByOutputCode(insertOutput: String): Option[Long] = {
    DB readOnly { implicit session =>
      sql"select id from answer where expected_output_code = ${insertOutput}".map(rs => rs.long("id")).single().apply()
    }
  }

  override def getExpectedOutputCodeFromAnswerId(ansId: Long) : Option[Long] = {
    DB readOnly { implicit session =>
      sql"select expected_output_code from answer where id = ${ansId}".map(rs => rs.long("expected_output_code")).single().apply()
    }
  }

  override def createQuestion(html: String, batchId: Long, uuid: UUID = UUID.randomUUID(), dateTime: DateTime = new DateTime()): Long = {
    DB localTx { implicit session =>
      sql"insert into question(batch_id, html, create_time, uuid) values(${batchId}, ${html}, ${dateTime}, ${uuid.toString})".updateAndReturnGeneratedKey().apply()
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

  override def updateAnswer(answerId: Long, accepted: Boolean) = {
    DB localTx { implicit session =>
      sql"update answer SET accepted = ${accepted} WHERE id = ${answerId}"
        .update().apply()
    }
  }

  override def getAssetIdsByQuestionId(questionId: Long): List[Long] = {
    DB readOnly { implicit session =>
      sql"select * from assets where question_id = ${questionId}".map(rs => rs.long("id")).list().apply()
    }
  }

  def getPermutationsIdsByPdfName(pdfName: String): List[Long] = {
    DB readOnly { implicit session =>
      DB readOnly { implicit session =>
        sql"select * from permutations where pdf_name = ${pdfName}".map(rs => rs.long("id")).list().apply()
      }
    }
  }

  def getStateOfPermutationId(id: Long) : Option[Long] = {
    DB readOnly { implicit session =>
      sql"select state from permutations where id = ${id}".map(rs => rs.long("state")).single().apply()
    }
  }

  def getGroupByPermutationId(id: Long) : Option[String] = {
    DB readOnly { implicit session =>
      sql"select permutation_group from permutations where id = ${id}".map(rs => rs.string("permutation_group")).single().apply()
    }
  }

  def getMethodUpByPermutationId(id: Long) : Option[Boolean] = {
    DB readOnly { implicit session =>
      sql"select method_up from permutations where id = ${id}".map(rs => rs.boolean("method_up")).single().apply()
    }
  }

  def updateStateOfPermutationIds(ids: List[Long], newState: Long) = {
    DB localTx { implicit session =>
      ids.foreach(id => {
        sql"update permutations SET state = ${newState} WHERE id = ${id}"
          .update().apply()
      })
    }
  }

  def getIdsByGroupAndPdfName(group: Int, pdfName: String) : List[Long] = {
    DB readOnly { implicit session =>
      sql"""select id from permutations where pdf_name = ${pdfName} and group_nr = ${group}""".map(rs => rs.long("id")).list().apply()
    }
  }

  def getIdsByAssumptionAndPdfName(assumption: String, pdfName: String): List[Long] = {
    DB readOnly { implicit session =>
      sql"""select id from permutations where pdf_name = ${pdfName} and permutation_group like %/${assumption}""".map(rs => rs.long("id")).list().apply()
    }
  }

}
