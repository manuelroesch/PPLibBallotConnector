package ch.uzh.ifi.pplibballotconnector.hcomp.pplibballotconnector

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pplibballotconnector.dao.{BallotDAO, DAO}
import ch.uzh.ifi.pplibballotconnector.hcomp.{BallotAnswer, BallotProperties, BallotQuery}
import org.joda.time.DateTime
import play.api.libs.json.Json

import scala.collection.parallel.mutable
import scala.util.Random
import scala.util.parsing.json.{JSON, JSONObject}

/**
 * Created by mattia on 06.07.15.
 */
@HCompPortal(builder = classOf[BallotPortalBuilder], autoInit = true)
class BallotPortalAdapter(val decorated: HCompPortalAdapter with AnswerRejection, val dao: DAO = new BallotDAO()) extends HCompPortalAdapter {

  override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {

    val outputCode = new Random(new DateTime().getMillis).nextLong()

    val batchId = properties match {
      case p: BallotProperties => {
        dao.getBatchIdByUUID(p.getBatch().uuid.toString).map { id => id }
          .getOrElse {
          dao.createBatch(p.getAllowedAnswersPerTurker(), UUID.randomUUID().toString)
        }
      }
      case _ => dao.createBatch(0, UUID.randomUUID().toString)
    }

    val html = query.question

    val questionId = dao.createQuestion(html, outputCode, batchId)

    val link = "http://andreas.ifi.uzh.ch:9000/showQuestion/".concat(dao.getQuestionUUID(questionId).getOrElse("-1"))

    val ans = decorated.sendQueryAndAwaitResult(FreetextQuery(link + "<br> click the link and enter here the code when you are finish:<br> <input type=\"text\" value=\"123\">"), properties)
      .get.asInstanceOf[FreetextAnswer]

    if(ans.answer.equals(outputCode+"")){
      val answerJson = dao.getAnswer(questionId).get
      val answer = JSON.parseFull(answerJson).asInstanceOf[Map[String, String]]

      decorated.approveAndBonusAnswer(ans)
      Some(BallotAnswer(answer, BallotQuery(xml.XML.loadString(html))))
    } else {
      decorated.rejectAnswer(ans, "Invalid code")
      processQuery(query, properties)
    }

  }

  override def getDefaultPortalKey: String = decorated.getDefaultPortalKey

  override def cancelQuery(query: HCompQuery): Unit = decorated.cancelQuery(query)

}

object BallotPortalAdapter {
  val CONFIG_ACCESS_ID_KEY = "hcomp.ballot.accessKeyID"
  val CONFIG_SECRET_ACCESS_KEY = "hcomp.ballot.secretAccessKey"
  val CONFIG_SANDBOX_KEY = "hcomp.ballot.sandbox"
  val PORTAL_KEY = "ballot"
}

class BallotPortalBuilder extends HCompPortalBuilder {

  val ACCESS_ID_KEY: String = "accessKeyID"
  val SECRET_ACCESS_KEY: String = "secretAccessKey"
  val SANDBOX: String = "sandbox"

  //FIXME: create new instance of ballotPortalAdapter
  override def build: HCompPortalAdapter = new BallotPortalAdapter(???)

  override def expectedParameters: List[String] = List(ACCESS_ID_KEY, SECRET_ACCESS_KEY)

  override def parameterToConfigPath: Map[String, String] = Map(
    ACCESS_ID_KEY -> BallotPortalAdapter.CONFIG_ACCESS_ID_KEY,
    SECRET_ACCESS_KEY -> BallotPortalAdapter.CONFIG_SECRET_ACCESS_KEY,
    SANDBOX -> BallotPortalAdapter.CONFIG_SANDBOX_KEY
  )
}