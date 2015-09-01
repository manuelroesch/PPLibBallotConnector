package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.integrationtest.console

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot._
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao.BallotDAO
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.DBSettings
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.report.Report
import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import com.typesafe.config.ConfigFactory

/**
 * Created by mattia on 07.07.15.
 */
object ConsoleIntegrationTest extends App with LazyLogger {

	val conf = ConfigFactory.load()

  val LIKERT_VALUE_CLEANED_ANSWERS = conf.getInt("likertCleanedAnswers")

	DBSettings.initialize()
	val dao = new BallotDAO

  if(args.length>=1){
	  DBSettings.loadPermutations(args(0), args(1))
  }else{
    logger.info("Resuming last run...")
  }

  val ballotPortalAdapter = HComp(BallotPortalAdapter.PORTAL_KEY)

  val algorithm250 = Algorithm250(dao, ballotPortalAdapter)

	val groups = dao.getAllPermutations().groupBy(gr => gr.groupName.split("/").apply(4)).toSeq
	groups.mpar.foreach(group => {
		group._2.foreach(permutation => {
			val p = dao.getPermutationById(permutation.id)
			if (p.isDefined && p.get.state == 0) {
        algorithm250.executePermutationWith250(p.get)
			}
		})
	})

	Report.writeCSVReport(dao)

}