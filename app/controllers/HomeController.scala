package controllers

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject._

import org.apache.commons.codec.binary.Base64
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.ws._
import scala.concurrent.ExecutionContext.Implicits.global


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(ws: WSClient) extends Controller {

  private val channelSecret = "" /*secret*/

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }


  def line = Action.async(BodyParsers.parse.json){ data =>
    val key:SecretKeySpec = new SecretKeySpec(channelSecret.getBytes(),"HmacSHA256")
    val mac:Mac = Mac.getInstance("HmacSHA256")
    val source = data.body.toString.getBytes("UTF-8")
    val signature = Base64.encodeBase64String(mac.doFinal(source))

    val request: WSRequest = ws.url("https://api.line.me/v2/bot/message/reply")
    val headers = request.withHeaders(
      "Content-Type" -> "application/json; charset=UTF-8",
      "Authorization" -> "Bearer ENTER_ACCESS_TOKEN"
    )

    data.headers.apply("X-Line-Signature") match {
      case `signature` => {
        // シグネチャが一致
        val events = (data.body \\ "events").head
        val replayToken = (events \ "replyToken").get.as[String]

        val body = Json.parse(
          s"""
             |{
             |    "replyToken":"${replayToken}",
             |    "messages":[
             |        {
             |            "type":"text",
             |            "text":"Hello,World!"
             |        }
             |    ]
             |}
             """
            .stripMargin
        )
        headers.post(body).map {res => Ok }
      }
      case _ => {
        // シグネチャが一致しないがステータスコードOKを返す
        headers.post("").map {res => Ok}
      }
    }
  }

}
