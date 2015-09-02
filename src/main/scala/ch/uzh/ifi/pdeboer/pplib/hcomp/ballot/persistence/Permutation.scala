package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence

/**
 * Created by mattia on 21.08.15.
 */
case class Permutation(id: Long, groupName: String, methodIndex: String, snippetFilename: String, pdfPath: String,
					   methodOnTop: Boolean, state: Long, excluded_step: Int, relativeHeightTop: Double, relativeHeightBottom: Double)