package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.report

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao.BallotDAO
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.Answer
import com.typesafe.config.ConfigFactory

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

			val permutationId = dao.getPermutationIdByQuestionId(answersForSnippet._2.head.questionId).get
			val allPermutationsDisabledByActualAnswer = dao.getAllPermutationsWithStateEquals(permutationId).filterNot(_.excluded_step == 0)
			val snippetName = answersForSnippet._1
			val allAnswerOfSnippet: List[Answer] = dao.getAllAnswersBySnippet(snippetName)

			val allAnswersParsed: List[ParsedAnswer] = AnswerParser.parseJSONAnswers(allAnswerOfSnippet)

			val overallSummary = SummarizedAnswersFormat.summarizeAnswers(allAnswersParsed)

			val cleanedAnswers = allAnswersParsed.filter(_.likert >= LIKERT_VALUE_CLEANED_ANSWERS)
			val cleanedSummary = SummarizedAnswersFormat.summarizeAnswers(cleanedAnswers)

			val feedback = allAnswersParsed.map(_.feedback).mkString(";")

			val firstExclusion = allPermutationsDisabledByActualAnswer.filter(_.excluded_step == 1).map(_.snippetFilename).mkString(";")
			val secondExclusion = allPermutationsDisabledByActualAnswer.filter(_.excluded_step == 2).map(_.snippetFilename).mkString(";")

			csvWriter.appendResult(snippetName, overallSummary, cleanedSummary, feedback, firstExclusion, secondExclusion)
		})

		csvWriter.close()
	}

}
