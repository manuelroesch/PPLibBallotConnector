package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.report

import java.io.{File, PrintWriter}

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao.BallotDAO
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.Answer
import com.typesafe.config.ConfigFactory
import play.api.libs.json.{JsObject, Json}

/**
 * Created by mattia on 31.08.15.
 */
object Report {

  val config = ConfigFactory.load()
  val RESULT_CSV_FILENAME = config.getString("resultFilename")
  val LIKERT_VALUE_CLEANED_ANSWERS = config.getInt("likertCleanedAnswers")

  def writeCSVReport(dao: BallotDAO) = {
    val writer = new PrintWriter(new File(RESULT_CSV_FILENAME))

    writer.write("snippet,yes answers,no answers,cleaned yes,cleaned no,yes answers,no answers,cleaned yes,cleaned no,feedbacks,firstExclusion,secondExclusion\n")

    val results = dao.allAnswers.groupBy(g => {
      dao.getAssetFileNameByQuestionId(g.questionId).get
    }).map(answersForSnippet => {

      val hints = dao.getPermutationIdByQuestionId(answersForSnippet._2.head.questionId).get
      val allPermutationsDisabledByActualAnswer = dao.getAllPermutationsWithStateEquals(hints).filterNot(f => f.excluded_step == 0)
      val snippetName = answersForSnippet._1
      val aa: List[Answer] = dao.getAllAnswersBySnippet(snippetName)

      val cleanFormatAnswers: List[Map[String, String]] = aa.map(a => Json.parse(a.answerJson).as[JsObject].fields.map(field => field._1 -> field._2.toString().replaceAll("\"", "")).toMap)
      val answers: List[CsvAnswer] = convertToCSVFormat(cleanFormatAnswers)

      val yesQ1 = answers.count(ans => isPositive(ans.q1).get)
      val yesQ2 = answers.count(ans => isPositive(ans.q2).isDefined && isPositive(ans.q2).get)
      val noQ1 = answers.count(ans => isNegative(ans.q1).get)
      val noQ2 = answers.count(ans => isNegative(ans.q2).isDefined && isNegative(ans.q2).get)

      val cleanedYesQ1 = answers.count(ans => ans.likert >= LIKERT_VALUE_CLEANED_ANSWERS && isPositive(ans.q1).get)
      val cleanedYesQ2 = answers.count(ans => ans.likert >= LIKERT_VALUE_CLEANED_ANSWERS && isPositive(ans.q2).isDefined && isPositive(ans.q2).get)
      val cleanedNoQ1 = answers.count(ans => ans.likert >= LIKERT_VALUE_CLEANED_ANSWERS && isNegative(ans.q1).get)
      val cleanedNoQ2 = answers.count(ans => ans.likert >= LIKERT_VALUE_CLEANED_ANSWERS && isNegative(ans.q2).isDefined && isNegative(ans.q2).get)

      val feedbacks = answers.map(_.feedback).mkString(";")

      val firstExcluded = allPermutationsDisabledByActualAnswer.filter(f => f.excluded_step == 1).map(_.snippetFilename).mkString(";")
      val secondExcluded = allPermutationsDisabledByActualAnswer.filter(f => f.excluded_step == 2).map(_.snippetFilename).mkString(";")

      snippetName + "," + yesQ1 + "," + noQ1 + "," + cleanedYesQ1 + "," + cleanedNoQ1 + "," + yesQ2 + "," + noQ2 + "," + cleanedYesQ2 + "," + cleanedNoQ2 + "," + feedbacks + "," + firstExcluded + "," + secondExcluded

    })

    writer.append(results.mkString("\n"))
    writer.close()
  }

  def isPositive(answer: Option[String]): Option[Boolean] = {
    if (answer.isDefined) {
      Some(answer.get.equalsIgnoreCase("yes"))
    } else {
      None
    }
  }

  def isNegative(answer: Option[String]): Option[Boolean] = {
    if (answer.isDefined) {
      Some(answer.get.equalsIgnoreCase("no"))
    } else {
      None
    }
  }

  def convertToCSVFormat(answers: List[Map[String, String]]): List[CsvAnswer] = {
    answers.map(ans => {
      val isRelated = ans.get("isRelated")
      val isCheckedBefore = ans.get("isCheckedBefore")
      val likert = ans.get("confidence")
      val descriptionIsRelated = ans.get("descriptionIsRelated")

      CsvAnswer(isRelated, isCheckedBefore, likert.get.toInt, descriptionIsRelated.get)
    })
  }
}
