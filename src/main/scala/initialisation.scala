import java.net.InetSocketAddress
import java.io.File

import scala.concurrent.duration._
import scorex.core.network.message._
import akka.actor.{ActorRef, Props}
import fundamentals._
import scorex.core.PersistentNodeViewModifier
import scorex.core.NodeViewHolder.CurrentView
import scorex.core.api.http._
import scorex.core.settings._
import scorex.core.app._
import scorex.core.consensus.{History, HistoryReader, SyncInfo}
import scorex.core.network.NodeViewSynchronizer
import scorex.core.network.message.MessageSpec
import scorex.core.transaction.{BoxTransaction, MemoryPool, MempoolReader}
import scorex.core.transaction.state.{MinimalState, Secret}
import scorex.core.transaction.wallet.Vault
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.utils.NetworkTimeProviderSettings
import scorex.core.utils._
import scorex.core.transaction.box.Box

 object fundamentals {

   type P <: PublicKey25519Proposition
   type PMOD <: PersistentNodeViewModifier
   type TX <: BoxTransaction[P, B]
   type B <: Box[P]
   type SI <: SyncInfo

   type HIS <: History[PMOD, _, _ <: History[PMOD, _, _]]
   type MP <: MemoryPool[TX, _ <: MemoryPool[TX, _]]
   type MS <: MinimalState[PMOD, _ <: MinimalState[_, _]]
   type VL <: Vault[TX, PMOD, _ <: Vault[TX, PMOD, _]]
   type SIS <: SyncInfoMessageSpec[SI]
   type HR <: HistoryReader[PMOD, SI]
   type MR <: MempoolReader[TX]

}

class initialisation(val settingsFilename: String) extends Application {

  val applicationName = "scorex-experimentation"
  val applicationVersion = Version(0, 1, 0)
  val fileName: String = settingsFilename
  val fileKeystore = new File(fileName + "-key")
  val fileBody = new File(fileName)
  val timeOption = Option(10000 seconds)
  val timeOut = 10000 seconds

  val socketAddress = new InetSocketAddress("localhost",8080)
  val byteArray =  Array(192, 168, 1, 1).map(_.toByte)

  val networkConfig = NetworkSettings(
    "experiment",
    timeOption,
    10000,
    true,
    Seq(socketAddress),
    socketAddress,
    5,
    timeOut,
    false,
    timeOption,
    timeOption,
    Option(socketAddress),
    timeOut,
    timeOut,
    1000,
    "0.10",
    "experiment",
    1000,
    10000,
    timeOut,
    timeOut,
    timeOut,
    timeOut,
    timeOption,
    timeOption )
  val timeConfig = NetworkTimeProviderSettings("experiment", 25 seconds, 1000 seconds)
  val walletConfig = WalletSettings(ByteStr(byteArray), "experiment", fileKeystore)
  val apiConfig = RESTApiSettings(socketAddress, None, None, 10 seconds)
  implicit val settings: ScorexSettings = ScorexSettings(fileBody, fileBody, networkConfig, apiConfig, walletConfig, timeConfig)
  override lazy val apiRoutes = Seq(PeersApiRoute(peerManagerRef, networkControllerRef, apiConfig), UtilsApiRoute(apiConfig))

  val additionalMessageSpecs: Seq[MessageSpec[_]] =  Seq( PeersSpec,  ModifiersSpec )
  val nodeViewSynchronizer : ActorRef =  actorSystem.actorOf(Props(classOf[NodeViewSynchronizer[TX, SI, SIS, PMOD,HR, MR]], this))
  val nodeViewHolderRef: ActorRef = actorSystem.actorOf(Props(classOf[CurrentView[HIS, MS, VL, MP]], this))
  val swaggerConfig: String = "experiment"

    println("initialised!")

}
