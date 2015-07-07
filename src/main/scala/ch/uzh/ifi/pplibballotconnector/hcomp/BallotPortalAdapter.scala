package ch.uzh.ifi.pplibballotconnector.hcomp.pplibballotconnector

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pplibballotconnector.dao.{BallotDAO, DAO}
import ch.uzh.ifi.pplibballotconnector.hcomp.{BallotAnswer, BallotProperties, BallotQuery}
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import play.api.libs.json.Json

import scala.collection.parallel.mutable
import scala.util.Random
import scala.util.parsing.json.{JSON, JSONObject}

/**
 * Created by mattia on 06.07.15.
 */
@HCompPortal(builder = classOf[BallotPortalBuilder], autoInit = true)
class BallotPortalAdapter(val decorated: HCompPortalAdapter with AnswerRejection, val dao: DAO = new BallotDAO(),
                          val baseFormUrl: String = ConfigFactory.load().getString("defaultFormUrl")) extends HCompPortalAdapter {

  // TODO: do not store this here
  var counter = 10

  override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {

    val batchId = properties match {
      case p: BallotProperties => {
        dao.getBatchIdByUUID(p.getBatch().uuid.toString).map { id => id }
          .getOrElse {
          dao.createBatch(p.getAllowedAnswersPerTurker(), UUID.randomUUID().toString)
        }
      }
      case _ => dao.createBatch(0, UUID.randomUUID().toString)
    }

    val outputCode = properties match {
      case p: BallotProperties => {
        p.getOutputCode()
      }
      case _ => new Random(new DateTime().getMillis).nextLong()
    }

    val html = query.question

    //TODO: find form action in html and rewrite it to match the correct endpoint
    val formLabel = (xml.XML.loadString(html) \ "form")
    if(formLabel.length>0){
      if(formLabel(0).attribute("action").isDefined){
        if(!formLabel(0).attribute("action").get.text.equalsIgnoreCase(baseFormUrl)){
          return None
        }
      }else {
        return None
      }
    }else {
      return None
    }



    //TODO: check if form is valid (contains inputs/select/textarea/buttons/...)

    val questionId = dao.createQuestion(html, outputCode, batchId)

    val link = "http://andreas.ifi.uzh.ch:9000/showQuestion/".concat(dao.getQuestionUUID(questionId).getOrElse("-1"))

    val ans = decorated.sendQueryAndAwaitResult(FreetextQuery(link + "<br> click the link and enter here the code when you are finish:<br> <input type=\"text\" value=\"123\">"), properties)
      .get.asInstanceOf[FreetextAnswer]

    if (ans.answer.equals(outputCode+"")) {
      val answerJson = dao.getAnswer(questionId).get
      val answer = JSON.parseFull(answerJson).get.asInstanceOf[Map[String, String]]

      decorated.approveAndBonusAnswer(ans)
      Some(BallotAnswer(answer, BallotQuery(xml.XML.loadString(html))))
    } else {
      decorated.rejectAnswer(ans, "Invalid code")
      //TODO: fix counter
      if(counter <= 0) {
        counter -= 1
        processQuery(query, properties)
      } else {
        None
      }
    }

  }

  override def getDefaultPortalKey: String = decorated.getDefaultPortalKey

  override def cancelQuery(query: HCompQuery): Unit = decorated.cancelQuery(query)

}

object BallotPortalAdapter {
  val CONFIG_ACCESS_ID_KEY = "hcomp.ballot.decoratedPortalKey"
  val FORM_POST_URL = ConfigFactory.load().getString("defaultFormUrl")
  val PORTAL_KEY = "ballot"
}

class BallotPortalBuilder extends HCompPortalBuilder {

  val DECORATED_PORTAL_KEY = "decoratedPortalKey"
  val BASE_FORM_URL = ConfigFactory.load().getString("defaultFormUrl")

  override def build: HCompPortalAdapter = new BallotPortalAdapter(
    HComp(params(DECORATED_PORTAL_KEY))
      .asInstanceOf[HCompPortalAdapter with AnswerRejection],
    baseFormUrl = params(BASE_FORM_URL))

  override def expectedParameters: List[String] = List(DECORATED_PORTAL_KEY, BASE_FORM_URL)

  override def parameterToConfigPath: Map[String, String] = Map(
    DECORATED_PORTAL_KEY -> BallotPortalAdapter.CONFIG_ACCESS_ID_KEY,
    BASE_FORM_URL -> BallotPortalAdapter.FORM_POST_URL
  )
}