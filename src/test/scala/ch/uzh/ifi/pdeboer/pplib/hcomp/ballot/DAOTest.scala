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
  val assetsIdWithFilename = new mutable.HashMap[Long, String]
  val batches = new mutable.HashMap[Long, String]
  val questions = new mutable.HashMap[Long, String]
  val answers = new mutable.HashMap[Long, String]
  var permutations = List.empty[Permutation]

  override def createBatch(allowedAnswerPerTurker: Int, uuid: UUID): Long = {
    logger.debug("Adding new Batch: " + uuid)
    batches += ((batches.size + 1).toLong -> uuid.toString)
    batches.size.toLong
  }

  override def getAnswerByQuestionId(questionId: Long): Option[String] = {
    answers.get(questionId)
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
    questions += ((questions.size + 1).toLong -> UUID.randomUUID().toString)
    questionToPermutationId += (questions.size.toLong -> permutationId)
    answers += (questions.size.toLong -> "{\"confidence\":\"7\", \"isRelated\":\"yes\", \"isCheckedBefore\":\"yes\", \"descriptionIsRelated\":\"test\", \"answer\":\"yes\"}")
    questions.size.toLong
  }

  override def getAssetIdsByQuestionId(questionId: Long): List[Long] = {
    var res = List.empty[Long]
    assets.foreach(b => {
      if (b._2 == questionId) {
        logger.debug("Found assetId : " + b._1)
        res ::= b._1
      }
    })
    res
  }

  override def createAsset(binary: Array[Byte], contentType: String, questionId: Long, filename: String): Long = {
    assets += ((assets.size + 1).toLong -> questionId)
    assetsIdWithFilename += ((assetsIdWithFilename.size +1).toLong -> filename)
    assets.size.toLong
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
    Some(1)
  }

  override def countAllAnswers(): Int = answers.size

  override def allAnswers(): List[Answer] = answers.zipWithIndex.map(ans => {
    val filename = getAssetFileNameByQuestionId(ans._1._1)
    Answer(ans._2, ans._1._1, ans._1._2.substring(0,ans._1._2.length-1)+",\"pdfFileName\":\""+filename+"\"}", true)
  }).toList

  override def countAllBatches(): Int = batches.size

  override def countAllQuestions(): Int = questions.size

  override def getAllQuestions: List[Question] = questions.map(q => {
    Question(q._1, 1)
  }).toList

  override def getAssetFileNameByQuestionId(qId: Long): Option[String] = {
    val assetId = getAssetIdsByQuestionId(qId)
    Some(assetsIdWithFilename.filter(id => assetId.contains(id._1) && id._2.endsWith(".pdf")).head._2)
  }

  override def getAnswerById(id: Long): Option[Answer] = {
    Some(Answer(id, 0, answers.getOrElse(id, ""), true))
  }

  override def mapQuestionToAssets(qId: Long, assetId: Long): Long = ???

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

  override def findAssetsIdByHashCode(hashCode: String): List[Long] = List[Long](1)

  override def getPermutationIdByQuestionId(qId: Long): Option[Long] = questionToPermutationId.get(qId)

  override def getAssetsContentById(id: Long): String = "application/test"

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