package ch.uzh.ifi.pplibballotconnector.hcomp

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pplibballotconnector.dao.{DAO, BallotDAO}

import scala.xml.NodeSeq

/**
 * Created by mattia on 06.07.15.
 */

case class BallotQuery(html: NodeSeq, suggestedPaymentCents: Int = 10, title: String = "") extends HCompQuery with Serializable{

  override def question: String = html.toString()

}

