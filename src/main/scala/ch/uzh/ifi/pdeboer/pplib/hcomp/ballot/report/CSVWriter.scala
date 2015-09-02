package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.report

import java.io.{File, PrintWriter}

import com.typesafe.config.ConfigFactory

/**
 * Created by mattia on 01.09.15.
 */
object CSVWriter {

	val config = ConfigFactory.load()
	val RESULT_CSV_FILENAME = config.getString("resultFilename")

	//TODO I recommend using a CSV writer for this. Then you don't need to worry about escaping etc
	val writer = new PrintWriter(new File(RESULT_CSV_FILENAME))

	def init() = {
		writer.write("snippet,yes answers,no answers,cleaned yes,cleaned no,yes answers,no answers,cleaned yes,cleaned no,feedbacks,firstExclusion,secondExclusion\n")
	}

	def appendResult(snippetName: String, overallSummary: SummarizedAnswersFormat, cleanedSummary: SummarizedAnswersFormat,
					 feedback: String, firstExcluded: String, secondExcluded: String) = {
		writer.append(snippetName + "," + overallSummary.yesQ1 + "," + overallSummary.noQ1 + "," + cleanedSummary.yesQ1 + "," +
			cleanedSummary.noQ1 + "," + overallSummary.yesQ2 + "," + overallSummary.noQ2 + "," + cleanedSummary.yesQ2 + "," +
			cleanedSummary.noQ2 + "," + feedback + "," + firstExcluded + "," + secondExcluded + "\n")
	}

	def close() = {
		writer.close()
	}
}
