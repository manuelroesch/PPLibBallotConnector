package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot


import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao.{BallotDAO, DAO}
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.{DBSettings, Permutation}
import junit.framework.Assert
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client.{BasicCookieStore, HttpClientBuilder}
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.util.EntityUtils
import org.junit.Test

import scala.xml.NodeSeq

/**
 * Created by mattia on 10.07.15.
 */
class BallotIntegrationTest {
  
  @Test
  def test(): Unit ={
    DBSettings.initialize()

    val dao = new BallotDAO()
    val countQuestions : Int = dao.countAllQuestions()
    val countAnswers : Int = dao.countAllAnswers()
    val countBatches : Int = dao.countAllBatches()

    val permutationId = dao.createPermutation(Permutation(0, "group", "method", "snippetPath", "pdfPath", methodOnTop = true, 0, 0, 0.0, 100.0))

    val decoratedPortalAdapter = new IntegrationPortalAdapter(dao)
    val ballotPortalAdapter = new BallotPortalAdapter(decoratedPortalAdapter, dao, "http://localhost:8081/")

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
    val properties : BallotProperties = new BallotProperties(Batch(1), List(Asset(Array.empty[Byte], "application/pdf", "empty filename")), permutationId)

    ballotPortalAdapter.processQuery(query, properties) match {
      case ans : Option[HTMLQueryAnswer] => {
        Assert.assertEquals(ans.get.answers.get("answer").get, "Yes")
        Assert.assertEquals(dao.countAllQuestions(),countQuestions+1)
        Assert.assertEquals(dao.countAllAnswers(), countAnswers+1)
        Assert.assertEquals(dao.countAllBatches(), countBatches+1)
      }
    }

  }

}

class IntegrationPortalAdapter(val dao: DAO = new BallotDAO()) extends HCompPortalAdapter with AnswerRejection {

  val httpClient = HttpClientBuilder.create().build()
  val httpContext = new BasicHttpContext()

  override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {

    val freeTextQuery = query match {
      case p: FreetextQuery => p
      case _ => FreetextQuery(query.question)
    }

    Some(FreetextAnswer(freeTextQuery, simulateClientRequestsOnFrontend(query, properties)))

  }

  def simulateClientRequestsOnFrontend(freeTextQuery: HCompQuery, properties: HCompQueryProperties, baseURL : String = "http://localhost:8081/") : String = {

    val actualProperties = properties match {
      case p: BallotProperties => p
      case _ => new BallotProperties(Batch(), List.empty[Asset], 1)
    }

    val cookieStore = new BasicCookieStore()
    httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore)

    val questionId = dao.getAllQuestions.filter(q => q.permutationId == actualProperties.permutationId).head.id
    val questionUUID = dao.getQuestionUUID(questionId)
    val link = baseURL + "showQuestion/" + questionUUID
    logger.debug("Question URL: " + link)


    //1. login to initialize the session
    performAndConsumeGetRequest("http://localhost:8081/login?TurkerID=integrationTest")

    //2. get question
    val questionPage = performAndConsumeGetRequest(link)

    //3. send answer
    logger.debug("Question id: " + questionId)

    val codePage = performAndConsumeGetRequest(s"http://localhost:8081/storeAnswer?questionId=${questionId.toString}&answer=Yes")

    httpClient.close()

    //4. get code
    codePage.substring(codePage.indexOf("<h1>") + 4, codePage.indexOf("</h1>"))
  }


  def performAndConsumePostRequest(url: String, params: java.util.ArrayList[NameValuePair]): String = {
    val request = new HttpPost(url)
    request.setEntity(new UrlEncodedFormEntity(params))
    val response = httpClient.execute(request, httpContext)
    val content = EntityUtils.toString(response.getEntity)
    EntityUtils.consumeQuietly(response.getEntity)
    content
  }

  def performAndConsumeGetRequest(url: String): String = {
    val request = new HttpGet(url)
    val response = httpClient.execute(request, httpContext).getEntity
    val content = EntityUtils.toString(response)
    EntityUtils.consumeQuietly(response)
    content
  }

  override def getDefaultPortalKey: String = "consolePortalAdapter"

  override def cancelQuery(query: HCompQuery): Unit = ???
}