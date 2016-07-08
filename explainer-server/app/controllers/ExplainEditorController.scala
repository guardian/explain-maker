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

class ExplainEditorController extends Controller {

  val explainersTable = Table[Explainer]("explainers")

  implicit val jsonReader = (
    (__ \ 'txt).read[String](minLength[String](2)) and
    (__ \ 'done).read[Boolean]
  ).tupled

  def index = Action { implicit request =>
    Ok("Ok!")
  }

  def get(id: String) = Action { implicit request =>
    Ok(views.html.explainEditor(id, "Explain Editor"))
  }

  def all = Action.async{ implicit request =>
    ExplainerStore.all.map{ r =>
        Ok(views.html.explainList(r))
    }.recover{ case err =>
      InternalServerError(err.getMessage)
    }
  }
}
