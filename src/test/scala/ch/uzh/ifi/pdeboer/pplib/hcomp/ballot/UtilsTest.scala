package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot

import org.junit.{Assert, Test}

/**
  * Created by pdeboer on 18/11/15.
  */
class UtilsTest {
	@Test
	def testGenerateSecret: Unit = {
		Assert.assertEquals(43, Utils.generateSecret().length)
	}

}
