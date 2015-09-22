package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.integrationtest.console

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot._
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao.BallotDAO
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.DBSettings
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.report.Report
import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger

/**
 * Created by mattia on 07.07.15.
 */
object ConsoleIntegrationTest extends App with LazyLogger {
	DBSettings.initialize()
	val dao = new BallotDAO

	if (args.length >= 1) {
		DBSettings.loadPermutations(args(0), args(1))
	} else {
		logger.info("Resuming last run...")
	}

	val ballotPortalAdapter = HComp(BallotPortalAdapter.PORTAL_KEY)

	/**
	 * Algorithm 250:
	 * The algorithm250 allows to disable redundant questions (or permutations).
	 * First, all permutations with the same groupname are disabled since an assumption is assumed to be related only to a single method.
	 * Second, all the permutation with the same methodIndex, pdf filename and assumption name are disabled. This second step is performed
	 * because it is assumed that a method (or group of methods) is/are related to only one assumption type.
	 */
	val algorithm250 = Algorithm250(dao, ballotPortalAdapter)

	val groups = dao.getAllPermutations().groupBy(gr => {
		gr.groupName.split("/").apply(1)
	}).toSeq

	groups.mpar.foreach(group => {
		group._2.foreach(permutation => {
			if (dao.getPermutationById(permutation.id).map(_.state).getOrElse(-1) == 0) {
				algorithm250.executePermutation(permutation)
			}
		})
	})

	Report.writeCSVReport(dao) //TODO does this always export everything in the DB? I'd consider changing this such that .writeCSVReport gets a list of HCompAnswer's
}