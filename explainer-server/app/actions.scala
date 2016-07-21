package actions

import javax.inject.{Inject, Singleton}

import com.gu.pandomainauth.model._
import com.gu.pandomainauth.{PanDomain, PublicKey, PublicSettings}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results._
import play.api.mvc.Security.AuthenticatedBuilder
import play.api.mvc._
import com.ning.http.client.AsyncHttpClient
import config.Config
import dispatch.Http
import services.PublicSettingsService

import scala.util.{Failure, Success}

trait AuthActions {

  val publicSettingsService: PublicSettingsService

  def userAuthStatusOptFor(req: RequestHeader): Option[AuthenticationStatus] = for {
    publicKey <- publicSettingsService.publicSettings.publicKey
    cookie <- req.cookies.get(publicSettingsService.publicSettings.assymCookieName)
  } yield PanDomain.authStatus(cookie.value, PublicKey(publicKey))
  
  object PandaAuthenticated extends AuthenticatedBuilder(req => userAuthStatusOptFor(req) match {
    case Some(Authenticated(u)) => Some(u)
    case _ => None
  }, onUnauthorized = req => {
    val requestUrl = s"https://${req.host}/${req.path}"
    val loginRedirect = Redirect(s"https://login.${Config.pandaDomain}/login", Map("returnUrl" -> Seq(requestUrl)))
      userAuthStatusOptFor(req).map {
        case NotAuthorized(u) => Forbidden(s"Sorry, ${u.user.emailDomain} is not authorized to use this tool")
        case _ => loginRedirect
      }.getOrElse(loginRedirect)
    }
  )

}