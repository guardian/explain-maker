package actions

import javax.inject.Inject

import com.gu.pandomainauth.model._
import com.gu.pandomainauth.{PanDomain, PublicKey}
import play.api.mvc.Results._
import play.api.mvc.Security.AuthenticatedBuilder
import play.api.mvc._
import config.Config
import services.PublicSettingsService


trait AuthActions {

  val publicSettingsService: PublicSettingsService

  def userAuthStatusOptFor(req: RequestHeader): Option[AuthenticationStatus] = for {
    publicKey <- publicSettingsService.publicSettings.publicKey
    cookie <- req.cookies.get(publicSettingsService.publicSettings.assymCookieName)
  } yield PanDomain.authStatus(cookie.value, PublicKey(publicKey))
  
  class PandaAuthenticated @Inject() (config: Config) extends AuthenticatedBuilder(req => userAuthStatusOptFor(req) match {
    case Some(Authenticated(u)) => Some(u)
    case _ => None
  }, onUnauthorized = req => {

    val requestUrl = s"https://explainers.${config.pandaDomain}/${req.path}"
    val loginRedirect = Redirect(s"https://login.${config.pandaDomain}/login", Map("returnUrl" -> Seq(requestUrl)))
      userAuthStatusOptFor(req).map {
        case NotAuthorized(u) => Forbidden(s"Sorry, ${u.user.emailDomain} is not authorized to use this tool")
        case _ => loginRedirect
      }.getOrElse(loginRedirect)
    }
  )

}