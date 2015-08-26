package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.{Answer, Permutation}
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

  override def createQuestion(html: String, batchId: Long, uuid: UUID = UUID.randomUUID(), dateTime: DateTime = new DateTime(), hints: Long): Long = {
    DB localTx { implicit session =>
      sql"insert into question(batch_id, html, create_time, uuid, hints) values(${batchId}, ${html}, ${dateTime}, ${uuid.toString}, ${hints})".updateAndReturnGeneratedKey().apply()
    }
  }

  override def getQuestionIdByUUID(uuid: String) : Option[Long] = {
    DB readOnly { implicit session =>
      sql"select id from question where uuid = ${uuid}".map(rs => rs.long("id")).single().apply()
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

  def loadPermutationsCSV(csv: String) : Boolean = {
    DB localTx { implicit session =>

      sql"SET FOREIGN_KEY_CHECKS = 0".update().apply()
      sql"truncate table permutations".update().apply()
      sql"SET FOREIGN_KEY_CHECKS = 1".update().apply()

      sql"""LOAD DATA LOCAL INFILE ${csv}
      INTO TABLE permutations
      COLUMNS TERMINATED BY ','
      OPTIONALLY ENCLOSED BY '"'
      ESCAPED BY '"'
      LINES TERMINATED BY '\n'
      IGNORE 1 LINES
        (group_name, method_index, snippet_filename, pdf_path, method_on_top)""".update().apply()
    }

    true
  }

  def getAllPermutations() : List[Permutation] = {
    DB readOnly { implicit session =>
      sql"select * from permutations".map(rs =>
        Permutation(rs.long("id"), rs.string("group_name"), rs.string("method_index"), rs.string("snippet_filename"), rs.string("pdf_path"), rs.boolean("method_on_top"), rs.long("state"), rs.int("excluded_step"))
      ).list().apply()
    }
  }

  def getPermutationById(id: Long) : Option[Permutation] = {
    DB readOnly { implicit session =>
      sql"select * from permutations where id = ${id}".map(rs =>
        Permutation(rs.long("id"), rs.string("group_name"), rs.string("method_index"), rs.string("snippet_filename"), rs.string("pdf_path"), rs.boolean("method_on_top"), rs.long("state"), rs.int("excluded_step"))
      ).single().apply()
    }
  }

  def getAllOpenByGroupName(groupName: String) : List[Permutation] = {
    DB readOnly { implicit session =>
      sql"select * from permutations where group_name = ${groupName} and state = 0".map(rs =>
        Permutation(rs.long("id"), rs.string("group_name"), rs.string("method_index"), rs.string("snippet_filename"), rs.string("pdf_path"), rs.boolean("method_on_top"), rs.long("state"), rs.int("excluded_step"))
      ).list().apply()
    }
  }

  def updateStateOfPermutationId(id: Long, becauseOfId: Long, excludedByStep: Int = 0) {
    DB localTx { implicit session =>
      sql"update permutations SET state = ${becauseOfId}, excluded_step = ${excludedByStep} WHERE id = ${id}"
        .update().apply()
    }
  }

  def getAllOpenGroupsStartingWith(partialGroupName: String) : List[Permutation] = {
    val result : List[Permutation] = getAllPermutationsWithStateEquals(0)
    result.filter(r => r.groupName.startsWith(partialGroupName)).map(m => m)
  }

  def getAllQuestions : List[Question] = {
    DB readOnly { implicit session =>
      sql"select * from question".map(rs => Question(rs.long("id"), rs.long("hints"))).list().apply()
    }
  }

  def getAssetFileNameByQuestionId(qId: Long) : Option[String] = {
    DB readOnly { implicit session =>
      sql"select filename from assets where question_id = ${qId}".map(rs => rs.string("filename")).single().apply()
    }
  }

  def getAllPermutationsWithStateEquals(state: Long): List[Permutation] = {
    DB readOnly { implicit session =>
      sql"select * from permutations where state = ${state}".map(rs =>
        Permutation(rs.long("id"), rs.string("group_name"), rs.string("method_index"), rs.string("snippet_filename"), rs.string("pdf_path"), rs.boolean("method_on_top"), rs.long("state"), rs.int("excluded_step"))
      ).list().apply()
    }
  }

  case class Question(id: Long, hints: Long)


  def getAllAnswers() : List[Answer] = {
    DB readOnly { implicit session =>
      sql"select * from answer".map(rs =>
        Answer(rs.long("id"), rs.long("question_id"), rs.string("answer_json"), rs.boolean("accepted"))
      ).list().apply()
    }
  }

  def getHintByQuestionId(qId: Long) : Option[Long] = {
    DB readOnly { implicit session =>
      sql"select hints from question where id = ${qId}".map(rs =>
        rs.long("hints")).single().apply()
    }
  }

  def getAllAnswersBySnippet(fileName: String) : List[Answer] = {
    getAllAnswers.filter(f => f.answerJson.contains(fileName))
  }

}
