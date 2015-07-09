package ch.uzh.ifi.pplibballotconnector

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import ch.uzh.ifi.pplibballotconnector.hcomp.ballot.{Batch, BallotProperties, BallotPortalAdapter}
import ch.uzh.ifi.pplibballotconnector.persistence.DBSettings

import scala.xml.NodeSeq

/**
 * Created by mattia on 07.07.15.
 */
object Main extends App with LazyLogger {

  DBSettings.initialize()

  val decoratedPortalAdapter = new ConsolePortalAdapter()
  val ballotPortalAdapter = new BallotPortalAdapter(decoratedPortalAdapter, baseURL = "http://localhost:9000/")

  val ballotHtmlPage: NodeSeq =
    <div>
      <p>
        Thank you for participating in our survey.<br />
        Our goal is to see whether you are able to grasp some of the main concepts in the field of statistics without needing to be an expert in that field - just by basic text understanding. For that matter, we have prepared multiple such surveys; in all of which you can participate at most once.
      </p>
      <hr style="width:100%"/>
      <p>
        Please have a look at the text-snipplet below. You'll find a <span style="background-color:#FFFF00;">statistical method marked in yellow</span> and a <span style="background-color:#00FF00;">prerequisite marked in green.</span>
      </p>
      <img src="https://uozdoe.qualtrics.com/WRQualtricsControlPanel/Graphic.php?IM=IM_6mL4HZmXUcLUbJj" style="width:70%;"></img>
      <br />
      <hr style="width:100%" />
      <p>
        In the text above, is there any kind of relationship between the prerequisite and the method?
        <br />
        Note that the relationship can be direct or indirect.
        <br />
        <ul>
          <li>example for a direct relationship: "We have tested [PREREQUISITE] before we used [METHOD] and found that ..."</li>
          <li>example for an indirect relationship: "Our data was tested for [PREREQUISITE]. Using [METHOD] on our data, we have found that ..."</li>
        </ul>
      </p>
      <form>
        <input type="submit" name="answer" value="Yes" style="width:100px;height:50px;" />
        <input type="submit" name="answer" value="No" style="width:100px;height:50px;"/>
      </form>
    </div>

  val query = HTMLQuery(ballotHtmlPage)
  val properties = new BallotProperties(Batch(), 1)

  ballotPortalAdapter.processQuery(query, properties) match {
    case ans : Option[HTMLQueryAnswer] => println("\n\n***** " + ans.get.answers)
    case None => logger.error("Error while getting the answer")
  }

}

class ConsolePortalAdapter extends HCompPortalAdapter with AnswerRejection {
  override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {

    val freeTextQuery = query match {
      case p: FreetextQuery => p
      case _ => FreetextQuery(query.question)
    }

    println(freeTextQuery.question)

    Some(FreetextAnswer(freeTextQuery, scala.io.StdIn.readLine("\n> ").toString()))
  }

  override def getDefaultPortalKey: String = "consolePortalAdapter"

  override def cancelQuery(query: HCompQuery): Unit = ???
}
