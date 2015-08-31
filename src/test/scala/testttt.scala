import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.Answer
import org.junit.Test

/**
 * Created by mattia on 26.08.15.
 */
class testttt {

  @Test
  def y = {
    val x = List[Answer](Answer(0, 0, "{\"questionId\" : \"1\", \"isRelated\" : \"Yes\", \"isCheckedBefore\" : \"Yes\", \"confidence\" : \"6\", \"pdfFileName\" : \"-1301683083_bra00175_0_0.pdf\", \"descriptionIsRelated\" : \"123123\"}", true))
    val map : List[Map[String, String]] = x.map(a => a.answerJson.substring(1, a.answerJson.length - 1) .replaceAll("\"", "").split(",").toList.map(aa => aa.split(":").head.replaceAll(" ", "") -> aa.split(":")(1).substring(1)).toMap)
    println(map)
  }

}
