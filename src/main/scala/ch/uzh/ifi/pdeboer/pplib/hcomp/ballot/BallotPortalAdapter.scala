package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao.{BallotDAO, DAO}
import play.api.libs.json.{JsObject, Json}

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

		val actualProperties: BallotProperties = properties match {
			case p: BallotProperties => p
			case _ => {
				val uuid = UUID.randomUUID()
				val batch = dao.createBatch(0, uuid)
				new BallotProperties(Batch(uuid), List(Asset(Array.empty[Byte], "application/pdf")), 0)
			}
		}

		var htmlToDisplayOnBallotPage: NodeSeq = query match {
			case q: HTMLQuery => q.html
			case _ => XML.loadString(query.question)
		}

		val batchIdFromDB: Long =
			dao.getBatchIdByUUID(actualProperties.batch.uuid).getOrElse(
				dao.createBatch(actualProperties.allowedAnswersPerTurker, actualProperties.batch.uuid))


		val expectedCodeFromDecoratedPortal = actualProperties.outputCode

		def checkAndFixFormFields: Boolean = {
			(htmlToDisplayOnBallotPage \\ "form").forall(f =>
				if (f.attribute("action").isEmpty && f.attribute("method").isEmpty) {
					val correctedHtmlToDisplayOnBallotPage = htmlToDisplayOnBallotPage.toString().replaceAll("\\<" + f.label + "(.*)\\>", "<form action=\"" + baseURL + "storeAnswer\" method=\"post\" $1>")
					htmlToDisplayOnBallotPage = XML.loadString(correctedHtmlToDisplayOnBallotPage)
					true
				} else {
					logger.error("Form contains an action and/or method attribute. Please remove them.")
					false
				}
			)
		}

		val answer: Option[HCompAnswer] = {
			if ((htmlToDisplayOnBallotPage \\ "form").nonEmpty) {

				val formWithoutActionAndMethod: Boolean = checkAndFixFormFields

				if (!formWithoutActionAndMethod || !(htmlToDisplayOnBallotPage \\ "form").exists(form => ensureFormHasValidInputElements(form))) {
					logger.error("Form's content is not valid.")
					None
				} else {
					val questionUUID = UUID.randomUUID()
					val questionId = dao.createQuestion(htmlToDisplayOnBallotPage.toString(), expectedCodeFromDecoratedPortal, batchIdFromDB, questionUUID)

					actualProperties.assets.foreach(asset => dao.createAsset(asset.binary, asset.contentType, questionId))

					val link = baseURL + "showQuestion/" + questionUUID

					val ans = decorated.sendQueryAndAwaitResult(
						FreetextQuery(
							s"""
							   Hey there. Thank you for being interested in this task! In the following <a href=\"$link\">URL</a> you'll find a Survey showing you a text snippet and asking you if two terms (highlighted in the text) do have a relationship of some sorts.<br/>
							   Please fill in the survey and, once finished, enter the confirmation code below such that we can pay you. <br/>
							   Please note that you will only be able to submit one assignment for this survey <br />
							   <a href=\"$link\">$link</a>""".stripMargin, "", "Are these two words in the text related?"), properties)
						.get.asInstanceOf[FreetextAnswer]

					if (ans.answer.equals(expectedCodeFromDecoratedPortal + "")) {
						decorated.approveAndBonusAnswer(ans)
						extractAnswerFromDatabase(questionId, htmlToDisplayOnBallotPage)
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
			} else {
				logger.error("There exists no Form tag in the html page.")
				None
			}
		}
		answer
	}

	def extractAnswerFromDatabase(questionId: Long, htmlToDisplayOnBallotPage: NodeSeq): Option[HCompAnswer] = {
		val result = Json.parse(dao.getAnswer(questionId).getOrElse("{}")).asInstanceOf[JsObject]
		val answer = result.fieldSet.map(f => (f._1 -> f._2.toString().replaceAll("\"", ""))).toMap
		Some(HTMLQueryAnswer(answer, HTMLQuery(htmlToDisplayOnBallotPage)))
	}

	override def getDefaultPortalKey: String = BallotPortalAdapter.PORTAL_KEY

	override def cancelQuery(query: HCompQuery): Unit = decorated.cancelQuery(query)

	def ensureFormHasValidInputElements(form: NodeSeq): Boolean = {
		val supportedFields = List[(String, Map[String, String])](
			"input" -> Map("type" -> "submit"),
			"textarea" -> Map("name" -> ""),
			"button" -> Map("type" -> "submit"),
			"select" -> Map("name" -> ""))

		var checkAttributesOfInputElements = List.empty[(NodeSeq, Map[String, String])]

		supportedFields.foreach(formField => {
			if ((form \\ formField._1).nonEmpty) {
				checkAttributesOfInputElements ::= ((form \\ formField._1) -> formField._2)
			}
		})

		if (checkAttributesOfInputElements.isEmpty) {
			logger.error("The form doesn't contain any input, select, textarea or button.")
			false
		} else {
			checkAttributesOfInputElements.forall(a => checkAttributesValueForInputElements(a._1, a._2))
		}
	}

	def checkAttributesValueForInputElements(inputElements: NodeSeq, attributesKeyValue: Map[String, String]): Boolean = {
		attributesKeyValue.forall(a => {
			inputElements.exists(element => element.attribute(a._1).exists(attribute => if (a._2.nonEmpty) {
				attribute.text.equalsIgnoreCase(a._2)
			} else {
				attribute.text.nonEmpty
			}))
		})
	}
}

object BallotPortalAdapter {
	val CONFIG_ACCESS_ID_KEY = "decoratedPortalKey"
	val CONFIG_BASE_URL = "baseURL"
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

	override def key: String = BallotPortalAdapter.PORTAL_KEY
}