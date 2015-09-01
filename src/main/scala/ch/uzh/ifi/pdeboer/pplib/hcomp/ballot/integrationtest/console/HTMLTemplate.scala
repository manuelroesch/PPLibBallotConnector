package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.integrationtest.console

import scala.xml.NodeSeq

/**
 * Created by mattia on 01.09.15.
 */
object HTMLTemplate {

  def createPage(imageBase64Format: String, isMethodOnTop: Boolean, baseURL: String, relativeHeightTop: Double = 0, relativeHeightBottom: Double = 0): NodeSeq = {
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

      <form action={baseURL + "storeAnswer"} method="GET" onsubmit="return checkFeedbackForm()">
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
		  	var snippetHeight = $('#snippet').height();
	 		var containerHeight = $('#imgContainer').height();
            //if($('#snippet').height() < $('#imgContainer').height()){
			if(Math.min(snippetHeight, containerHeight) == snippetHeight) {
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
