package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.{Answer, Permutation, Question}
import org.joda.time.DateTime
import scalikejdbc._

/**
 * Created by mattia on 06.07.15.
 */
class BallotDAO extends DAO {

	override def countAllAnswers(): Int = {
		DB readOnly { implicit session =>
			sql"SELECT count(*) AS count FROM answer".map(rs => rs.int("count")).single().apply().get
		}
	}

	override def countAllBatches(): Int = {
		DB readOnly { implicit session =>
			sql"SELECT count(*) AS count FROM batch".map(rs => rs.int("count")).single().apply().get
		}
	}

	override def countAllQuestions(): Int = {
		DB readOnly { implicit session =>
			sql"SELECT count(*) AS count FROM question".map(rs => rs.int("count")).single().apply().get
		}
	}

	override def createBatch(allowedAnswersPerTurker: Int, uuid: UUID): Long = {
		DB localTx { implicit session =>
			sql"INSERT INTO batch(allowed_answers_per_turker, uuid) VALUES(${allowedAnswersPerTurker}, ${uuid.toString})".updateAndReturnGeneratedKey().apply()
		}
	}

	override def getAnswerByQuestionId(questionId: Long): Option[String] = {
		DB readOnly { implicit session =>
			sql"SELECT answer_json FROM answer WHERE question_id = ${questionId}".map(rs => rs.string("answer_json")).single.apply()
		}
	}

	override def getAnswerIdByOutputCode(insertOutput: String): Option[Long] = {
		DB readOnly { implicit session =>
			sql"SELECT id FROM answer WHERE expected_output_code = ${insertOutput}".map(rs => rs.long("id")).single().apply()
		}
	}

	override def getAnswerById(id: Long): Option[Answer] = {
		DB readOnly { implicit session =>
			sql"SELECT * FROM answer WHERE id = ${id}".map(rs => Answer(rs.long("id"), rs.long("question_id"), rs.string("answer_json"), rs.boolean("accepted"))).single().apply()
		}
	}

	override def getExpectedOutputCodeFromAnswerId(ansId: Long): Option[Long] = {
		DB readOnly { implicit session =>
			sql"SELECT expected_output_code FROM answer WHERE id = ${ansId}".map(rs => rs.long("expected_output_code")).single().apply()
		}
	}

	override def createQuestion(html: String, batchId: Long, uuid: UUID = UUID.randomUUID(), dateTime: DateTime = new DateTime(), permutationId: Long): Long = {
		DB localTx { implicit session =>
			sql"INSERT INTO question(batch_id, html, create_time, uuid, permutation) VALUES(${batchId}, ${html}, ${dateTime}, ${uuid.toString}, ${permutationId})".updateAndReturnGeneratedKey().apply()
		}
	}

	override def getQuestionIdByUUID(uuid: String): Option[Long] = {
		DB readOnly { implicit session =>
			sql"SELECT id FROM question WHERE uuid = ${uuid}".map(rs => rs.long("id")).single().apply()
		}
	}

	override def getQuestionUUID(questionId: Long): Option[String] = {
		DB readOnly { implicit session =>
			sql"SELECT uuid FROM question WHERE id = ${questionId}".map(rs => rs.string("uuid")).single().apply()
		}
	}

	override def getBatchIdByUUID(uuid: UUID): Option[Long] = {
		DB readOnly { implicit session =>
			sql"SELECT id FROM batch WHERE uuid = ${uuid.toString}".map(rs => rs.long("id")).single().apply()
		}
	}

	override def createAsset(binary: Array[Byte], contentType: String, questionId: Long, filename: String): Long = {
		DB localTx { implicit session =>
			sql"INSERT INTO assets(byte_array, content_type, question_id, filename) VALUES(${binary}, ${contentType}, ${questionId}, ${filename})"
				.updateAndReturnGeneratedKey().apply()
		}
	}

	override def updateAnswer(answerId: Long, accepted: Boolean) = {
		DB localTx { implicit session =>
			sql"UPDATE answer SET accepted = ${accepted} WHERE id = ${answerId}"
				.update().apply()
		}
	}

	override def getAssetIdsByQuestionId(questionId: Long): List[Long] = {
		DB readOnly { implicit session =>
			sql"SELECT * FROM assets WHERE question_id = ${questionId}".map(rs => rs.long("id")).list().apply()
		}
	}

	def loadPermutationsCSV(csv: String): Boolean = {
		DB localTx { implicit session =>

			sql"SET FOREIGN_KEY_CHECKS = 0".update().apply()
			sql"TRUNCATE TABLE permutations".update().apply()
			sql"SET FOREIGN_KEY_CHECKS = 1".update().apply()

			sql"""LOAD DATA LOCAL INFILE ${csv}
      INTO TABLE permutations
      COLUMNS TERMINATED BY ','
      OPTIONALLY ENCLOSED BY '"'
      ESCAPED BY '"'
      LINES TERMINATED BY '\n'
      IGNORE 1 LINES
        (group_name, method_index, snippet_filename, pdf_path, method_on_top ,relative_height_top, relative_height_bottom)""".update().apply()
		}

		true
	}

	def createPermutation(permutation: Permutation): Long = {
		DB localTx { implicit session =>
			sql"""INSERT INTO permutations(group_name, method_index, snippet_filename, pdf_path, method_on_top, relative_height_top, relative_height_bottom)
      VALUES (group_name = ${permutation.groupName}, method_index = ${permutation.methodIndex},
      snippet_filename = ${permutation.snippetFilename}, pdf_path = ${permutation.pdfPath}, method_on_top = ${permutation.methodOnTop},
      relative_height_top = ${permutation.relativeHeightTop}, relative_height_bottom = ${permutation.relativeHeightBottom})"""
				.updateAndReturnGeneratedKey().apply()
		}
	}

	def getAllPermutations(): List[Permutation] = {
		DB readOnly { implicit session =>
			sql"SELECT * FROM permutations".map(rs =>
				Permutation(rs.long("id"), rs.string("group_name"), rs.string("method_index"), rs.string("snippet_filename"), rs.string("pdf_path"), rs.boolean("method_on_top"), rs.long("state"), rs.int("excluded_step"), rs.double("relative_height_top"), rs.double("relative_height_bottom"))
			).list().apply()
		}
	}

	def getPermutationById(id: Long): Option[Permutation] = {
		DB readOnly { implicit session =>
			sql"SELECT * FROM permutations WHERE id = ${id}".map(rs =>
				Permutation(rs.long("id"), rs.string("group_name"), rs.string("method_index"), rs.string("snippet_filename"), rs.string("pdf_path"), rs.boolean("method_on_top"), rs.long("state"), rs.int("excluded_step"), rs.double("relative_height_top"), rs.double("relative_height_bottom"))
			).single().apply()
		}
	}

	def getAllOpenByGroupName(groupName: String): List[Permutation] = {
		DB readOnly { implicit session =>
			sql"SELECT * FROM permutations WHERE group_name = ${groupName} AND state = 0".map(rs =>
				Permutation(rs.long("id"), rs.string("group_name"), rs.string("method_index"), rs.string("snippet_filename"), rs.string("pdf_path"), rs.boolean("method_on_top"), rs.long("state"), rs.int("excluded_step"), rs.double("relative_height_top"), rs.double("relative_height_bottom"))
			).list().apply()
		}
	}

	def updateStateOfPermutationId(id: Long, becauseOfId: Long, excludedByStep: Int = 0) {
		DB localTx { implicit session =>
			sql"UPDATE permutations SET state = ${becauseOfId}, excluded_step = ${excludedByStep} WHERE id = ${id}"
				.update().apply()
		}
	}

	def getAllOpenGroupsStartingWith(partialGroupName: String): List[Permutation] = {
		val result: List[Permutation] = getAllPermutationsWithStateEquals(0)
		result.filter(r => r.groupName.startsWith(partialGroupName)).map(m => m)
	}

	override def getAllQuestions: List[Question] = {
		DB readOnly { implicit session =>
			sql"SELECT * FROM question".map(rs => Question(rs.long("id"), rs.long("permutation"))).list().apply()
		}
	}

	override def getAssetFileNameByQuestionId(qId: Long): Option[String] = {
		DB readOnly { implicit session =>
			sql"SELECT filename FROM assets WHERE question_id = ${qId}".map(rs => rs.string("filename")).single().apply()
		}
	}

	def getAllPermutationsWithStateEquals(state: Long): List[Permutation] = {
		DB readOnly { implicit session =>
			sql"SELECT * FROM permutations WHERE state = ${state}".map(rs =>
				Permutation(rs.long("id"), rs.string("group_name"), rs.string("method_index"), rs.string("snippet_filename"), rs.string("pdf_path"), rs.boolean("method_on_top"), rs.long("state"), rs.int("excluded_step"), rs.double("relative_height_top"), rs.double("relative_height_bottom"))).list().apply()
		}
	}

	override def allAnswers: List[Answer] = {
		DB readOnly { implicit session =>
			sql"SELECT * FROM answer WHERE accepted = 1".map(rs =>
				Answer(rs.long("id"), rs.long("question_id"), rs.string("answer_json"), rs.boolean("accepted"))
			).list().apply()
		}
	}

	def getPermutationIdByQuestionId(qId: Long): Option[Long] = {
		DB readOnly { implicit session =>
			sql"SELECT permutation FROM question WHERE id = ${qId}".map(rs =>
				rs.long("permutation")).single().apply()
		}
	}

	def getAllAnswersBySnippet(fileName: String): List[Answer] = {
		allAnswers.filter(f => f.answerJson.contains(fileName))
	}

}
