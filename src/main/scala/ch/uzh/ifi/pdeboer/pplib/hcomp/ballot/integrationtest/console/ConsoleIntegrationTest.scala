package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.integrationtest.console

import java.io._
import java.util.UUID
import javax.activation.MimetypesFileTypeMap

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot._
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao.BallotDAO
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.{Answer, DBSettings, Permutation}
import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import com.typesafe.config.ConfigFactory
import play.api.libs.json.{JsObject, Json}

import scala.xml.NodeSeq

/**
 * Created by mattia on 07.07.15.
 */
object ConsoleIntegrationTest extends App with LazyLogger {

	val config = ConfigFactory.load()

	val ANSWERS_PER_QUERY = config.getInt("answersPerSnippet")
	val RESULT_CSV_FILENAME = config.getString("resultFilename")
	val LIKERT_VALUE_CLEANED_ANSWERS = config.getInt("likertCleanedAnswers")

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
			if (p.isDefined && p.get.state == 0) {
				executePermutationWith250(p.get)
			}
		})
	})

	writeCSVReport

	def executePermutationWith250(p: Permutation) = {
		val answers: List[CsvAnswer] = buildAndAskQuestion(new File(p.pdfPath), new File(p.snippetFilename), p.id)

		if (shouldOtherSnippetsBeDisabled(answers)) {
			dao.updateStateOfPermutationId(p.id, p.id)
			dao.getAllOpenByGroupName(p.groupName).foreach(g => {
				dao.updateStateOfPermutationId(g.id, p.id, 1)
			})
			val groupName = p.groupName.split("/")
			dao.getAllOpenGroupsStartingWith(groupName.slice(0, 2).mkString("/")).filter(_.methodIndex == p.methodIndex).foreach(g => {
				dao.updateStateOfPermutationId(g.id, p.id, 2)
			})
		} else {
			dao.updateStateOfPermutationId(p.id, -1)
		}
	}

	def shouldOtherSnippetsBeDisabled(answers: List[CsvAnswer]): Boolean = {
		val cleanedYesQ1 = answers.count(ans => ans.likert >= LIKERT_VALUE_CLEANED_ANSWERS && isPositive(ans.q1).get)
		val cleanedYesQ2 = answers.count(ans => ans.likert >= LIKERT_VALUE_CLEANED_ANSWERS && isPositive(ans.q2).isDefined && isPositive(ans.q2).get)
		val cleanedNoQ1 = answers.count(ans => ans.likert >= LIKERT_VALUE_CLEANED_ANSWERS && isNegative(ans.q1).get)
		val cleanedNoQ2 = answers.count(ans => ans.likert >= LIKERT_VALUE_CLEANED_ANSWERS && isNegative(ans.q2).isDefined && isNegative(ans.q2).get)
		cleanedYesQ1 > cleanedNoQ1 && cleanedYesQ2 > cleanedNoQ2
	}


	def buildAndAskQuestion(pdfFile: File, snippetFile: File, permutationId: Long): List[CsvAnswer] = {

		val base64Image = Utils.getBase64String(snippetFile)
		val permutation = dao.getPermutationById(permutationId).get
		val ballotHtmlPage: NodeSeq = createHtmlPage(base64Image, permutation.methodOnTop, permutation.relativeHeightTop, permutation.relativeHeightBottom)
		val pdfInputStream: InputStream = new FileInputStream(pdfFile)
		val pdfBinary = Stream.continually(pdfInputStream.read).takeWhile(-1 !=).map(_.toByte).toArray

		val contentType = new MimetypesFileTypeMap().getContentType(pdfFile.getName)

		val properties = new BallotProperties(Batch(UUID.randomUUID()),
			List(Asset(pdfBinary, contentType, pdfFile.getName)), 1, 50, permutationId)

		val answers: List[HTMLQueryAnswer] = askQuestion(0, HTMLQuery(ballotHtmlPage), properties, List.empty[HTMLQueryAnswer])

		answers.map(a => CsvAnswer(a.answers.get("isRelated"), a.answers.get("isCheckedBefore"), a.answers.get("confidence").get.toInt, a.answers.get("descriptionIsRelated").get))
	}

	def parseAnswer(answer: HTMLQueryAnswer): Option[Boolean] = {
		val likert = answer.answers.get("confidence").get.toInt
		if (likert >= LIKERT_VALUE_CLEANED_ANSWERS) {
			if (answer.answers.get("isRelated").get.equalsIgnoreCase("yes") && answer.answers.get("isCheckedBefore").get.equalsIgnoreCase("yes")) {
				Some(true)
			}
			else {
				Some(false)
			}
		} else {
			None
		}
	}

	def askQuestion(it: Int, query: HTMLQuery, properties: BallotProperties, sofar: List[HTMLQueryAnswer]): List[HTMLQueryAnswer] = {
		if (it < ANSWERS_PER_QUERY) {
			try {
				ballotPortalAdapter.processQuery(query, properties) match {
					case ans: Option[HTMLQueryAnswer] => {
						if (ans.isDefined) {
							logger.info(s"Answer: ${ans.get.answers.mkString("\n- ")}")
							askQuestion(it + 1, query, properties, sofar ::: List[HTMLQueryAnswer](ans.get))
						} else {
							logger.info("Error while getting the answer. The query may contain some errors.")
							sofar
						}
					}
				}
			}
			catch {
				case e: Throwable => {
					logger.error("There was a problem with the query engine", e)
					List.empty[HTMLQueryAnswer]
				}
			}
		} else {
			sofar
		}
	}

	def writeCSVReport() = {
		val writer = new PrintWriter(new File(RESULT_CSV_FILENAME))

		writer.write("snippet,yes answers,no answers,cleaned yes,cleaned no,yes answers,no answers,cleaned yes,cleaned no,feedbacks,firstExclusion,secondExclusion\n")

		val results = dao.allAnswers.groupBy(g => {
			dao.getAssetFileNameByQuestionId(g.questionId).get
		}).map(answersForSnippet => {

			val hints = dao.getPermutationIdByQuestionId(answersForSnippet._2.head.questionId).get
			val allPermutationsDisabledByActualAnswer = dao.getAllPermutationsWithStateEquals(hints).filterNot(f => f.excluded_step == 0)
			val snippetName = answersForSnippet._1
			val aa: List[Answer] = dao.getAllAnswersBySnippet(snippetName)

			val cleanFormatAnswers: List[Map[String, String]] = aa.map(a => Json.parse(a.answerJson).as[JsObject].fields.map(field => field._1 -> field._2.toString().replaceAll("\"", "")).toMap)
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

		writer.append(results.mkString("\n"))
		writer.close()
	}

	def isPositive(answer: Option[String]): Option[Boolean] = {
		if (answer.isDefined) {
			Some(answer.get.equalsIgnoreCase("yes"))
		} else {
			None
		}
	}

	def isNegative(answer: Option[String]): Option[Boolean] = {
		if (answer.isDefined) {
			Some(answer.get.equalsIgnoreCase("no"))
		} else {
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
				(such as ANOVA) to compare groups of data and derive findings. These
				<b>Statistical methods</b>
				in general require some
				<b>prerequisites</b>
				to be satisfied before being applied to data. Please have a look at the text-snipplet below. You'll find a
				<span style="background-color:#FFFF00;">statistical method marked in yellow</span>
				and a
				<span style="background-color:#00FF00;">prerequisite marked in green.</span>
			</p>

			<div class="row" style="display: table;">
				<div class="col-md-2" style="float: none;display: table-cell;vertical-align: top;"></div>
				<div class="col-md-8" style="float: none;display: table-cell;vertical-align: top;">
					<div id="imgContainer" style="width:100%; height:350px; border:1px solid black;overflow:auto;">
						<img id="snippet" src={imageBase64Format} width="100%"></img>
					</div>
				</div>
				<div class="col-md-2" style="float: none;display: table-cell;vertical-align: top;">
					<div id="snippetButtons">

						<button type="button" id="top" class="btn btn-info" style="width:200px;" aria-hidden="true">
							<span class="glyphicon glyphicon-arrow-up"></span>{if (isMethodOnTop) {
							"Scroll to Method"
						} else {
							"Scroll to Prerequisite"
						}}
						</button>
						<br/>
						<br/>
						<button type="button" id="bottom" class="btn btn-info" style="width:200px;" aria-hidden="true">
							<span class="glyphicon glyphicon-arrow-down"></span>{if (isMethodOnTop) {
							"Scroll to Prerequisite"
						} else {
							"Scroll to Method"
						}}
						</button>
					</div>
				</div>
			</div>
			<br/>

			<div id="assets">
				If you would like to read more context in order to give better and more accurate answers, you can browse the PDF file by clicking
				<img src="http://www.santacroceopera.it/Images/Pages/PDF.png"></img>
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

				<hr style="width:100%"/>
				<p>
					Please select the number below that best represents how certain you feel about the answer you have provided before.
				</p>

				<div class="form-group" style="width:100%;">
					<label class="col-sm-6 control-label">Not certain at all</label>
					<label class="col-sm-6 control-label" style="text-align: right">Absolutely certain</label>
				</div>

				<div class="form-group" style="width:100%;">
					<div class="col-sm-12">
						<div class="well">
							<input id="ex1" data-slider-id="ex1Slider" type="text" name="confidence" data-slider-min="1" data-slider-max="7" data-slider-step="1" data-slider-value="1" data="confidence: '1'" value="1" style="display: none;width:100%;">
							</input>
						</div>
					</div>
				</div>

				<hr style="width:100%"/>
				<input type="submit" class="btn btn-large btn-primary" style="width:150px;float:right;" value="Submit Answer"/>

			</form>
			<br/>
			<br/>
			<script type="text/javascript">
				{scala.xml.PCData(
				"""$('#ex1').slider({
                tooltip: 'always',
                  formatter: function(value) {
                  return value;
                }
              });
				"""
			)}
			</script>

			<script type="text/javascript">
				{scala.xml.PCData( """
          $('#top').click(function() {
            $('#imgContainer').animate({
              scrollTop: $('#imgContainer')[0].scrollHeight*""" + (relativeHeightTop - 2.0) + """/100
              }, 1000);
          });

          $('#bottom').click(function() {
            $('#imgContainer').animate({
              scrollTop: $('#imgContainer')[0].scrollHeight*""" + (relativeHeightBottom - 2.0) + """/100
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
			<script type="text/javascript">
				{scala.xml.PCData( """

        $('#ex1').slider({
          tooltip: 'always',
          formatter: function(value) {
            return value;
          }
        });

        var step = 10;
          var scrolling = false;

          $('#up').bind('click', function(event) {
            event.preventDefault();
            $('#imgContainer').animate({
              scrollTop: '-=' + step + 'px'
            });
          }).bind('mouseover', function(event) {
            scrolling = true;
            scrollContent('up');
          }).bind('mouseout', function(event) {
            scrolling = false;
          });


          $('#down').bind('click', function(event) {
            event.preventDefault();
            $('#imgContainer').animate({
              scrollTop: '+=' + step + 'px'
            });
          }).bind('mouseover', function(event) {
            scrolling = true;
            scrollContent('down');
          }).bind('mouseout', function(event) {
            scrolling = false;
          });

          function scrollContent(direction) {
            var amount = (direction === 'up' ? '-=1px' : '+=1px');
            $('#imgContainer').animate({
              scrollTop: amount
            }, 10, function() {
              if (scrolling) {
                scrollContent(direction);
              }
            });
          }
								   """)}
			</script>
		</div>
	}

}