package ch.uzh.ifi.pplibballotconnector.hcomp

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.hcomp.mturk.MechanicalTurkPortalAdapter
import ch.uzh.ifi.pplibballotconnector.dao.DAO
import ch.uzh.ifi.pplibballotconnector.hcomp.pplibballotconnector.{BallotPortalAdapter, BallotPortalBuilder}
import org.junit.{Assert, Test}

/**
 * Created by mattia on 07.07.15.
 */
class BallotPortalAdapterTest {

  @Test
  def testProcessQuery: Unit = {
    val b = new BallotPortalAdapter(new PortalAdapterTest(), new DaoTest())
    val query = BallotQuery(<h1>test<form action="http://www.andreas.ifi.uzh.ch:9000/storeAnswer"></form></h1>)
    val prop = new BallotProperties(new Batch(), 1, 123)

    val ans = b.processQuery(query, prop)

    Assert.assertEquals(ans.asInstanceOf[Option[BallotAnswer]].get.answers.get("answer").get,"yes")
  }

  @Test
  def testWithoutBallotProperties: Unit = {
    val b = new BallotPortalAdapter(new PortalAdapterTest(), new DaoTest())
    val ans = b.processQuery(BallotQuery(<h1>test<form action="http://www.andreas.ifi.uzh.ch:9000/storeAnswer"></form></h1>), new HCompQueryProperties())
    Assert.assertEquals(ans, None)
  }

  @Test
  def testWithoutForm: Unit = {
    val b = new BallotPortalAdapter(new PortalAdapterTest(), new DaoTest())
    val ans = b.processQuery(BallotQuery(<h1>test</h1>), new BallotProperties(new Batch(), 1, 123))
    Assert.assertEquals(ans, None)
  }

  //TODO: Test if html contains form action with correct endpoint
  //TODO: Test if form contains inputs/select/textareas/buttons...

}

class PortalAdapterTest extends HCompPortalAdapter with AnswerRejection{
  override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
    Some(FreetextAnswer(FreetextQuery("<div>Awesome question</div>"), "123"))
  }

  override def getDefaultPortalKey: String = "test"

  override def cancelQuery(query: HCompQuery): Unit = false
}

class DaoTest extends DAO {
  override def createBatch(allowedAnswerPerTurker: Int, uuid: String): Long = {
    1
  }

  override def getAnswer(questionId: Long): Option[String] = {
    Assert.assertTrue(questionId == 1)
    Some("{\"answer\":\"yes\"}")
  }

  override def getBatchIdByUUID(uuid: String): Option[Long] = None

  override def getQuestionUUID(questionId: Long): Option[String] = Some("deb04032-247a-11e5-bde4-45920f92afb9")

  override def createQuestion(html: String, outputCode: Long, batchId: Long): Long = {
    Assert.assertEquals(batchId, 1)
    1
  }
}
