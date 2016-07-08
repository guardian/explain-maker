package actions

import com.gu.pandomainauth.model._
import com.gu.pandomainauth.{PanDomain, PublicKey, PublicSettings}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results._
import play.api.mvc.Security.AuthenticatedBuilder
import play.api.mvc._
import com.ning.http.client.AsyncHttpClient
import config.Config
import dispatch.Http

import scala.util.{Failure, Success}

object PanAuthenticationSettings {
  implicit val httpClient = Http(new AsyncHttpClient())

  val publicSettings = new PublicSettings(Config.pandaDomain, { // Config.domain
    case Success(settings) =>
      println("successfully updated pan-domain public settings")
    case Failure(err) =>
      println("failed to update pan-domain public settings", err)
  })

  def publicKey: Option[String] = publicSettings.publicKey
}


/**
  * These ActionFunctions serve as components that can be composed to build the
  * larger, more-generally useful pipelines in 'CommonActions'.
  *
  * https://www.playframework.com/documentation/2.5.x/ScalaActionsComposition
  */
object ActionRefiners {

  def userAuthStatusOptFor(req: RequestHeader): Option[AuthenticationStatus] = for {
    publicKey <- PanAuthenticationSettings.publicKey
    cookie <- req.cookies.get(PanAuthenticationSettings.publicSettings.assymCookieName)
  } yield PanDomain.authStatus(cookie.value, PublicKey(publicKey))


  // TODO Remove code used only by Lazy devs! Editorial tools team, feel free to get rid of this
  val FallbackUserForDevOnly = if (Config.stage=="DEV") Some(AuthenticatedUser(
    user = com.gu.pandomainauth.model.User("Dev", System.getProperty("user.name"), System.getProperty("user.name"), None),
    authenticatingSystem = System.getenv("HOSTNAME"),
    authenticatedIn = Set.empty,
    expires = Long.MaxValue,
    multiFactor = true
  )) else None

  object PandaAuthenticated extends AuthenticatedBuilder(req => userAuthStatusOptFor(req) match {
    case Some(Authenticated(u)) => Some(u)
    case _ => FallbackUserForDevOnly
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