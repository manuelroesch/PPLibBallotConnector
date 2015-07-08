package ch.uzh.ifi.pplibballotconnector.hcomp.ballot

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pplibballotconnector.dao.{BallotDAO, DAO}
import org.joda.time.DateTime

import scala.util.Random
import scala.util.parsing.json.JSON
import scala.xml.{NodeSeq, XML}

/**
 * Created by mattia on 06.07.15.
 */
@HCompPortal(builder = classOf[BallotPortalBuilder], autoInit = true)
class BallotPortalAdapter(val decorated: HCompPortalAdapter with AnswerRejection, val dao: DAO = new BallotDAO(),
						  val baseURL: String) extends HCompPortalAdapter {

	// Think about moving this variable somewhere else
	var numRetriesProcessQuery = 10

	override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
		val htmlToDisplayOnBallotPage: NodeSeq = query match {
			case q: HTMLQuery => q.html
			case _ => XML.loadString(query.question)
		}

		val batchIdFromDB = properties match {
			case p: BallotProperties => {
				dao.getBatchIdByUUID(p.getBatch().uuid.toString).getOrElse {
					dao.createBatch(p.getAllowedAnswersPerTurker(), UUID.randomUUID().toString)
				}
			}
			case _ => dao.createBatch(0, UUID.randomUUID().toString)
		}

		val expectedCodeFromDecoratedPortal = properties match {
			case p: BallotProperties => {
				p.getOutputCode()
			}
			case _ => new Random(new DateTime().getMillis).nextLong()
		}

		val formLabel = htmlToDisplayOnBallotPage \\ "form"
		if (formLabel.nonEmpty) {
			//TODO put code to replace "action" here instead of throwing exception
			if (!formLabel.exists(form => ensureFormHasInputElements(form) &&
				form.attribute("method").exists(f => f.text.equalsIgnoreCase("post")
				))) {
				logger.error("Form is not valid.")
				return None //TODO work without RETURN statement in SCALA.
			}
		} else {
			logger.error("There exists no form tag in the html page.")
			return None
		}

		val questionUUID = UUID.randomUUID().toString
		val questionId = dao.createQuestion(htmlToDisplayOnBallotPage.toString(), expectedCodeFromDecoratedPortal, batchIdFromDB, questionUUID)

		val link = baseURL + "showQuestion/" + questionUUID

		val ans = decorated.sendQueryAndAwaitResult(FreetextQuery(link + "<br> click the link and enter here the code when you are finish:<br> <input type=\"text\">"), properties)
			.get.asInstanceOf[FreetextAnswer]

		if (ans.answer.equals(expectedCodeFromDecoratedPortal + "")) {
			val answerJson = dao.getAnswer(questionId).get
			val answer = JSON.parseFull(answerJson).get.asInstanceOf[Map[String, String]]

			decorated.approveAndBonusAnswer(ans)
			Some(HTMLQueryAnswer(answer, HTMLQuery(htmlToDisplayOnBallotPage)))
		} else {
			decorated.rejectAnswer(ans, "Invalid code")

			if (numRetriesProcessQuery > 0) {
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


	def ensureFormHasInputElements(form: NodeSeq): Boolean = {
		val (inputs, selects, textareas, buttons) = (form \\ "input", form \\ "select", form \\ "textarea", form \\ "button")

		if (inputs.isEmpty && selects.isEmpty && textareas.isEmpty && buttons.isEmpty) {
			logger.error("The form doesn't contain any input, select, textarea or button.")
			return false //TODO dont use return
		} else {
			List(inputs, selects, textareas, buttons).forall(l => true) //TODO call validation methods here
			return (
				(if (inputs.nonEmpty)
					validateInput(inputs)
				else true)
					&&
					(if (selects.nonEmpty)
						validateSelect(selects)
					else true)
					&&
					(if (textareas.nonEmpty)
						validateTextarea(textareas)
					else true)
					&&
					(if (buttons.nonEmpty)
						validateButton(buttons)
					else true)
				)
		}
	}

	//TODO merge all element-methods into 1, where you define what exactly to test for using parameters
	def validateInput(input: NodeSeq): Boolean = {
		input.forall(i => {
			i.attribute("type").isDefined && i.attribute("type").get.text.equalsIgnoreCase("submit")
		})
	}

	def validateTextarea(textarea: NodeSeq): Boolean = {
		textarea.forall(t => {
			t.attribute("name").isDefined && !t.attribute("name").get.text.isEmpty
		})
	}

	def validateButton(button: NodeSeq): Boolean = {
		button.forall(b => {
			b.attribute("type").isDefined && b.attribute("type").get.text.equalsIgnoreCase("submit")
		})
	}

	def validateSelect(select: NodeSeq): Boolean = {
		select.forall(s => {
			if (s.attribute("name").isDefined && !s.attribute("name").get.text.isEmpty) {
				val options = s \\ "option"
				if (options.isEmpty) {
					logger.error("The select tag: " + s + " doesn't contain any options.")
					false
				} else {
					if (!options.forall(o => o.attribute("value").get.nonEmpty)) {
						logger.error("The select tag: " + s + " contains invalid options.")
						false
					} else {
						true
					}
				}
			} else {
				logger.error("The select tag: " + s + " is not valid.")
				false
			}
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