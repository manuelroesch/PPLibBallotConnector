package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.report

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao.BallotDAO
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.Answer
import com.typesafe.config.ConfigFactory
import play.api.libs.json.{JsObject, Json}

/**
 * Created by mattia on 31.08.15.
 */
object Report {

  val config = ConfigFactory.load()
  val LIKERT_VALUE_CLEANED_ANSWERS = config.getInt("likertCleanedAnswers")

  def writeCSVReport(dao: BallotDAO) = {

    CSVWriter.init()

    dao.allAnswers.groupBy(g => {
      dao.getAssetFileNameByQuestionId(g.questionId).get
    }).map(answersForSnippet => {

      val hints = dao.getPermutationIdByQuestionId(answersForSnippet._2.head.questionId).get
      val allPermutationsDisabledByActualAnswer = dao.getAllPermutationsWithStateEquals(hints).filterNot(f => f.excluded_step == 0)
      val snippetName = answersForSnippet._1
      val allAnswerOfSnippet: List[Answer] = dao.getAllAnswersBySnippet(snippetName)

      val cleanFormatAnswers: List[Map[String, String]] = allAnswerOfSnippet.map(singleAnswerOfSnippet => {
        Json.parse(singleAnswerOfSnippet.answerJson).as[JsObject].fields.map(field => field._1 -> field._2.toString().replaceAll("\"", "")).toMap
      })
      val answers: List[ParsedAnswer] = convertToCSVFormat(cleanFormatAnswers)

      val yesQ1 = answers.count(ans => ans.isPositive(ans.q1).get).toString
      val yesQ2 = answers.count(ans => ans.isPositive(ans.q2).isDefined && ans.isPositive(ans.q2).get).toString
      val noQ1 = answers.count(ans => ans.isNegative(ans.q1).get).toString
      val noQ2 = answers.count(ans => ans.isNegative(ans.q2).isDefined && ans.isNegative(ans.q2).get).toString

      val cleanedAnswers = answers.filter(_.likert >= LIKERT_VALUE_CLEANED_ANSWERS)

      val cleanedYesQ1 = cleanedAnswers.count(ans => ans.isPositive(ans.q1).get).toString
      val cleanedYesQ2 = cleanedAnswers.count(ans => ans.isPositive(ans.q2).isDefined && ans.isPositive(ans.q2).get).toString
      val cleanedNoQ1 = cleanedAnswers.count(ans => ans.isNegative(ans.q1).get).toString
      val cleanedNoQ2 = cleanedAnswers.count(ans => ans.isNegative(ans.q2).isDefined && ans.isNegative(ans.q2).get).toString

      val feedbacks = answers.map(_.feedback).mkString(";")

      val firstExcluded = allPermutationsDisabledByActualAnswer.filter(f => f.excluded_step == 1).map(_.snippetFilename).mkString(";")
      val secondExcluded = allPermutationsDisabledByActualAnswer.filter(f => f.excluded_step == 2).map(_.snippetFilename).mkString(";")

      CSVWriter.addResult(snippetName, yesQ1, noQ1, cleanedYesQ1, cleanedNoQ1, yesQ2, noQ2, cleanedYesQ2, cleanedNoQ2, feedbacks, firstExcluded, secondExcluded)
    })

    CSVWriter.close()
  }


  def convertToCSVFormat(answers: List[Map[String, String]]): List[ParsedAnswer] = {
    answers.map(ans => {
      val isRelated = ans.get("isRelated")
      val isCheckedBefore = ans.get("isCheckedBefore")
      val likert = ans.get("confidence")
      val descriptionIsRelated = ans.get("descriptionIsRelated")

      ParsedAnswer(isRelated, isCheckedBefore, likert.get.toInt, descriptionIsRelated.get)
    })
  }
}
