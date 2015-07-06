package ch.uzh.ifi.pplibballotconnector.hcomp

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pplibballotconnector.dao.{DAO, BallotDAO}

/**
 * Created by mattia on 06.07.15.
 */


case class BallotQuery(uuid: String, html: String, outputCode: Long, questionId: Long) extends HCompQuery with Serializable{

  def getAnswer(dao: DAO): Option[HCompAnswer] = {
    Some(BallotAnswer(this, dao.getAnswer(questionId).get.toLong))
  }

  override def suggestedPaymentCents: Int = 8

  override def title: String = ""

  override def question: String = html
}

