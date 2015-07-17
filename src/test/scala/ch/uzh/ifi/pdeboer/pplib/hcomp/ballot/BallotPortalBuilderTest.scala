package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot

import ch.uzh.ifi.pdeboer.pplib.hcomp.HComp
import ch.uzh.ifi.pdeboer.pplib.hcomp.mturk.MechanicalTurkPortalAdapter
import org.junit.{Assert, Test}

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
    b.setParameter(b.BASE_URL, "http://andreas.ifi.uzh.ch:9000/")

    val p = b.build.asInstanceOf[BallotPortalAdapter]

    Assert.assertEquals(mturk, p.decorated)
  }
}