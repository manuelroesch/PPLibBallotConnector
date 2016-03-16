package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.snippet

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.integrationtest.console.Constants
import ch.uzh.ifi.pdeboer.pplib.process.entities.{HCompQueryBuilder, InstructionGenerator, Patch, ProcessStub}

import scala.reflect.runtime.universe._
import scala.xml.NodeSeq

/**
  * Created by pdeboer on 30/08/15.
  */
class SnippetHTMLQueryBuilder(ballotHtmlPage: NodeSeq, questionDescription: String, questionTitle: String = "") extends HCompQueryBuilder[List[Patch]] {
	override def buildQuery(input: List[Patch], base: ProcessStub[_, _], nonBaseClassInstructionGenerator: Option[InstructionGenerator] = None): HCompQuery = {
		HTMLQuery(ballotHtmlPage, questionPreviewOverride = questionDescription, title = questionTitle)
	}

	override def parseAnswer[TARGET](input: List[Patch], answer: HCompAnswer, base: ProcessStub[_, _])(implicit baseCls: TypeTag[TARGET]): Option[TARGET] = {
		val ans = answer.is[HTMLQueryAnswer]
		val ret = if (baseCls.tpe <:< typeOf[String]) {
			val likert = ans.answers.get("confidence").get.toInt
			if (likert >= Constants.LIKERT_VALUE_CLEANED_ANSWERS) {
				if (ans.answers.getOrElse("isRelated", "").equalsIgnoreCase(SnippetHTMLQueryBuilder.POSITIVE) &&
					ans.answers.getOrElse("isCheckedBefore", "").equalsIgnoreCase(SnippetHTMLQueryBuilder.POSITIVE)) {
					Some(SnippetHTMLQueryBuilder.POSITIVE)
				}
				else {
					Some(SnippetHTMLQueryBuilder.NEGATIVE)
				}
			} else {
				None
			}
		} else None
		ret.asInstanceOf[Option[TARGET]]
	}
}

object SnippetHTMLQueryBuilder {
	val POSITIVE = "yes"
	val NEGATIVE = "no"
}