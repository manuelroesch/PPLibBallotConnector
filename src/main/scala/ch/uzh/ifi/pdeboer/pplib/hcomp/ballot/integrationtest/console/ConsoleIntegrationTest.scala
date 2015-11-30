package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.integrationtest.console

import java.io.File

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot._
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao.BallotDAO
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.{DBSettings, Permutation}
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.report.Report
import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger

import scala.io.Source

/**
  * Created by mattia on 07.07.15.
  */
object ConsoleIntegrationTest extends App with LazyLogger {
	DBSettings.initialize()
	val dao = new BallotDAO

	val ballotPortalAdapter = HComp(BallotPortalAdapter.PORTAL_KEY)

	/**
	  * Algorithm 250:
	  * The algorithm250 allows to disable redundant questions (or permutations).
	  * First, all permutations with the same groupname are disabled since an assumption is assumed to be related only to a single method.
	  * Second, all the permutation with the same methodIndex, pdf filename and assumption name are disabled. This second step is performed
	  * because it is assumed that a method (or group of methods) is/are related to only one assumption type.
	  */
	val algorithm250 = Algorithm250(dao, ballotPortalAdapter)

	private val DEFAULT_TEMPLATE_ID: Long = 1L

	if (args.length == 1 && args(0) == "inittemplate") {
		logger.info("init template")
		val template: File = new File("template/perm.csv")
		if (template.exists()) {
			val templatePermutations = Source.fromFile(template).getLines().drop(1).map(l => {
				val perm: Permutation = Permutation.fromCSVLine(l)
				dao.createPermutation(perm)
			})
			templatePermutations.foreach(permutationId => {
				val q = algorithm250.buildQuestion(dao.getPermutationById(permutationId).get, isTemplate = true)
				ballotPortalAdapter.sendQuery(HTMLQuery(q._2, 1, "", ""), q._1)
			})
			Thread.sleep(1000)
			assert(!templatePermutations.contains(DEFAULT_TEMPLATE_ID), "Our template didn't get ID 1. Please adapt DB. Current template IDs: " + templatePermutations.mkString(","))
		}
		logger.info("done")
		System.exit(0)
	} else if (args.length == 2) {
		logger.info("Loading new permutations")
		DBSettings.loadPermutations(args(0), args(1))
	} else {
		logger.info("Resuming last run...")
	}

	val groups = dao.getAllPermutations().filter(_.id != DEFAULT_TEMPLATE_ID).groupBy(gr => {
		gr.groupName.split("/").apply(0)
	}).map(g => (g._1, g._2.sortBy(_.distanceMinIndexMax))).toList

	groups.mpar.foreach(group => {
		group._2.foreach(permutation => {
			if (dao.getPermutationById(permutation.id).map(_.state).getOrElse(-1) == 0) {
				algorithm250.executePermutation(permutation)
			}
		})
	})

	Report.writeCSVReport(dao)
	Report.writeCSVReportAllAnswers(dao)
}