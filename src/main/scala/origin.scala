import java.net.InetSocketAddress

import scorex.core.settings.RESTApiSettings
import scorex.core.app.Application
import scorex.core.app.Version
import scorex.core.api.http._
import scala.concurrent.duration._

import scorex.core.transaction._
import scorex.core.api.client._
import scorex.core.consensus._

abstract class origin (val settingsFilename: String) extends Application {

  val applicationName = "scorex-genesis"
  val appVersion = Version(0, 1, 0)
  val apiConfig = RESTApiSettings(new InetSocketAddress("localhost",8080), None, None, 10 seconds)

  val apiRoutes = Seq(
    PeersApiRoute(peerManagerRef, networkControllerRef, apiConfig),
    UtilsApiRoute(apiConfig),
  )

  println("initialised!")

}
