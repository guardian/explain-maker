package controllers

import javax.inject.Inject

import actions.AuthActions
import com.gu.contentatom.thrift.Atom
import com.gu.scanamo.syntax.{set => _}
import db.ExplainerDB
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import services.PublicSettingsService
import shared._
import shared.util.ExplainerAtomImplicits

class ExplainEditorController @Inject() (val publicSettingsService: PublicSettingsService) extends Controller with AuthActions with ExplainerAtomImplicits {


  def get(id: String) = PandaAuthenticated { implicit request =>
    Ok(views.html.explainEditor(id,request.user))
  }

  def all = PandaAuthenticated.async{ implicit request =>

    ExplainerDB.all.map{ r =>
        def sorting(e1: Atom, e2: Atom): Boolean = {
          val time1:Long = e1.contentChangeDetails.lastModified.map(_.date).getOrElse(0)
          val time2:Long = e2.contentChangeDetails.lastModified.map(_.date).getOrElse(0)
          time1 > time2
        }
        Ok(views.html.explainList(r.sortWith(sorting),request.user))
    }.recover{ case err =>
      InternalServerError(err.getMessage)
    }
  }
}
