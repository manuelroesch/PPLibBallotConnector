package ch.uzh.ifi.pplibballotconnector.hcomp.ballot

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pplibballotconnector.dao.{BallotDAO, DAO}
import org.joda.time.DateTime

import scala.collection.mutable
import scala.util.Random
import scala.util.parsing.json.JSON
import scala.xml._

/**
 * Created by mattia on 06.07.15.
 */
@HCompPortal(builder = classOf[BallotPortalBuilder], autoInit = true)
class BallotPortalAdapter(val decorated: HCompPortalAdapter with AnswerRejection, val dao: DAO = new BallotDAO(),
                          val baseURL: String) extends HCompPortalAdapter {

  // Think about moving this variable somewhere else
  var numRetriesProcessQuery = 10

  override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {

    var htmlToDisplayOnBallotPage: NodeSeq = query match {
      case q: HTMLQuery => q.html
      case _ => XML.loadString(query.question)
    }

    var answer: Option[HCompAnswer] = null

    val batchIdFromDB = properties match {
      case p: BallotProperties => {
        dao.getBatchIdByUUID(p.batch.uuid.toString).getOrElse {
          dao.createBatch(p.allowedAnswersPerTurker, UUID.randomUUID().toString)
        }
      }
      case _ => dao.createBatch(0, UUID.randomUUID().toString)
    }

    val expectedCodeFromDecoratedPortal = properties match {
      case p: BallotProperties => {
        p.outputCode
      }
      case _ => Math.abs(new Random(new DateTime().getMillis).nextLong())
    }

    if ((htmlToDisplayOnBallotPage \\ "form").nonEmpty) {
      // Check and complete if action is not set or is set wrong
      (htmlToDisplayOnBallotPage \\ "form").foreach(f =>
        if(!(f.attribute("action").isDefined && f.attribute("action").get.text.equalsIgnoreCase(baseURL+"storeAnswer"))){
          htmlToDisplayOnBallotPage = XML.loadString(htmlToDisplayOnBallotPage.toString().replace("<"+f.label+">", "<form action=\""+baseURL+"storeAnswer\" method=\"post\">")) \\ "form"
        }
      )
      if (!(htmlToDisplayOnBallotPage \\ "form").exists(form => ensureFormHasValidInputElements(form))) {
        logger.error("Form is not valid.")
        answer = None
      }
    } else {
      logger.error("There exists no Form tag in the html page.")
      answer = None
    }

    if (answer != None) {
      val questionUUID = UUID.randomUUID().toString
      val questionId = dao.createQuestion(htmlToDisplayOnBallotPage.toString(), expectedCodeFromDecoratedPortal, batchIdFromDB, questionUUID)
      val link = baseURL + "showQuestion/" + questionUUID
      val ans = decorated.sendQueryAndAwaitResult(FreetextQuery(link + "<br> click the link and enter here the code when you are finish:<br> <input type=\"text\">"), properties)
        .get.asInstanceOf[FreetextAnswer]

      if (ans.answer.equals(expectedCodeFromDecoratedPortal + "")) {
        answer = extractAnswerFromDatabase(questionId, htmlToDisplayOnBallotPage)
        decorated.approveAndBonusAnswer(ans)
      } else {
        decorated.rejectAnswer(ans, "Invalid code")
        if (numRetriesProcessQuery > 0) {
          numRetriesProcessQuery -= 1
          answer = processQuery(query, properties)
        } else {
          logger.error("Query reached the maximum number of retry attempts.")
          answer = None
        }
      }
    }
    answer
  }

  def extractAnswerFromDatabase(questionId: Long, htmlToDisplayOnBallotPage: NodeSeq): Option[HCompAnswer] = {
    val answerParsed = JSON.parseFull(dao.getAnswer(questionId).get).get.asInstanceOf[Map[String, String]]
    Some(HTMLQueryAnswer(answerParsed, HTMLQuery(htmlToDisplayOnBallotPage)))
  }

  override def getDefaultPortalKey: String = decorated.getDefaultPortalKey

  override def cancelQuery(query: HCompQuery): Unit = decorated.cancelQuery(query)

  //TODO: restructure this
  def ensureFormHasValidInputElements(form: NodeSeq): Boolean = {
    val checkAttributesOfInputElements = new mutable.HashMap[NodeSeq, Map[String, String]]
    if((form \\ "input").nonEmpty){ checkAttributesOfInputElements += (form \\ "input" ->  Map("type" -> "submit"))}
    if((form \\ "textarea").nonEmpty){ checkAttributesOfInputElements += (form \\ "textarea" ->  Map("name" -> ""))}
    if((form \\ "button").nonEmpty){ checkAttributesOfInputElements += (form \\ "button" ->  Map("type" -> "submit"))}
    if((form \\ "select").nonEmpty){ checkAttributesOfInputElements += (form \\ "select" ->  Map("name" -> ""))}
    if (checkAttributesOfInputElements.isEmpty) {
      logger.error("The form doesn't contain any input, select, textarea or button.")
      false
    } else {
      checkAttributesOfInputElements.forall(a => validate(a._1, a._2))
    }
  }

  def validate(inputElements: NodeSeq, attributesKeyValue: Map[String, String]): Boolean = {
    attributesKeyValue.forall(a => {
      inputElements.exists(element => element.attribute(a._1).exists(attribute => attribute.text.equalsIgnoreCase(a._2)))
    })
  }
}

object BallotPortalAdapter {
  val CONFIG_ACCESS_ID_KEY = "hcomp.ballot.decoratedPortalKey"
  val CONFIG_BASE_URL = "hcomp.ballot.baseURL"
  val PORTAL_KEY = "ballot"
}

class BallotPortalBuilder extends HCompPortalBuilder {

  val DECORATED_PORTAL_KEY = "decoratedPortalKey"
  val BASE_URL = "BaseURL"

  override def build: HCompPortalAdapter = new BallotPortalAdapter(
    HComp(params(DECORATED_PORTAL_KEY))
      .asInstanceOf[HCompPortalAdapter with AnswerRejection],
    baseURL = params(BASE_URL))

  override def expectedParameters: List[String] = List(DECORATED_PORTAL_KEY, BASE_URL)

  override def parameterToConfigPath: Map[String, String] = Map(
    DECORATED_PORTAL_KEY -> BallotPortalAdapter.CONFIG_ACCESS_ID_KEY,
    BASE_URL -> BallotPortalAdapter.CONFIG_BASE_URL
  )
}