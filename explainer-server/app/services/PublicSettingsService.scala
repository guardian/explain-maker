package services

import javax.inject.{Inject, Singleton}

import com.gu.pandomainauth.PublicSettings
import com.ning.http.client.AsyncHttpClient
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import config.Config
import dispatch.Http
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}


@Singleton
class PublicSettingsService @Inject() (appLifecycle: ApplicationLifecycle, config: Config) {

  implicit val httpClient = Http(new AsyncHttpClient())
  // This code is called when the application starts.
  val publicSettings = new PublicSettings(config.pandaDomain, { // Config.domain
    case Success(settings) =>
      Logger.debug("successfully updated pan-domain public settings")
    case Failure(err) =>
      Logger.warn("failed to update pan-domain public settings", err)
    })

  publicSettings.start()
  appLifecycle.addStopHook { () =>
    publicSettings.stop()
    Future.successful(())
  }
}

