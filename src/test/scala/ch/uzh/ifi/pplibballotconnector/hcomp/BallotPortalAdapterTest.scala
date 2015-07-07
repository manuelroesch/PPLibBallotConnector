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
  def testInitIncomplete: Unit = {
    val b = new BallotPortalAdapter(new PortalAdapterTest(), new DaoTest())
    val query = BallotQuery(<h1>test</h1>)
    val prop = new BallotProperties(new Batch(), 1, 123)

    val ans = b.processQuery(query, prop)

    Assert.assertEquals(ans.asInstanceOf[Option[BallotAnswer]].get.answers.get("answer").get,"yes")
  }

}

class PortalAdapterTest extends HCompPortalAdapter with AnswerRejection{
  override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
    Some(FreetextAnswer(FreetextQuery("<div>Awesome question</div>"), "123"))
  }

  override def getDefaultPortalKey: String = "test"

  override def cancelQuery(query: HCompQuery): Unit = null
}

class DaoTest extends DAO {
  override def createBatch(allowedAnswerPerTurker: Int, uuid: String): Long = {
    Assert.assertTrue(allowedAnswerPerTurker == 1)
    1
  }

  override def getAnswer(questionId: Long): Option[String] = {
    Assert.assertTrue(questionId == 1)
    Some("{\"answer\":\"yes\"}")
  }

  override def getBatchIdByUUID(uuid: String): Option[Long] = None

  override def getQuestionUUID(questionId: Long): Option[String] = None

  override def createQuestion(html: String, outputCode: Long, batchId: Long): Long = {
    Assert.assertEquals(<h1>test</h1>.toString(), html)
    Assert.assertEquals(batchId, 1)
    1
  }
}
