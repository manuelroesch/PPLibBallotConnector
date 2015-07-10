package ch.uzh.ifi.pplibballotconnector.hcomp.ballot

import java.io.{InputStreamReader, BufferedReader}
import java.util

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pplibballotconnector.dao.BallotDAO
import ch.uzh.ifi.pplibballotconnector.persistence.DBSettings
import junit.framework.Assert
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.client.protocol.ClientContext
import org.apache.http.impl.client.{BasicCookieStore, DefaultHttpClient}
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.util.EntityUtils
import scala.xml.NodeSeq

/**
 * Created by mattia on 10.07.15.
 */
class BallotIntegrationTest {
  
  //@Test
  def test(): Unit ={
    DBSettings.initialize()

    val dao = new BallotDAO()

    val decoratedPortalAdapter = new IntegrationPortalAdapter()
    val ballotPortalAdapter = new BallotPortalAdapter(decoratedPortalAdapter, dao, "http://localhost:9000/")

    val ballotHtmlPage: NodeSeq =
      <div>
        <p>
          Some text
        </p>
        <hr style="width:100%"/>
        <p>
          Test question ?
          <br />
        </p>
        <form>
          <input type="submit" name="answer" value="Yes" style="width:100px;height:50px;" />
          <input type="submit" name="answer" value="No" style="width:100px;height:50px;"/>
        </form>
      </div>

    val query = HTMLQuery(ballotHtmlPage)
    val properties = new BallotProperties(Batch(), 1)

    ballotPortalAdapter.processQuery(query, properties) match {
      case ans : Option[HTMLQueryAnswer] => {
        Assert.assertEquals(ans.get.answers.get("answer").get, "Yes")
      }
    }

  }

}

class IntegrationPortalAdapter extends HCompPortalAdapter with AnswerRejection {
  override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {

    val freeTextQuery = query match {
      case p: FreetextQuery => p
      case _ => FreetextQuery(query.question)
    }
    Some(FreetextAnswer(freeTextQuery, simulateClientRequestsOnFrontend(query)))
  }

  def simulateClientRequestsOnFrontend(query: HCompQuery): String = {
    val httpClient = new DefaultHttpClient()
    val cookieStore = new BasicCookieStore()
    val httpContext = new BasicHttpContext()
    httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore)

    val questionURL = query.question.substring(0, query.question.indexOf("<br>"))
    logger.debug("Question URL: " + questionURL)

    //1. login
    val postLogin = new HttpPost("http://localhost:9000/login")
    val urlParameters = new util.ArrayList[NameValuePair]()
    urlParameters.add(new BasicNameValuePair("TurkerID", "integrationTest"))
    postLogin.setEntity(new UrlEncodedFormEntity(urlParameters))

    val loginResponse = httpClient.execute(postLogin, httpContext)
    EntityUtils.consumeQuietly(loginResponse.getEntity)

    //2. get question
    val getQuestion = new HttpGet(questionURL)
    val questionRequest = httpClient.execute(getQuestion, httpContext).getEntity
    val questionPage = EntityUtils.toString(questionRequest)
    EntityUtils.consumeQuietly(questionRequest)

    //3. send answer
    val questionId = questionPage.substring(questionPage.indexOf("name=\\\"questionId\\\" value=\\\"") + 28, questionPage.indexOf("\\\">"))
    logger.debug("Question id: " + questionId)
    val postAnswer = new HttpPost("http://localhost:9000/storeAnswer")
    val ansParameters = new util.ArrayList[NameValuePair]()
    ansParameters.add(new BasicNameValuePair("questionId", questionId))
    ansParameters.add(new BasicNameValuePair("answer", "Yes"))
    postAnswer.setEntity(new UrlEncodedFormEntity(ansParameters))

    val answerResponse = httpClient.execute(postAnswer, httpContext)
    val codePage = EntityUtils.toString(answerResponse.getEntity)
    EntityUtils.consumeQuietly(answerResponse.getEntity)
    httpClient.close()

    //4. get code
    codePage.substring(codePage.indexOf("<h1>") + 4, codePage.indexOf("</h1>"))
  }

  override def getDefaultPortalKey: String = "consolePortalAdapter"

  override def cancelQuery(query: HCompQuery): Unit = ???
}