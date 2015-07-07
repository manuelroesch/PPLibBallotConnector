package ch.uzh.ifi.pplibballotconnector

import ch.uzh.ifi.pplibballotconnector.util.LazyLogger
import ch.uzh.ifi.pplibballotconnector.persistence.DBSettings

/**
 * Created by mattia on 07.07.15.
 */
object Main extends App with LazyLogger {

  println("Hi")
  logger.info("TEST logger")

  DBSettings.initialize()
}
