package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.report

import java.io.{File, PrintWriter}

import com.typesafe.config.ConfigFactory

/**
 * Created by mattia on 01.09.15.
 */
object CSVWriter {

  val config = ConfigFactory.load()
  val RESULT_CSV_FILENAME = config.getString("resultFilename")

  val writer = new PrintWriter(new File(RESULT_CSV_FILENAME))

  def init() = {
    writer.write("snippet,yes answers,no answers,cleaned yes,cleaned no,yes answers,no answers,cleaned yes,cleaned no,feedbacks,firstExclusion,secondExclusion\n")
  }

  def addResult(result: String*) = {
    writer.append(result.mkString(",")+"\n")
  }

  def close() = {
    writer.close()
  }
}
