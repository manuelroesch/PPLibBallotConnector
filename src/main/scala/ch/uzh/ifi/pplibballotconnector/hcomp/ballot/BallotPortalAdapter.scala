package ch.uzh.ifi.pplibballotconnector.hcomp.ballot

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pplibballotconnector.dao.{BallotDAO, DAO}
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime

import scala.util.Random
import scala.util.parsing.json.JSON

/**
 * Created by mattia on 06.07.15.
 */
@HCompPortal(builder = classOf[BallotPortalBuilder], autoInit = true)
class BallotPortalAdapter(val decorated: HCompPortalAdapter with AnswerRejection, val dao: DAO = new BallotDAO(),
                          val baseUrl: String) extends HCompPortalAdapter {

  // Think about moving this variable somewhere else
  var numRetriesProcessQuery = 10

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

    //TODO: rewrite this so that looks good
    val formLabel = (xml.XML.loadString(html) \ "form")
    if(formLabel.length>0){
      if(formLabel.head.attribute("action").isDefined){
        val tag = formLabel.head
        if(!tag.attribute("action").get.text.equalsIgnoreCase(baseUrl+"storeAnswer")){
          return None
        }
        var valid = false
        for(c <- tag.child){
          if(!valid && c.label.equalsIgnoreCase("input") && c.attribute("type").isDefined && c.attribute("type").get.text.equalsIgnoreCase("submit")){
            valid = true
          }
          if(!valid && c.label.equalsIgnoreCase("select") && c.attribute("name").isDefined && !c.attribute("name").get.text.isEmpty){
            if(c.child.length>0){
              if(c.child.head.label.equalsIgnoreCase("option") && !c.child.head.attribute("value").get.isEmpty){
                valid = true
              }
            }
          }
          if(!valid && c.label.equalsIgnoreCase("textarea") && c.attribute("name").isDefined && !c.attribute("name").get.text.isEmpty){
            valid = true
          }
          if(!valid && c.label.equalsIgnoreCase("button") && c.attribute("type").isDefined && !c.attribute("type").get.text.equalsIgnoreCase("submit")){
            valid = true
          }
        }
        if(!valid){
          logger.error("Form elements are not valid for query: " + query)
          return None
        }
      }else {
        logger.error("Action attribute is not defined in the form tag of query: " + query)
        return None
      }
    }else {
      logger.error("There is no form in the html page for query: " + query)
      return None
    }

    val questionId = dao.createQuestion(html, outputCode, batchId)

    val link = baseUrl+"showQuestion/".concat(dao.getQuestionUUID(questionId).getOrElse("-1"))

    val ans = decorated.sendQueryAndAwaitResult(FreetextQuery(link + "<br> click the link and enter here the code when you are finish:<br> <input type=\"text\">"), properties)
      .get.asInstanceOf[FreetextAnswer]

    if (ans.answer.equals(outputCode+"")) {
      val answerJson = dao.getAnswer(questionId).get
      val answer = JSON.parseFull(answerJson).get.asInstanceOf[Map[String, String]]

      decorated.approveAndBonusAnswer(ans)
      Some(BallotAnswer(answer, BallotQuery(xml.XML.loadString(html))))
    } else {
      decorated.rejectAnswer(ans, "Invalid code")
      
      if(numRetriesProcessQuery > 0) {
        numRetriesProcessQuery -= 1
        processQuery(query, properties)
      } else {
        logger.error("Query reached the maximum number of retry attempts.")
        None
      }
    }

  }

  override def getDefaultPortalKey: String = decorated.getDefaultPortalKey

  override def cancelQuery(query: HCompQuery): Unit = decorated.cancelQuery(query)

}

object BallotPortalAdapter {
  val CONFIG_ACCESS_ID_KEY = "hcomp.ballot.decoratedPortalKey"
  val BASE_URL = ConfigFactory.load().getString("hcomp.ballot.baseURL")
  val PORTAL_KEY = "ballot"
}

class BallotPortalBuilder extends HCompPortalBuilder {

  val DECORATED_PORTAL_KEY = "decoratedPortalKey"
  val BASE_URL = ConfigFactory.load().getString("hcomp.ballot.baseURL")

  override def build: HCompPortalAdapter = new BallotPortalAdapter(
    HComp(params(DECORATED_PORTAL_KEY))
      .asInstanceOf[HCompPortalAdapter with AnswerRejection],
    baseUrl = params(BASE_URL))

  override def expectedParameters: List[String] = List(DECORATED_PORTAL_KEY, BASE_URL)

  override def parameterToConfigPath: Map[String, String] = Map(
    DECORATED_PORTAL_KEY -> BallotPortalAdapter.CONFIG_ACCESS_ID_KEY,
    BASE_URL -> BallotPortalAdapter.BASE_URL
  )
}