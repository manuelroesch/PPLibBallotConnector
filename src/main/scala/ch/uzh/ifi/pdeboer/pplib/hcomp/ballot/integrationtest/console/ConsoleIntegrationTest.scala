package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.integrationtest.console

import java.io._
import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao.BallotDAO
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.{Answer, DBSettings}
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.{Asset, BallotPortalAdapter, BallotProperties, Batch}
import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import org.apache.commons.codec.binary.Base64

import scala.io.Source
import scala.xml.NodeSeq

/**
 * Created by mattia on 07.07.15.
 */
object ConsoleIntegrationTest extends App with LazyLogger {

  val ANSWERS_PER_QUERY = 1
  val RESULT_CSV_FILENAME = "results.csv"
  val LIKERT_VALUE_CLEANED_ANSWERS = 5

  DBSettings.initialize()
  val dao = new BallotDAO
  try {
    if (args(0).equalsIgnoreCase("init")) {
      dao.loadPermutationsCSV(args(1))
    }
  } catch {
    case e: Exception => logger.debug("Resuming last run...")
  }

  val ballotPortalAdapter = HComp(BallotPortalAdapter.PORTAL_KEY)

  val groups = dao.getAllPermutations().groupBy(gr => gr.groupName.substring(0, gr.groupName.indexOf("/"))).toSeq

  groups.mpar.foreach(group => {
    group._2.foreach(permutation => {
      val p = dao.getPermutationById(permutation.id)
      if(p.isDefined && p.get.state == 0) {
        val answers: List[Map[String, String]] = prepareHCompQuestionAndAsk(new File(p.get.pdfPath), new File(p.get.snippetFilename), p.get.id)
        // Aggregate answer and look if cleaned Q1 = y and Q2 = Y is the result
        val cleanedYesQ1 = answers.count(ans => ans.get("confidence").get.toInt>=LIKERT_VALUE_CLEANED_ANSWERS && isPositive(ans.get("isRelated")).get)
        val cleanedYesQ2 = answers.count(ans => ans.get("confidence").get.toInt>=LIKERT_VALUE_CLEANED_ANSWERS && isPositive(ans.get("isCheckedBefore")).isDefined && isPositive(ans.get("isCheckedBefore")).get)
        val cleanedNoQ1 = answers.count(ans => ans.get("confidence").get.toInt>=LIKERT_VALUE_CLEANED_ANSWERS && isNegative(ans.get("isRelated")).get)
        val cleanedNoQ2 = answers.count(ans => ans.get("confidence").get.toInt>=LIKERT_VALUE_CLEANED_ANSWERS && isNegative(ans.get("isCheckedBefore")).isDefined && isNegative(ans.get("isCheckedBefore")).get)
        if(cleanedYesQ1> cleanedNoQ1 && cleanedYesQ2 > cleanedNoQ2){
          //Set state of permutation to 1
          dao.updateStateOfPermutationId(p.get.id, p.get.id)
          // disable some permutations
          dao.getAllOpenByGroupName(p.get.groupName).map(g => {
            dao.updateStateOfPermutationId(g.id, p.get.id, 1)
          })
          val secondStep = p.get.groupName.split("/")
          dao.getAllOpenGroupsStartingWith(secondStep.slice(0,2).mkString("/")).filter(_.methodIndex == p.get.methodIndex).map(g => {
            dao.updateStateOfPermutationId(g.id, p.get.id, 2)
          })
        }else {
          //Set state of permutation to -1
          dao.updateStateOfPermutationId(p.get.id, -1)
        }
      }
    })
  })

  createCSVReport


  def prepareHCompQuestionAndAsk(pdfFile: File, snippet: File, hints: Long): List[Map[String, String]] = {

    val base64Image = getBase64String(snippet)
    val permutation = dao.getPermutationById(hints)
    val ballotHtmlPage: NodeSeq = createHtmlPage(base64Image, dao.getPermutationById(hints).get.methodOnTop, permutation.get.relativeHeightTop, permutation.get.relativeHeightBottom)
    val query = HTMLQuery(ballotHtmlPage)
    val pdfName = pdfFile.getName
    val pdfPath = pdfFile.getParentFile.getPath
    val pdfInputStream: InputStream = new FileInputStream(pdfFile)
    val pdfSource = Source.fromInputStream(pdfInputStream)
    val pdfBinary = Stream.continually(pdfInputStream.read).takeWhile(-1 !=).map(_.toByte).toArray
    pdfSource.close()

    val contentType = "application/pdf"//new MimetypesFileTypeMap().getContentType(pdfFile)

    val properties = new BallotProperties(Batch(UUID.randomUUID()),
      List(Asset(pdfBinary, contentType, pdfName)), 1, 50, hints)

    val answers : List[HTMLQueryAnswer] = askQuestion(0, query, properties, List.empty[HTMLQueryAnswer])

    answers.map(a => a.answers)
  }

  def askQuestion(it: Int, query: HTMLQuery, properties: BallotProperties, sofar: List[HTMLQueryAnswer]): List[HTMLQueryAnswer] = {
    if(it < ANSWERS_PER_QUERY) {
      try {
        ballotPortalAdapter.processQuery(query, properties) match {
          case ans: Option[HTMLQueryAnswer] => {
            if (ans.isDefined) {
              logger.info(s"Answer: ${ans.get.answers.mkString("\n- ")}")
              askQuestion(it+1, query, properties, sofar ::: List[HTMLQueryAnswer](ans.get))
            } else {
              logger.info("Error while getting the answer.")
              askQuestion(it, query, properties, sofar)
            }
          }
          case _ => {
            logger.info("Unknown error!")
            askQuestion(it, query, properties, sofar)
          }
        }
      }
      catch {
        case e: Throwable => {
          logger.error("There was a problem with the query engine", e)
          List.empty[HTMLQueryAnswer]
        }
      }
    }else {
      sofar
    }
  }

  def createCSVReport: Unit = {
    val writer = new PrintWriter(new File(RESULT_CSV_FILENAME))

    writer.write("snippet,yes answers,no answers,cleaned yes,cleaned no,yes answers,no answers,cleaned yes,cleaned no,feedbacks,firstExclusion,secondExclusion\n")

    val results = dao.getAllAnswers.groupBy(g => {dao.getAssetFileNameByQuestionId(g.questionId).get}).map(answersForSnippet => {

      val hints = dao.getHintByQuestionId(answersForSnippet._2.head.questionId).get
      val allPermutationsDisabledByActualAnswer = dao.getAllPermutationsWithStateEquals(hints).filterNot(f => f.excluded_step == 0)
      val snippetName = answersForSnippet._1
      val aa: List[Answer] = dao.getAllAnswersBySnippet(snippetName)

      val cleanFormatAnswers: List[Map[String, String]] = aa.map(a => a.answerJson.substring(1, a.answerJson.length - 1) .replaceAll("\"", "").split(",").toList.map(aa => aa.split(":").head.replaceAll(" ", "") -> aa.split(":")(1).substring(1)).toMap)
      val answers: List[CsvAnswer] = convertToCSVFormat(cleanFormatAnswers)

      val yesQ1 = answers.count(ans => isPositive(ans.q1).get)
      val yesQ2 = answers.count(ans => isPositive(ans.q2).isDefined && isPositive(ans.q2).get)
      val noQ1 = answers.count(ans => isNegative(ans.q1).get)
      val noQ2 = answers.count(ans => isNegative(ans.q2).isDefined && isNegative(ans.q2).get)

      val cleanedYesQ1 = answers.count(ans => ans.likert >= LIKERT_VALUE_CLEANED_ANSWERS && isPositive(ans.q1).get)
      val cleanedYesQ2 = answers.count(ans => ans.likert >= LIKERT_VALUE_CLEANED_ANSWERS && isPositive(ans.q2).isDefined && isPositive(ans.q2).get)
      val cleanedNoQ1 = answers.count(ans => ans.likert >= LIKERT_VALUE_CLEANED_ANSWERS && isNegative(ans.q1).get)
      val cleanedNoQ2 = answers.count(ans => ans.likert >= LIKERT_VALUE_CLEANED_ANSWERS && isNegative(ans.q2).isDefined && isNegative(ans.q2).get)

      val feedbacks = answers.map(_.feedback).mkString(";")

      val firstExcluded = allPermutationsDisabledByActualAnswer.filter(f => f.excluded_step == 1).map(_.snippetFilename).mkString(";")
      val secondExcluded = allPermutationsDisabledByActualAnswer.filter(f => f.excluded_step == 2).map(_.snippetFilename).mkString(";")

      snippetName + "," + yesQ1 + "," + noQ1 + "," + cleanedYesQ1 + "," + cleanedNoQ1 + "," + yesQ2 + "," + noQ2 + "," + cleanedYesQ2 + "," + cleanedNoQ2 + "," + feedbacks + "," + firstExcluded + "," + secondExcluded

    })

    writer.write(results.mkString("\n"))
    writer.close()
  }

  def isPositive(answer: Option[String]): Option[Boolean] = {
    if(answer.isDefined) {
      Some(answer.get.equalsIgnoreCase("yes"))
    }else if (answer.isDefined) {
      Some(false)
    }else{
      None
    }
  }

  def isNegative(answer: Option[String]): Option[Boolean] = {
    if(answer.isDefined) {
      Some(answer.get.equalsIgnoreCase("no"))
    }else if (answer.isDefined) {
      Some(false)
    }else{
      None
    }
  }

  def convertToCSVFormat(answers: List[Map[String, String]]): List[CsvAnswer] = {
    answers.map(ans => {
      val isRelated = ans.get("isRelated")
      val isCheckedBefore = ans.get("isCheckedBefore")
      val likert = ans.get("confidence")
      val descriptionIsRelated = ans.get("descriptionIsRelated")

      CsvAnswer(isRelated, isCheckedBefore, likert.get.toInt, descriptionIsRelated.get)
    })
  }

  case class CsvAnswer(q1: Option[String], q2: Option[String], likert: Int, feedback: String)

  def getBase64String(image: File) = {
    val imageInFile: FileInputStream = new FileInputStream(image)
    val imageData = new Array[Byte](image.length().asInstanceOf[Int])
    imageInFile.read(imageData)
    "data:image/png;base64," + Base64.encodeBase64String(imageData)
  }

  def createHtmlPage(imageBase64Format: String, isMethodOnTop: Boolean, relativeHeightTop: Double = 0, relativeHeightBottom: Double = 0): NodeSeq = {
    <div ng-controller="QuestionCtrl">

      <p>
        Thank you for participating in our survey.
        <br/>
        Our goal is to see whether you are able to grasp some of the main concepts in the field of statistics without needing to be an expert in that field - just by basic text understanding. For that matter, we have prepared multiple such surveys; in all of which you can participate
        <b>at most once</b>
        .
      </p>
      <hr style="width:100%"/>
      <p>
        In the field of statistics, one generally uses
        <b>statistical methods</b>
        (such as ANOVA) to compare groups of data and derive findings. These <b>Statistical methods</b> in general require some
        <b>prerequisites</b> to be satisfied before being applied to data. Please have a look at the text-snipplet below. You'll find a <span style="background-color:#FFFF00;">statistical method marked in yellow</span> and a <span style="background-color:#00FF00;">prerequisite marked in green.</span>
      </p>

      <div class="row" style="display: table;">
        <div class="col-md-2" style="float: none;display: table-cell;vertical-align: top;"> </div>
        <div class="col-md-8" style="float: none;display: table-cell;vertical-align: top;">
          <div id="imgContainer" style="width:100%; height:350px; border:1px solid black;overflow:auto;">
            <img id="snippet" src={imageBase64Format} width="100%"></img>
          </div>
        </div>
        <div class="col-md-2" style="float: none;display: table-cell;vertical-align: top;">
          <div id="snippetButtons">

            <button type="button" id="top" class="btn btn-info" style="width:200px;" aria-hidden="true">
              <span class="glyphicon glyphicon-arrow-up"> </span>{if (isMethodOnTop) {
              "Scroll to Method"
            } else {
              "Scroll to Prerequisite"
            }}
            </button>
            <br />
            <br />
            <button type="button" id="bottom" class="btn btn-info" style="width:200px;" aria-hidden="true">
              <span class="glyphicon glyphicon-arrow-down"> </span> {if (isMethodOnTop) {
              "Scroll to Prerequisite"
            } else {
              "Scroll to Method"
            }}
            </button>
          </div>
        </div>
      </div>
      <br />

      <div id="assets">
        If you would like to read more context in order to give better and more accurate answers, you can browse the PDF file by clicking <img src="http://www.santacroceopera.it/Images/Pages/PDF.png"></img>
      </div>

      <br/>
      <hr style="width:100%"/>
      <div>
        <h2>In the text above, is there any kind of relationship between the
          <span style="background-color:#00FF00;">prerequisite</span>
          and the
          <span style="background-color:#FFFF00;">method</span>
          ?</h2>
        Note that the relationship can be direct or indirect.
        <br/>
        <ul>
          <li>example for a direct relationship: "We have tested [PREREQUISITE] before we used [METHOD] and found that ..."</li>
          <li>example for an indirect relationship: "Our data was tested for [PREREQUISITE]. Using [METHOD] on our data, we have found that ..."</li>
        </ul>
      </div>

      <form onsubmit="return checkFeedbackForm()">
        <h3>
          <label class="radio-inline">
            <input type="radio" name="isRelated" ng-model="isRelated" id="yes" value="Yes" required="required"/>
            Yes
          </label>
          <label class="radio-inline">
            <input type="radio" name="isRelated" ng-model="isRelated" id="no" value="No"/>
            No
          </label>
        </h3>

        <span ng-if="isRelated=='Yes'">
          <hr style="width:100%"/>
          <div>
            <h2>
              Did the authors of the text confirm that they have checked the
              <span style="background-color:#00FF00;">prerequisite</span>
              before applying the
              <span style="background-color:#FFFF00;">method</span>
              ?
            </h2>
          </div>
          <h3>
            <label class="radio-inline">
              <input type="radio" name="isCheckedBefore" id="yes" value="Yes" required="required"/>
              Yes
            </label>
            <label class="radio-inline">
              <input type="radio" name="isCheckedBefore" id="no" value="No"/>
              No
            </label>
          </h3>
        </span>

        <hr style="width:100%"/>

        <div class="form-group">
          <label for="descriptionIsRelated">
            Please briefly describe why you selected Yes/No in the previous questions. Please also let us know if you felt uncertain with the answer you've provided. This is also your opportunity to tell us what you thought about this HIT.
          </label>
          <textarea class="form-control" name="descriptionIsRelated" id="descriptionIsRelated" rows="5" required="required">Your text here</textarea>
        </div>

        <input type="hidden" name="confidence" value="7"></input>

        <hr style="width:100%"/>
        <input type="submit" class="btn btn-large btn-primary" style="width:150px;float:right;" value="Submit Answer"/>

      </form>
      <br/>
      <br/>

      <script type="text/javascript">
        {scala.xml.PCData( """
          $('#top').click(function() {
            $('#imgContainer').animate({
              scrollTop: $('#imgContainer')[0].scrollHeight*"""+(relativeHeightTop-2.0)+"""/100
              }, 1000);
          });

          $('#bottom').click(function() {
            $('#imgContainer').animate({
              scrollTop: $('#imgContainer')[0].scrollHeight*"""+(relativeHeightBottom-2.0)+"""/100
              }, 1000);
          });


          $(document).ready( function() {
            if($('#snippet').height() < $('#imgContainer').height()){
              $('#snippetButtons').hide();
            }else{
              $('#top').click();
            }
          });

          function checkFeedbackForm()  {
            var value = document.getElementById('descriptionIsRelated').value;
						if(value.length == 0 || value == 'Your text here') {
						  alert('Please provide feedback!');
							return false;
            } else {
						  return true;
						}
          }; """
      )}
      </script>

    </div>
  }

}