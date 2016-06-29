package controllers

import models.TaskMemStore.InsufficientStorageException
import models.{ExplainerStore, TaskModel}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc._
import shared.{Explainer, Task}
import upickle.default._
import com.gu.scanamo._
import com.gu.scanamo.syntax.{set => _, _}

import scala.concurrent.Future
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType._

object ExplainEditorController extends Controller{

  val explainersTable = Table[Explainer]("explainers")

  implicit val jsonReader = (
    (__ \ 'txt).read[String](minLength[String](2)) and
    (__ \ 'done).read[Boolean]
  ).tupled

  def index = Action { implicit request =>
    Ok(views.html.todo("TODO"))
  }

  def get(id: Long) = Action { implicit request =>
    Ok(views.html.explainEditor("Explain Editor"))
  }


  def all = Action.async{ implicit request =>
    TaskModel.store.all.map{ r =>
      Ok(write(r))
    }.recover{ case err =>
      InternalServerError(err.getMessage)
    }
  }

  def create = Action.async(parse.json){ implicit request =>
    val fn = (txt: String, done: Boolean) =>
      TaskModel.store.create(txt, done).map{ r =>
        Ok(write(r))
      }.recover{
        case e: InsufficientStorageException => InsufficientStorage(e)
        case e: Throwable => InternalServerError(e)
      }
    executeRequest(fn)
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

  def delete(id: Long) = Action.async{ implicit request =>
    TaskModel.store.delete(id).map{ r =>
      if(r) Ok else BadRequest
    }.recover{ case e => InternalServerError(e)}
  }

  def clear = Action.async{ implicit request =>
    TaskModel.store.clearCompletedTasks.map{ r =>
      Ok(write(r))
    }.recover{ case e => InternalServerError(e)}
  }

}
