package ch.uzh.ifi.pplibballotconnector.hcomp.ballot

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pplibballotconnector.dao.DAO
import ch.uzh.ifi.pplibballotconnector.util.LazyLogger
import org.junit.{Assert, Test}

import scala.collection.mutable

/**
 * Created by mattia on 07.07.15.
 */
class BallotPortalAdapterTest {

  @Test
  def testProcessQuery: Unit = {
    val b = new BallotPortalAdapter(new PortalAdapterTest(), new DAOTest(), "http://www.andreas.ifi.uzh.ch:9000/")
    val query = BallotQuery(<div><h1>test</h1><form action="http://www.andreas.ifi.uzh.ch:9000/storeAnswer"><input type="submit" name="answer" value="yes" /></form></div>)
    val prop = new BallotProperties(new Batch(), 1, 123)

    val ans = b.processQuery(query, prop)

    Assert.assertEquals(ans.asInstanceOf[Option[BallotAnswer]].get.answers.get("answer").get, "yes")
  }

  @Test
  def testWithoutBallotProperties: Unit = {
    val daoTest = new DAOTest()
    val b = new BallotPortalAdapter(new PortalAdapterTest(), daoTest, "http://www.andreas.ifi.uzh.ch:9000/")
    val ans = b.processQuery(BallotQuery(<div><h1>test</h1><form action="http://www.andreas.ifi.uzh.ch:9000/storeAnswer"><input type="submit" name="answer" value="yes" /></form></div>),
      new HCompQueryProperties())

    // Outputcode will be generated by the portal adapter and won't match the value sent by the PortalAdapterTest (fix value = 123)
    Assert.assertEquals(ans, None)

    // 10 retries + 1 other successful test = 11 questions/batches/answers in database
    Assert.assertEquals(daoTest.questions.size, 11)
    Assert.assertEquals(daoTest.batches.size, 11)
    Assert.assertEquals(daoTest.answers.size, 11)
  }

  @Test
  def testWithoutForm: Unit = {
    val b = new BallotPortalAdapter(new PortalAdapterTest(), new DAOTest(), "http://www.andreas.ifi.uzh.ch:9000/")
    val ans = b.processQuery(BallotQuery(<h1>test</h1>), new BallotProperties(new Batch(), 1, 123))

    Assert.assertEquals(ans, None)
  }

  @Test
  def testWithoutFormsInput: Unit = {
    val b = new BallotPortalAdapter(new PortalAdapterTest(), new DAOTest(), "http://www.andreas.ifi.uzh.ch:9000/")
    val ans = b.processQuery(BallotQuery(<div><h1>test</h1><form action="http://www.andreas.ifi.uzh.ch:9000/storeAnswer"></form></div>), new BallotProperties(new Batch(), 1, 123))
    Assert.assertEquals(ans, None)
  }

}

class PortalAdapterTest() extends HCompPortalAdapter with AnswerRejection{
  override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
    Some(FreetextAnswer(FreetextQuery("<div>Awesome question</div>"), "123"))
  }

  override def getDefaultPortalKey: String = "test"

  override def cancelQuery(query: HCompQuery): Unit = false
}

class DAOTest extends DAO with LazyLogger{

  val batches = new mutable.HashMap[Long, String]
  val questions = new mutable.HashMap[Long, String]
  val answers = new mutable.HashMap[Long, String]

  override def createBatch(allowedAnswerPerTurker: Int, uuid: String): Long = {
    logger.debug("Adding new Batch: " + uuid )
    batches += ((batches.size+1).toLong -> uuid)
    batches.size.toLong
  }

  override def getAnswer(questionId: Long): Option[String] = {
    answers.get(questionId)
  }

  override def getBatchIdByUUID(uuid: String): Option[Long] = {
    batches.foreach(b => {
      if(b._2.equals(uuid)){
        logger.debug("Found batch by UUID: " + b._1)
        return Some(b._1)
      }
    })
    Some(-1)
  }

  override def getQuestionUUID(questionId: Long): Option[String] = {
    questions.get(questionId)
  }

  override def createQuestion(html: String, outputCode: Long, batchId: Long): Long = {
    questions += ((questions.size+1).toLong -> UUID.randomUUID().toString)
    answers += ((answers.size+1).toLong -> "{\"answer\":\"yes\"}")
    logger.debug("Adding new Question with outputCode: " + outputCode)
    questions.size.toLong
  }
}