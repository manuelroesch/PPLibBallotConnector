package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence

/**
 * Created by Mattia on 19.01.2015.
 */

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.integrationtest.console.ConsoleIntegrationTest._
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import scalikejdbc._
import scalikejdbc.config.DBs

trait DBSettings {
  DBSettings.initialize()
}

object DBSettings extends LazyLogger{

  private var isInitialized = false

  def initialize(): Unit = this.synchronized {
    if (!isInitialized) {
      DBs.setupAll()

      GlobalSettings.loggingSQLErrors = true
      //GlobalSettings.sqlFormatter = SQLFormatterSettings("devteam.misc.HibernateSQLFormatter")
      DBInitializer.run()
      isInitialized = true
      logger.debug("Database initializated")
    }
  }

  def resumeOrInitializeDB(init: Option[String], path: Option[String]): Unit = {
    try {
      if (init.get.equalsIgnoreCase("init")) {
        dao.loadPermutationsCSV(path.get)
      }
    } catch {
      case e: Exception => logger.debug("Resuming last run...")
    }
  }

}