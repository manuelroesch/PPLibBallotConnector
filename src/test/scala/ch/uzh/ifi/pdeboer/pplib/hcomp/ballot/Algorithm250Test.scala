package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.Permutation
import org.junit.{Assert, Test}

/**
 * Created by mattia on 15.09.15.
 */
class Algorithm250Test {


  @Test
  def testExecutePermutation: Unit = {

    val dao = new DAOTest()
    val ballotPortalAdapter = new BallotPortalAdapter(new PortalAdapterTest(), dao, "http://www.andreas.ifi.uzh.ch:9000/")
    val alg = Algorithm250(dao, ballotPortalAdapter)

    val permutation = Permutation(1, "pdfFile/Assumption/15:555", "METHOD_15:9898", getClass.getResource("/pngFile.png").getPath, getClass.getResource("/pdfFile.pdf").getPath, false, 0, 0, 0.0, 0.0)
    dao.createPermutation(permutation)

    alg.executePermutation(permutation)

    Assert.assertEquals(dao.getAllPermutations().head.state, 1)
    Assert.assertEquals(dao.getAllPermutations().head.excluded_step, 0)

  }

  @Test
  def testExecutePermutationWithFirstExclusion: Unit = {

    val dao = new DAOTest()
    val ballotPortalAdapter = new BallotPortalAdapter(new PortalAdapterTest(), dao, "http://www.andreas.ifi.uzh.ch:9000/")
    val alg = Algorithm250(dao, ballotPortalAdapter)

    val permutation1 = Permutation(1, "pdfFile/Assumption/15:555", "METHOD_15:9898", getClass.getResource("/pngFile.png").getPath, getClass.getResource("/pdfFile.pdf").getPath, false, 0, 0, 0.0, 0.0)
    val permutation2 = Permutation(2, "pdfFile/Assumption/15:555", "SOMETHING_DIFFERENT_59:8888", getClass.getResource("/pngFile.png").getPath, getClass.getResource("/pdfFile.pdf").getPath, false, 0, 0, 0.0, 0.0)
    dao.createPermutation(permutation1)
    dao.createPermutation(permutation2)

    alg.executePermutation(permutation1)
    Assert.assertEquals(dao.getPermutationById(1).get.state, 1)
    Assert.assertEquals(dao.getPermutationById(1).get.excluded_step, 0)
    Assert.assertEquals(dao.getPermutationById(2).get.state, 1)
    Assert.assertEquals(dao.getPermutationById(2).get.excluded_step, 1)


    alg.executePermutation(permutation2)
    Assert.assertEquals(dao.getPermutationById(2).get.state, 1)
    Assert.assertEquals(dao.getPermutationById(2).get.excluded_step, 1)

  }

  @Test
  def testExecutePermutationWithSecondExclusion: Unit = {

    val dao = new DAOTest()
    val ballotPortalAdapter = new BallotPortalAdapter(new PortalAdapterTest(), dao, "http://www.andreas.ifi.uzh.ch:9000/")
    val alg = Algorithm250(dao, ballotPortalAdapter)

    val permutation1 = Permutation(1, "pdfFile/Assumption/15:555", "METHOD_15:9898", getClass.getResource("/pngFile.png").getPath, getClass.getResource("/pdfFile.pdf").getPath, false, 0, 0, 0.0, 0.0)
    val permutation2 = Permutation(2, "pdfFile/Assumption/7:123", "METHOD_15:9898", getClass.getResource("/pngFile.png").getPath, getClass.getResource("/pdfFile.pdf").getPath, false, 0, 0, 0.0, 0.0)
    dao.createPermutation(permutation1)
    dao.createPermutation(permutation2)

    alg.executePermutation(permutation1)
    Assert.assertEquals(dao.getPermutationById(1).get.state, 1)
    Assert.assertEquals(dao.getPermutationById(1).get.excluded_step, 0)
    Assert.assertEquals(dao.getPermutationById(2).get.state, 1)
    Assert.assertEquals(dao.getPermutationById(2).get.excluded_step, 2)

    alg.executePermutation(permutation2)
    Assert.assertEquals(dao.getPermutationById(2).get.state, 1)
    Assert.assertEquals(dao.getPermutationById(2).get.excluded_step, 2)
  }

  @Test
  def testExecutePermutationWithFirstAndSecondExclusion: Unit = {

    val dao = new DAOTest()
    val ballotPortalAdapter = new BallotPortalAdapter(new PortalAdapterTest(), dao, "http://www.andreas.ifi.uzh.ch:9000/")
    val alg = Algorithm250(dao, ballotPortalAdapter)

    val permutation1 = Permutation(1, "pdfFile/Assumption/15:555", "METHOD_15:9898", getClass.getResource("/pngFile.png").getPath, getClass.getResource("/pdfFile.pdf").getPath, false, 0, 0, 0.0, 0.0)
    val permutation2 = Permutation(2, "pdfFile/Assumption/7:123", "METHOD_15:9898", getClass.getResource("/pngFile.png").getPath, getClass.getResource("/pdfFile.pdf").getPath, false, 0, 0, 0.0, 0.0)
    val permutation3 = Permutation(3, "pdfFile/Assumption/15:555", "AnotherMethod_1:123123", getClass.getResource("/pngFile.png").getPath, getClass.getResource("/pdfFile.pdf").getPath, false, 0, 0, 0.0, 0.0)

    dao.createPermutation(permutation1)
    dao.createPermutation(permutation2)
    dao.createPermutation(permutation3)

    alg.executePermutation(permutation1)
    Assert.assertEquals(dao.getPermutationById(1).get.state, 1)
    Assert.assertEquals(dao.getPermutationById(1).get.excluded_step, 0)
    Assert.assertEquals(dao.getPermutationById(2).get.state, 1)
    Assert.assertEquals(dao.getPermutationById(2).get.excluded_step, 2)
    Assert.assertEquals(dao.getPermutationById(3).get.state, 1)
    Assert.assertEquals(dao.getPermutationById(3).get.excluded_step, 1)
  }

}
