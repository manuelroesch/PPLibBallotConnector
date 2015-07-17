package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot

import java.io.{File, FileInputStream, FilenameFilter}
import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.DBSettings
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import org.apache.commons.codec.binary.Base64

import scala.xml.NodeSeq

/**
 * Created by mattia on 07.07.15.
 */
object Main extends App with LazyLogger {

  DBSettings.initialize()

  val ballotPortalAdapter = HComp.apply(BallotPortalAdapter.PORTAL_KEY)

  val SNIPPET_DIR = "snippets/"

  val ANSWERS_PER_QUERY = 1

  new File(SNIPPET_DIR).listFiles(new FilenameFilter {
    override def accept(dir: File, name: String): Boolean = name.endsWith(".png")
  }).foreach(snippet => {

    val base64Image = getBase64String(snippet)

    val ballotHtmlPage : NodeSeq = createHtmlPage(base64Image)
    val query = HTMLQuery(ballotHtmlPage)
    val properties = new BallotProperties(Batch(UUID.randomUUID()), 1)

    var answers = List.empty[HTMLQueryAnswer]
    do {
      ballotPortalAdapter.processQuery(query, properties) match {
        case ans: Option[HTMLQueryAnswer] => {
          if (ans.isDefined) {
            answers ::= ans.get
            println("Answer: " + ans.get.answers.mkString(" "))
          } else {
            println("Error while getting the answer")
          }
        }
        case _ => println("Unknown error!")
      }
    }
    while(answers.size < ANSWERS_PER_QUERY)

    println(answers.foreach(a => "ANSWER: "+a.answers))
  })

  def getBase64String(image: File) = {
    val imageInFile : FileInputStream = new FileInputStream(image)
    val imageData = new Array[Byte](image.length().asInstanceOf[Int])
    imageInFile.read(imageData)
    "data:image/png;base64,"+Base64.encodeBase64String(imageData)
  }

  def createHtmlPage(imageBase64Format: String) : NodeSeq = {
      <div ng-controller="QuestionCtrl">
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
          <img src={imageBase64Format} style="border:1px solid black;" width="90%" height="90%"></img>
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

        <form>
          <h3>
            <label class="radio-inline">
              <input type="radio" name="isRelated" ng-model="isRelated" id="yes" value="Yes" required="required" /> Yes
            </label>
            <label class="radio-inline">
              <input type="radio" name="isRelated" ng-model="isRelated" id="no" value="No" /> No
            </label>
          </h3>

          <span ng-if="isRelated=='Yes'">
            <hr style="width:100%" />
            <div>
              <h2>
                Did the authors of the text confirm that they have checked the <span style="background-color:#00FF00;">prerequisite</span> before applying the <span style="background-color:#FFFF00;">method</span>?
              </h2>
            </div>
            <h3>
              <label class="radio-inline">
                <input type="radio" name="isCheckedBefore" id="yes" value="Yes" required="required" /> Yes
              </label>
              <label class="radio-inline">
                <input type="radio" name="isCheckedBefore" id="no" value="No" /> No
              </label>
            </h3>
          </span>

          <hr style="width:100%" />

          <div class="form-group">
            <label for="descriptionIsRelated">
              Please briefly describe why you selected Yes/No in the previous questions. Please also let us know if you felt uncertain with the answer you've provided. This is also your opportunity to tell us what you thought about this HIT.
            </label>
            <textarea class="form-control" name="descriptionIsRelated" id="descriptionIsRelated" rows="5" required="required"> </textarea>
          </div>

          <hr style="width:100%" />
          <p>
            Please select the number below that best represents how certain you feel about the answer you have provided before.
          </p>
          <div class="form-group">
            <div style="width:100%">
              <span style="float:left">
                <b>Not certain at all</b>
              </span>

              <span style="float:right;">
                <b>Absolutely certain</b>
              </span>
            </div>

            <input type="range" min="0" max="5" value="0" id="slider" name="isRelatedAccuracy"/>
          </div>

          <hr style="width:100%" />
          <input type="submit" class="btn btn-large btn-primary" style="width:150px;float:right;" value="Submit Answer" />

        </form>
        <br />
        <br />
      </div>
  }

}

@HCompPortal(builder = classOf[ConsolePortalBuilder], autoInit = true)
class ConsolePortalAdapter extends HCompPortalAdapter with AnswerRejection {
  override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {

    val freeTextQuery = query match {
      case p: FreetextQuery => p
      case _ => FreetextQuery(query.question)
    }

    println(freeTextQuery.question)

    Some(FreetextAnswer(freeTextQuery, scala.io.StdIn.readLine("\n> ").toString()))
  }

  override def getDefaultPortalKey: String = ConsolePortalAdapter.PORTAL_KEY

  override def cancelQuery(query: HCompQuery): Unit = ???
}
object ConsolePortalAdapter {
  val CONFIG_ACCESS_ID_KEY = "hcomp.ballot.decoratedPortalKey"
  val CONFIG_BASE_URL = "hcomp.ballot.baseURL"
  val PORTAL_KEY = "ConsolePortalKey"
}

class ConsolePortalBuilder extends HCompPortalBuilder {

  override def build: HCompPortalAdapter = new ConsolePortalAdapter()

  override def expectedParameters: List[String] = List()

  override def parameterToConfigPath: Map[String, String] = Map()
}