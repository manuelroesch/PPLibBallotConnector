package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.snippet

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.integrationtest.console.Constants
import ch.uzh.ifi.pdeboer.pplib.process.entities.{HCompQueryBuilder, Patch, ProcessStub}

import scala.reflect.ClassTag
import scala.xml.NodeSeq

/**
 * Created by pdeboer on 30/08/15.
 */
class SnippetHTMLQueryBuilder(ballotHtmlPage: NodeSeq) extends HCompQueryBuilder[List[Patch]] {
	override def buildQuery(queryKey: String, input: List[Patch], base: ProcessStub[_, _]): HCompQuery = {
		HTMLQuery(ballotHtmlPage)
	}

	override def parseAnswer[TARGET](queryKey: String, input: List[Patch], answer: HCompAnswer, base: ProcessStub[_, _])(implicit baseCls: ClassTag[TARGET]): Option[TARGET] = {
		val ans = answer.is[HTMLQueryAnswer]
		val ret = baseCls.runtimeClass match {
			case x: Class[String] =>
				val likert = ans.answers.get("confidence").get.toInt
				if (likert >= Constants.LIKERT_VALUE_CLEANED_ANSWERS) {
					if (ans.answers.get("isRelated").get.equalsIgnoreCase(SnippetHTMLQueryBuilder.POSITIVE) &&
						ans.answers.get("isCheckedBefore").get.equalsIgnoreCase(SnippetHTMLQueryBuilder.POSITIVE)) {
						Some(SnippetHTMLQueryBuilder.POSITIVE)
					}
					else {
						Some(SnippetHTMLQueryBuilder.NEGATIVE)
					}
				} else {
					None
				}
			case _ => None
		}
		ret.asInstanceOf[Option[TARGET]]
	}
}

object SnippetHTMLQueryBuilder {
	val POSITIVE = "yes"
	val NEGATIVE = "no"
}