package controllers

import com.gu.scanamo._
import com.gu.scanamo.syntax.{set => _}
import models.ExplainerStore
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc._
import shared.Explainer

import scala.concurrent.Future

object ExplainEditorController extends Controller{

  val explainersTable = Table[Explainer]("explainers")

  implicit val jsonReader = (
    (__ \ 'txt).read[String](minLength[String](2)) and
    (__ \ 'done).read[Boolean]
  ).tupled

  def index = Action { implicit request =>
    Ok("Ok!")
  }

  def get(id: Long) = Action { implicit request =>
    Ok(views.html.explainEditor("Explain Editor"))
  }

  def all = Action.async{ implicit request =>
    ExplainerStore.all.map{ r =>
        Ok(views.html.explainList(r))
    }.recover{ case err =>
      InternalServerError(err.getMessage)
    }
  }

  def update(id: Long) = Action.async(parse.json){ implicit request =>
    val (fieldName, value) = request.body.as[JsObject].fields.head
    for {
       explainer <- ExplainerStore.update(id, Symbol(fieldName), value.as[JsString].value)
    } yield {
      explainer
      Ok
    }
  }

  def executeRequest(fn: (String, Boolean) => Future[Result])
    (implicit request: Request[JsValue]) = {
    request.body.validate[(String, Boolean)].map{
      case (txt, done) => {
        fn(txt, done)
      }
    }.recoverTotal{
      e => Future(BadRequest(e))
    }
  }


}
