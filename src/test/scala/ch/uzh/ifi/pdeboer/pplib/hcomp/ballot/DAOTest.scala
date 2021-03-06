package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao.DAO
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.{Answer, Permutation, Question}
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import org.joda.time.DateTime

/**
  * Created by mattia on 15.09.15.
  */
class DAOTest extends DAO with LazyLogger {

	var assets = List.empty[(Long, Long)]
	var questionToPermutationId = List.empty[(Long, Long)]
	var assetsIdWithFilename = List.empty[(Long, String)]
	var assetsIdWithContentType = List.empty[(Long, String)]
	var assetsHashCodeToassetId = List.empty[(String, Long)]
	var batches = List.empty[(Long, String)]
	var questions = List.empty[(Long, String)]
	var question2assets = List.empty[(Long, Long)]
	var answers = List.empty[(Long, String)]
	var permutations = List.empty[Permutation]

	override def createBatch(allowedAnswerPerTurker: Int, uuid: UUID): Long = {
		logger.debug("Adding new Batch: " + uuid)
		batches = batches ::: List[(Long, String)]((batches.size + 1).toLong -> uuid.toString)
		batches.size.toLong
	}

	override def getAnswerByQuestionId(questionId: Long): Option[String] = {
		Some(answers.find(a => a._1 == questionId).get._2)
	}

	override def getBatchIdByUUID(uuid: UUID): Option[Long] = {
		batches.foreach(b => {
			if (b._2.equals(uuid)) {
				logger.debug("Found batch by UUID: " + b._1)
				return Some(b._1)
			}
		})
		None
	}

	override def getQuestionUUID(questionId: Long): Option[String] = {
		Some(questions.find(q => q._1 == questionId).get._2)
	}

	override def createQuestion(html: String, batchId: Long, uuid: UUID = UUID.randomUUID(), dateTime: DateTime = new DateTime(), permutationId: Long, secret: String): Long = {
		questions = questions ::: List[(Long, String)]((questions.size + 1).toLong -> UUID.randomUUID().toString)
		questionToPermutationId = questionToPermutationId ::: List[(Long, Long)](questions.size.toLong -> permutationId)
		answers = answers ::: List[(Long, String)](questions.size.toLong -> "{\"confidence\":\"7\", \"isRelated\":\"yes\", \"isCheckedBefore\":\"yes\", \"descriptionIsRelated\":\"test\", \"answer\":\"yes\"}")
		questions.size.toLong
	}

	override def getAssetIdsByQuestionId(questionId: Long): List[Long] = {
		question2assets.filter(b => b._1 == questionId).map(_._2)
	}

	override def createAsset(binary: Array[Byte], contentType: String, filename: String): Long = {
		val hashCode: String = java.security.MessageDigest.getInstance("SHA-1").digest(binary).map("%02x".format(_)).mkString

		val possibleMatch = findAssetsIdByHashCode(hashCode).map(id => id -> getAssetsContentById(id))
				.find(p => p._2.equalsIgnoreCase(contentType))

		val assetId = if (possibleMatch.nonEmpty) {
			possibleMatch.get._1
		} else {
			assets = assets ::: List[(Long, Long)](assets.size + 1.toLong -> 0)
			assetsIdWithFilename = assetsIdWithFilename ::: List[(Long, String)](assets.size.toLong -> filename)
			assetsIdWithContentType = assetsIdWithContentType ::: List[(Long, String)](assets.size.toLong -> contentType)
			assetsHashCodeToassetId = assetsHashCodeToassetId ::: List[(String, Long)](hashCode -> assets.size.toLong)
			assets.size.toLong
		}
		assetId
	}

	override def updateAnswer(answerId: Long, accepted: Boolean): Unit = true


	override def getAnswerIdByOutputCode(insertOutput: String): Option[Long] = {
		Some(answers.head._1)
	}

	override def getExpectedOutputCodeFromAnswerId(ansId: Long): Option[Long] = {
		Some(123)
	}

	override def getQuestionIdByUUID(uuid: String): Option[Long] = {
		Some(questions.find(p => p._2.equalsIgnoreCase(uuid)).get._1)
	}

	override def countAllAnswers(): Int = answers.size

	override def allAnswers(): List[Answer] = {
		answers.map(ans => {
			Answer(ans._1, new DateTime(), ans._1, ans._2.substring(0, ans._2.length - 1) + ", \"pdfFileName\":\"" + getAssetPDFFileNameByQuestionId(ans._1).get + "\"}", accepted = true)
		})
	}

	override def countAllBatches(): Int = batches.size

	override def countAllQuestions(): Int = questions.size

	override def getAllQuestions: List[Question] = questions.map(q => {
		Question(q._1, 1)
	}).toList

	override def getAnswerById(id: Long): Option[Answer] = {
		Some(Answer(id, new DateTime(), 0, answers.find(_._1 == id).get._2, true))
	}

	override def mapQuestionToAssets(qId: Long, assetId: Long): Long = {
		question2assets = question2assets ::: List[(Long, Long)](qId -> assetId)
		question2assets.size
	}

	override def createPermutation(permutation: Permutation): Long = {
		permutations = permutations ::: List[Permutation](permutation)
		permutations.size
	}

	override def getAllOpenGroupsStartingWith(partialGroupName: String): List[Permutation] = {
		permutations.filter(p => {
			p.groupName.startsWith(partialGroupName) && p.state == 0
		})
	}

	override def getAllPermutationsWithStateEquals(state: Long): List[Permutation] = {
		permutations.filter(p => {
			p.state == state
		})
	}

	override def getPermutationById(id: Long): Option[Permutation] = {
		permutations.find(x => x.id == id)
	}

	override def loadPermutationsCSV(csv: String): Boolean = true

	override def findAssetsIdByHashCode(hc: String): List[Long] = {
		assetsHashCodeToassetId.filter(_._1.equalsIgnoreCase(hc)).map(_._2)
	}

	override def getPermutationIdByQuestionId(qId: Long): Option[Long] = {
		Some(questionToPermutationId.find(_._1 == qId).get._2)
	}

	override def getAssetsContentById(id: Long): String = {
		val filename = assetsIdWithFilename.find(_._1 == id).map(_._2)
		if (filename.get.endsWith(".png")) {
			"image/png"
		} else if (filename.get.endsWith(".pdf")) {
			"application/pdf"
		} else if (filename.get.endsWith(".js")) {
			"application/javascript"
		} else {
			""
		}
	}

	override def getAllAnswersForSnippet(fileName: String): List[Answer] = {
		allAnswers().filter(_.answerJson.contains(fileName))
	}

	override def getAllPermutations(): List[Permutation] = {
		permutations
	}

	override def updateStateOfPermutationId(id: Long, becauseOfId: Long, excludedByStep: Int): Unit = {
		println(s"Updating permutation: $id, state: $becauseOfId, excluded_step(0/1/2): $excludedByStep")
		permutations = permutations.map(p => {
			if (p.id == id && p.state == 0 && p.excluded_step == 0) {
				Permutation(p.id, p.groupName, p.methodIndex, p.snippetFilename, p.pdfPath, p.methodOnTop, becauseOfId, excludedByStep, p.relativeHeightTop, p.relativeHeightBottom, p.distanceMinIndexMax)
			} else {
				p
			}
		})
	}

	override def getAllOpenByGroupName(groupName: String): List[Permutation] = {
		permutations.filter(p => p.state == 0 && p.groupName.equalsIgnoreCase(groupName))
	}

	override def getQuestionIDsAnsweredSince(date: DateTime): List[Long] = Nil
}