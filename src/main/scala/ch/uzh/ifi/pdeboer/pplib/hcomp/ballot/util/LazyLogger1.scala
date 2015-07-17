package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.util

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

trait LazyLogger1 {
  @transient protected lazy val logger: Logger =
    Logger(LoggerFactory.getLogger(getClass.getName))
}