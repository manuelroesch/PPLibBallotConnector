package ch.uzh.ifi.pplibballotconnector.persistence

/**
 * Created by Mattia on 19.01.2015.
 */

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

}