package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.snippet

import com.typesafe.scalalogging.LazyLogging

import scala.xml.{NodeSeq, XML}

/**
 * Created by mattia on 02.09.15.
 */
object SnippetHTMLValidator extends LazyLogging {

  def checkAndFixHTML(ns: NodeSeq, baseURL: String): NodeSeq = {
    val allForms = (ns \\ "form").map(n => XML.loadString(n.toString().replaceAll("<form(.*)>", "<form action=\"" + baseURL + "storeAnswer\" method=\"get\" $1>")))

    val htmlToDisplayOnBallotPage : NodeSeq = ns
    htmlToDisplayOnBallotPage
  }

  def hasFormValidInputElements(form: NodeSeq): Boolean = {
    val supportedFields = List[(String, Map[String, List[String]])](
      "input" -> Map("type" -> List[String]("submit", "radio", "hidden")),
      "textarea" -> Map("name" -> List.empty[String]),
      "button" -> Map("type" -> List[String]("submit")),
      "select" -> Map("name" -> List.empty[String]))

    val checkAttributesOfInputElements = supportedFields.map(formField => {
      if ((form \\ formField._1).nonEmpty) {
        (form \\ formField._1) -> formField._2
      }
    }).collect { case found: (NodeSeq, Map[String, List[String]]) => found }

    if (checkAttributesOfInputElements.isEmpty) {
      logger.error("The form doesn't contain any input, select, textarea or button.")
      false
    } else {
      checkAttributesOfInputElements.forall(a => hasInputElementAllNeededAttributes(a._1, a._2))
    }
  }

  private def hasInputElementAllNeededAttributes(inputElements: NodeSeq, attributesKeyValue: Map[String, List[String]]): Boolean = {
    attributesKeyValue.exists(attribute => {
      inputElements.exists(element =>
        element.attribute(attribute._1).exists(attributeValue => {
          if (attributeValue.text.nonEmpty) {
            if(attribute._2.isEmpty){
              true
            }else {
              attribute._2.contains(attributeValue.text)
            }
          } else {
            if (attribute._2.isEmpty) {
              true
            } else {
              false
            }
          }
        })
      )
    })
  }

}
