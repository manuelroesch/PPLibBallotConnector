package ch.uzh.ifi.pplibballotconnector.hcomp

import ch.uzh.ifi.pdeboer.pplib.hcomp.HComp
import ch.uzh.ifi.pdeboer.pplib.hcomp.mturk.MechanicalTurkPortalAdapter
import ch.uzh.ifi.pdeboer.pplib.hcomp.randomportal.RandomHCompPortal
import ch.uzh.ifi.pplibballotconnector.hcomp.pplibballotconnector.{BallotPortalAdapter, BallotPortalBuilder}
import org.junit.{Assert, Test}
import scala.concurrent.duration._

/**
 * Created by mattia on 07.07.15.
 */
class BallotPortalBuilderTest {
  @Test
  def testInitIncomplete: Unit = {
    val b = new BallotPortalBuilder()

    try {
      b.build
      Assert.assertFalse("we have actually expected an exception here", true)
    }
    catch {
      case e: Throwable => Assert.assertTrue(true)
    }
  }

  @Test
  def testInitComplete: Unit = {

    val mturk = new MechanicalTurkPortalAdapter("aaa", "bbb")
    HComp.addPortal("asd", mturk)

    val b = new BallotPortalBuilder()
    b.setParameter(b.DECORATED_PORTAL_KEY, "asd")

    val p = b.build.asInstanceOf[BallotPortalAdapter]

    Assert.assertEquals(mturk, p.decorated)
  }
}