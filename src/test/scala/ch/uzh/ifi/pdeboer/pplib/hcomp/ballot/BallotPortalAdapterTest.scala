package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.Permutation
import org.junit.{Assert, Test}

/**
  * Created by mattia on 07.07.15.
  */
class BallotPortalAdapterTest {

	@Test
	def testProcessQuery: Unit = {
		val dao = new DAOTest()
		val b = new BallotPortalAdapter(new PortalAdapterTest(), dao, "http://www.andreas.ifi.uzh.ch:9000/")
		val query = HTMLQuery(<div>
			<h1>test</h1> <form something="cool">
				<input type="submit" name="answer" value="yes"/>
			</form>
		</div>)

		val permutation1 = Permutation(1, "pdfFile/Assumption/15:555", "METHOD_15:9898", getClass.getResource("/pngFile.png").getPath, getClass.getResource("/pdfFile.pdf").getPath, false, 0, 0, 0.0, 0.0, 10)
		dao.createPermutation(permutation1)

		val prop = new BallotProperties(Batch(), List(Asset(Array.empty[Byte], "application/pdf", "empty filename")), permutation1.id)

		val ans = b.processQuery(query, prop)

		Assert.assertEquals(ans.asInstanceOf[Option[HTMLQueryAnswer]].get.answers.get("answer").get, "yes")
		Assert.assertTrue((ans.asInstanceOf[Option[HTMLQueryAnswer]].get.query.asInstanceOf[HTMLQuery].html \\ "form").toString contains "something=\"cool\"")
		Assert.assertTrue((ans.asInstanceOf[Option[HTMLQueryAnswer]].get.query.asInstanceOf[HTMLQuery].html \\ "form").toString contains "action=\"http://www.andreas.ifi.uzh.ch:9000/storeAnswer\"")
	}

	@Test
	def testWithoutBallotProperties: Unit = {
		val dao = new DAOTest()
		val b = new BallotPortalAdapter(new PortalAdapterTest(), dao, "http://www.andreas.ifi.uzh.ch:9000/")

		val permutation1 = Permutation(1, "pdfFile/Assumption/15:555", "METHOD_15:9898", getClass.getResource("/pngFile.png").getPath, getClass.getResource("/pdfFile.pdf").getPath, false, 0, 0, 0.0, 0.0, 10)
		dao.createPermutation(permutation1)

		val ans = b.processQuery(HTMLQuery(<div>
			<h1>test</h1> <form>
				<input type="submit" name="answer" value="yes"/>
			</form>
		</div>),
			new HCompQueryProperties())

		Assert.assertEquals(ans.asInstanceOf[Option[HTMLQueryAnswer]].get.answers.get("answer").get, "yes")

		Assert.assertEquals(dao.questions.size, 1)
		Assert.assertEquals(dao.batches.size, 1)
		Assert.assertEquals(dao.answers.size, 1)
	}

	@Test
	def testWithoutForm: Unit = {
		val dao = new DAOTest
		val b = new BallotPortalAdapter(new PortalAdapterTest(), dao, "http://www.andreas.ifi.uzh.ch:9000/")

		val permutation1 = Permutation(1, "pdfFile/Assumption/15:555", "METHOD_15:9898", getClass.getResource("/pngFile.png").getPath, getClass.getResource("/pdfFile.pdf").getPath, false, 0, 0, 0.0, 0.0, 10)
		dao.createPermutation(permutation1)

		val ans = b.processQuery(HTMLQuery(<h1>test</h1>), new BallotProperties(Batch(), List(Asset(Array.empty[Byte], "application/pdf", "empty filename")), 1))

		Assert.assertEquals(ans, None)
	}

	@Test
	def testDeepFormStructure: Unit = {
		val dao = new DAOTest
		val b = new BallotPortalAdapter(new PortalAdapterTest(), dao, "http://www.andreas.ifi.uzh.ch:9000/")

		val permutation1 = Permutation(1, "pdfFile/Assumption/15:555", "METHOD_15:9898", getClass.getResource("/pngFile.png").getPath, getClass.getResource("/pdfFile.pdf").getPath, false, 0, 0, 0.0, 0.0, 10)
		dao.createPermutation(permutation1)

		val ans = b.processQuery(HTMLQuery(<div>
			<h1>test</h1> <div>
				<form>
					<p>
						<input type="submit" name="answer" value="yes"/>
					</p>
				</form>
			</div>
		</div>), new BallotProperties(Batch(), List(Asset(Array.empty[Byte], "application/pdf", "empty filename")), 1))
		Assert.assertEquals(ans.asInstanceOf[Option[HTMLQueryAnswer]].get.answers.get("answer").get, "yes")
	}

	@Test
	def testWithInvalidInputAttribute: Unit = {
		val dao = new DAOTest

		val b = new BallotPortalAdapter(new PortalAdapterTest(), dao, "http://www.andreas.ifi.uzh.ch:9000/")

		val permutation1 = Permutation(1, "pdfFile/Assumption/15:555", "METHOD_15:9898", getClass.getResource("/pngFile.png").getPath, getClass.getResource("/pdfFile.pdf").getPath, false, 0, 0, 0.0, 0.0, 10)
		dao.createPermutation(permutation1)

		val ans = b.processQuery(HTMLQuery(<div>
			<h1>test</h1> <div>
				<form>
					<p>
						<input type="button" name="answer" value="yes"/>
					</p>
				</form>
			</div>
		</div>), new BallotProperties(Batch(), List(Asset(Array.empty[Byte], "application/pdf", "empty filename")), 1))
		Assert.assertEquals(ans, None)
	}

	@Test
	def testWithoutFormsInput: Unit = {
		val dao = new DAOTest

		val b = new BallotPortalAdapter(new PortalAdapterTest(), dao, "http://www.andreas.ifi.uzh.ch:9000/")

		val permutation1 = Permutation(1, "pdfFile/Assumption/15:555", "METHOD_15:9898", getClass.getResource("/pngFile.png").getPath, getClass.getResource("/pdfFile.pdf").getPath, false, 0, 0, 0.0, 0.0, 10)
		dao.createPermutation(permutation1)

		val ans = b.processQuery(HTMLQuery(<div>
			<h1>test</h1> <form></form>
		</div>), new BallotProperties(Batch(), List(Asset(Array.empty[Byte], "application/pdf", "empty filename")), 1))
		Assert.assertEquals(ans, None)
	}

	@Test
	def testWithActionInput: Unit = {
		val dao = new DAOTest

		val b = new BallotPortalAdapter(new PortalAdapterTest(), dao, "http://www.andreas.ifi.uzh.ch:9000/")

		val permutation1 = Permutation(1, "pdfFile/Assumption/15:555", "METHOD_15:9898", getClass.getResource("/pngFile.png").getPath, getClass.getResource("/pdfFile.pdf").getPath, false, 0, 0, 0.0, 0.0, 10)
		dao.createPermutation(permutation1)

		val ans = b.processQuery(HTMLQuery(<div>
			<h1>test</h1> <form action="http://www.google.com/">
				<input type="submit" name="answer" value="yes"/>
			</form>
		</div>), new BallotProperties(Batch(), List(Asset(Array.empty[Byte], "application/pdf", "empty filename")), 1))
		Assert.assertEquals(ans.asInstanceOf[Option[HTMLQueryAnswer]].get.answers.get("answer").get, "yes")
		Assert.assertTrue((ans.asInstanceOf[Option[HTMLQueryAnswer]].get.query.asInstanceOf[HTMLQuery].html \\ "form").toString contains "action=\"http://www.andreas.ifi.uzh.ch:9000/storeAnswer\"")
	}
}

class PortalAdapterTest() extends HCompPortalAdapter with AnswerRejection {
	override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
		Some(FreetextAnswer(FreetextQuery("<div>Awesome question</div>"), "123"))
	}

	override def getDefaultPortalKey: String = "test"

	override def cancelQuery(query: HCompQuery): Unit = false
}


