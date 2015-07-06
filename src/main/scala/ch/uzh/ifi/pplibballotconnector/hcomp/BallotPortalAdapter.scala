package ch.uzh.ifi.pplibballotconnector.hcomp.pplibballotconnector

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pplibballotconnector.dao.{BallotDAO, DAO}
import ch.uzh.ifi.pplibballotconnector.hcomp.{BallotAnswer, BallotProperties, BallotQuery}
import org.joda.time.DateTime

import scala.util.Random

/**
 * Created by mattia on 06.07.15.
 */
class BallotPortalAdapter(val decorated: HCompPortalAdapter, val dao: DAO = new BallotDAO()) extends HCompPortalAdapter{

  def processQuery(query: BallotQuery, properties: BallotProperties): Option[HCompAnswer] = {
    // Generate random code
    val outputCode = new Random(new DateTime().getMillis).nextLong()

    // FIXME: Abstraction. BallotProperties has a UUID.
    // FIXME: Only create a new batch if there exist no batch in the DB with the particular UUID.
    val id = properties.uuid
    val batchId = dao.createBatch(properties.allowedAnswersPerTurker)

    // Create the question
    val questionId = dao.createQuestion(query.question, query.outputCode, batchId)

    // Create link
    val link = "http://andreas.ifi.uzh.ch:9000/showQuestion/".concat(dao.getQuestionUUID(questionId).getOrElse("-1"))

    val ans = decorated.sendQueryAndAwaitResult(query, properties)

    if(ans.get.is[BallotAnswer].answer == outputCode){
      query.getAnswer(dao)
    } else {
      decorated.cancelQuery(query)
      processQuery(query, properties)
    }

  }

  override def getDefaultPortalKey: String = decorated.getDefaultPortalKey

  override def cancelQuery(query: HCompQuery): Unit = decorated.cancelQuery(query)

}