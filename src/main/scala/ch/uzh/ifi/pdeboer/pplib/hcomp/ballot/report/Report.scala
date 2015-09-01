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

    val csvWriter = CSVWriter
    csvWriter.init()

    dao.allAnswers.groupBy(g => {
      dao.getAssetFileNameByQuestionId(g.questionId).get
    }).map(answersForSnippet => {

      val hints = dao.getPermutationIdByQuestionId(answersForSnippet._2.head.questionId).get
      val allPermutationsDisabledByActualAnswer = dao.getAllPermutationsWithStateEquals(hints).filterNot(f => f.excluded_step == 0)
      val snippetName = answersForSnippet._1
      val allAnswerOfSnippet: List[Answer] = dao.getAllAnswersBySnippet(snippetName)

      val cleanedFormatAnswers: List[Map[String, String]] = allAnswerOfSnippet.map(singleAnswerOfSnippet => {
        Json.parse(singleAnswerOfSnippet.answerJson).as[JsObject].fields.map(field => field._1 -> field._2.toString().replaceAll("\"", "")).toMap
      })

      val allAnswers: List[ParsedAnswer] = AnswerParser.parseAnswers(cleanedFormatAnswers)
      val overallSummary = SummarizedAnswersFormat.count(allAnswers)

      val cleanedAnswers = allAnswers.filter(_.likert >= LIKERT_VALUE_CLEANED_ANSWERS)
      val cleanedSummary = SummarizedAnswersFormat.count(cleanedAnswers)

      val feedback = allAnswers.map(_.feedback).mkString(";")

      val firstExcluded = allPermutationsDisabledByActualAnswer.filter(f => f.excluded_step == 1).map(_.snippetFilename).mkString(";")
      val secondExcluded = allPermutationsDisabledByActualAnswer.filter(f => f.excluded_step == 2).map(_.snippetFilename).mkString(";")

      csvWriter.addResult(snippetName, overallSummary, cleanedSummary, feedback, firstExcluded, secondExcluded)
    })

    csvWriter.close()
  }

}
