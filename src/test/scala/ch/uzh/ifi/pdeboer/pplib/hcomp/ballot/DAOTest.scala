package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao.DAO
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.{Answer, Permutation, Question}
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import org.joda.time.DateTime

import scala.collection.mutable

/**
 * Created by mattia on 15.09.15.
 */
class DAOTest extends DAO with LazyLogger {

  val assets = new mutable.HashMap[Long, Long]
  val questionToPermutationId = new mutable.HashMap[Long, Long]
  var assetsIdWithFilename = List.empty[(Long, String)]
  var assetsIdWithContentType = List.empty[(Long, String)]
  var assetsHashCodeToassetId = List.empty[(String, Long)]
  val batches = new mutable.HashMap[Long, String]
  var questions = new mutable.HashMap[Long, String]
  var question2assets = List.empty[(Long, Long)]
  var answers = List.empty[(Long, String)]
  var permutations = List.empty[Permutation]

  override def createBatch(allowedAnswerPerTurker: Int, uuid: UUID): Long = {
    logger.debug("Adding new Batch: " + uuid)
    batches += ((batches.size + 1).toLong -> uuid.toString)
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
    questions.get(questionId)
  }

  override def createQuestion(html: String, batchId: Long, uuid: UUID = UUID.randomUUID(), dateTime: DateTime = new DateTime(), permutationId: Long): Long = {
    this.synchronized{
      questions += ((questions.size + 1).toLong -> UUID.randomUUID().toString)
      questionToPermutationId += (questions.size.toLong -> permutationId)
      answers = answers ::: List[(Long, String)]((questions.size.toLong -> "{\"confidence\":\"7\", \"isRelated\":\"yes\", \"isCheckedBefore\":\"yes\", \"descriptionIsRelated\":\"test\", \"answer\":\"yes\"}"))
      questions.size.toLong
    }
  }

  override def getAssetIdsByQuestionId(questionId: Long): List[Long] = {
    question2assets.filter(b => b._1 == questionId).map(_._2)
  }

  override def createAsset(binary: Array[Byte], contentType: String, questionId: Long, filename: String): Long = {

    this.synchronized {
      val hashCode : String = java.security.MessageDigest.getInstance("SHA-1").digest(binary).map("%02x".format(_)).mkString

      val possibleMatch = findAssetsIdByHashCode(hashCode).map(id => id -> getAssetsContentById(id))
        .find(p => p._2.equalsIgnoreCase(contentType))

      val assetId = if (possibleMatch.nonEmpty) {
        possibleMatch.get._1
      } else {
        assets += ((assets.size + 1).toLong -> questionId)
        assetsIdWithFilename = assetsIdWithFilename ::: List[(Long, String)](assets.size.toLong -> filename)
        assetsIdWithContentType = assetsIdWithContentType ::: List[(Long, String)](assets.size.toLong -> contentType)
        assetsHashCodeToassetId = assetsHashCodeToassetId ::: List[(String, Long)](hashCode -> assets.size.toLong)
        assets.size.toLong
      }

      mapQuestionToAssets(questionId, assetId)
      assetId
    }
  }

  override def updateAnswer(answerId: Long, accepted: Boolean): Unit = {
    true
  }


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
      Answer(ans._1, ans._1, ans._2.substring(0, ans._2.length-1)+", \"pdfFileName\":\""+getAssetPDFFileNameByQuestionId(ans._1).get+"\"}", true)
    })
  }

  override def countAllBatches(): Int = batches.size

  override def countAllQuestions(): Int = questions.size

  override def getAllQuestions: List[Question] = questions.map(q => {
    Question(q._1, 1)
  }).toList

  override def getAssetPDFFileNameByQuestionId(qId: Long): Option[String] = {
    val assetIds = getAssetIdsByQuestionId(qId)
    val assets = assetsIdWithFilename.filter(a => assetIds.contains(a._1)).map(_._2)
    assets.find(_.endsWith(".pdf"))
  }

  override def getAnswerById(id: Long): Option[Answer] = {
    Some(Answer(id, 0, answers.find(_._1==id).get._2, true))
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
      p.groupName.startsWith(partialGroupName) && p.state==0
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

  override def getPermutationIdByQuestionId(qId: Long): Option[Long] = questionToPermutationId.get(qId)

  override def getAssetsContentById(id: Long): String = {
    val filename = assetsIdWithFilename.find(_._1 == id).map(_._2)
    if(filename.get.endsWith(".png")){
      "image/png"
    }else if(filename.get.endsWith(".pdf")) {
      "application/pdf"
    }else if(filename.get.endsWith(".js")) {
      "application/javascript"
    }else {
      ""
    }
  }

  override def getAllAnswersBySnippet(fileName: String): List[Answer] = {
    allAnswers().filter(_.answerJson.contains(fileName))
  }

  override def getAllPermutations(): List[Permutation] = {
    permutations
  }

  override def updateStateOfPermutationId(id: Long, becauseOfId: Long, excludedByStep: Int): Unit = {
    println(s"Updating permutation: $id, state: $becauseOfId, excluded_step(0/1/2): $excludedByStep")
    permutations = permutations.map(p => {
      if(p.id == id && p.state == 0 && p.excluded_step == 0){
        Permutation(p.id, p.groupName, p.methodIndex, p.snippetFilename, p.pdfPath, p.methodOnTop, becauseOfId, excludedByStep, p.relativeHeightTop, p.relativeHeightBottom)
      }else {
        Permutation(p.id, p.groupName, p.methodIndex, p.snippetFilename, p.pdfPath, p.methodOnTop, p.state, p.excluded_step, p.relativeHeightTop, p.relativeHeightBottom)
      }
    })
  }

  override def getAllOpenByGroupName(groupName: String): List[Permutation] = {
    permutations.filter(p => p.state == 0 && p.groupName.equalsIgnoreCase(groupName))
  }
}