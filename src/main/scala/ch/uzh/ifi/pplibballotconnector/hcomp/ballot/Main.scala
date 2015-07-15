package ch.uzh.ifi.pplibballotconnector.hcomp.ballot

import java.io.{File, FileInputStream}

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import ch.uzh.ifi.pplibballotconnector.persistence.DBSettings
import org.apache.commons.codec.binary.Base64

import scala.xml.NodeSeq

/**
 * Created by mattia on 07.07.15.
 */
object Main extends App with LazyLogger {

  DBSettings.initialize()

  val decoratedPortalAdapter = new ConsolePortalAdapter()
  val ballotPortalAdapter = new BallotPortalAdapter(decoratedPortalAdapter, baseURL = "http://localhost:9000/")

  val SNIPPET_DIR = "snippets/"

  new File(SNIPPET_DIR).listFiles().foreach(snippet => {
    val imageInFile : FileInputStream = new FileInputStream(snippet)
    val imageData = new Array[Byte](snippet.length().asInstanceOf[Int])
    imageInFile.read(imageData)
    val base64Image = "data:image/png;base64,"+Base64.encodeBase64String(imageData)

    val ballotHtmlPage : NodeSeq =
      <div>
        <p>
          Thank you for participating in our survey.<br />
          Our goal is to see whether you are able to grasp some of the main concepts in the field of statistics without needing to be an expert in that field - just by basic text understanding. For that matter, we have prepared multiple such surveys; in all of which you can participate <b>at most once</b>.
        </p>
        <hr style="width:100%"/>
        <p>
          In the field of statistics, one generally uses <b>statistical methods</b> (such as ANOVA) to compare groups of data and derive findings.<br />
          These <b>Statistical methods</b> in general require some <b>prerequisites</b> to be satisfied before being applied to data.<br />
          Please have a look at the text-snipplet below. You'll find a <span style="background-color:#FFFF00;">statistical method marked in yellow</span> and a <span style="background-color:#00FF00;">prerequisite marked in green.</span>
        </p>
        <div>
          <img src={base64Image} style="border:1px solid black;" width="90%" height="90%"></img>
        </div>

        <br />
        <hr style="width:100%" />
        <div>
          <h2>In the text above, is there any kind of relationship between the <span style="background-color:#00FF00;">prerequisite</span> and the <span style="background-color:#FFFF00;">method</span>?</h2>
          Note that the relationship can be direct or indirect.
          <br />
          <ul>
            <li>example for a direct relationship: "We have tested [PREREQUISITE] before we used [METHOD] and found that ..."</li>
            <li>example for an indirect relationship: "Our data was tested for [PREREQUISITE]. Using [METHOD] on our data, we have found that ..."</li>
          </ul>
        </div>
        <div class="container">
          <div class="row">
            <div class="col-md-6 col-md-offset-3">
              <form>
                <input type="submit" name="answer" value="Yes" class="btn btn-large btn-primary" style="width:150px;" />
                <input type="submit" name="answer" value="No" class="btn btn-large btn-primary" style="width:150px;" />
              </form>
            </div>
          </div>
        </div>
        <br />
        <br />
      </div>

    val query = HTMLQuery(ballotHtmlPage)
    val properties = new BallotProperties(Batch(), 1)

    ballotPortalAdapter.processQuery(query, properties) match {
      case ans : Option[HTMLQueryAnswer] => {
        if (ans.isDefined){
          println("\n\n***** " + ans.get.answers)
        }else {
          println("Error while getting the answer")
        }
      }
      case _ => println("Unknown error!")
    }

  })
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
