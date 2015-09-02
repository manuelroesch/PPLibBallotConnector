package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.report


/**
 * Created by mattia on 01.09.15.
 */
case class SummarizedAnswersFormat(yesQ1: Int, noQ1: Int, yesQ2: Int, noQ2: Int)

object SummarizedAnswersFormat {
  def summarizeAnswers(answers: List[ParsedAnswer]) : SummarizedAnswersFormat = {
    val yesQ1 = answers.count(ans => AnswerParser.evaluateAnswer(ans.q1).get == true)
    val yesQ2 = answers.count(ans => AnswerParser.evaluateAnswer(ans.q2).isDefined && AnswerParser.evaluateAnswer(ans.q2).get == true)

    val noQ1 = answers.count(ans => AnswerParser.evaluateAnswer(ans.q1).get == false)
    val noQ2 = answers.count(ans => AnswerParser.evaluateAnswer(ans.q2).isDefined && AnswerParser.evaluateAnswer(ans.q2).get == false)
    SummarizedAnswersFormat(yesQ1, noQ1, yesQ2, noQ2)
  }
}