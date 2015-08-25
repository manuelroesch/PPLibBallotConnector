package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence

/**
 * Created by mattia on 24.08.15.
 */
case class Answer(id: Long, questionId: Long, answerJson: String, accepted: Boolean)