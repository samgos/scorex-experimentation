import java.net.InetSocketAddress
import java.io.File

import scala.concurrent.duration._
import scorex.core.network.message.{SyncInfoMessageSpec, _}
import akka.actor.{ActorRef, Props}
import scorex.core.{NodeViewHolder, PersistentNodeViewModifier}
import scorex.core.api.http._
import scorex.core.settings._
import scorex.core.app._
import scorex.core.consensus.{History, HistoryReader, SyncInfo}
import scorex.core.network.peer.PeerManagerRef
import scorex.core.network.{NetworkControllerRef, NodeViewSynchronizer, NodeViewSynchronizerRef}
import scorex.core.transaction.{BoxTransaction, MemoryPool, MempoolReader}
import scorex.core.transaction.state.{MinimalState, Secret}
import scorex.core.transaction.wallet.Vault
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.utils.NetworkTimeProviderSettings
import scorex.core.utils._
import scorex.core.transaction.box.Box
import scorex.core.utils.NetworkTimeProvider


class zInit(val settingsFilename: String) extends Application {

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

  val applicationName = "scorex-experimentation"
  val applicationVersion = Version(0, 1, 0)
  val fileName: String = settingsFilename
  val fileKeystore = new File(fileName + "-key")
  val fileBody = new File(fileName)
  val timeOption = Option(10000 seconds)
  val timeOut = 10000 seconds

  val socketAddress = new InetSocketAddress("localhost",8080)
  val byteArray =  Array(192, 168, 1, 1).map(_.toByte)

  val networkConfig = new NetworkSettings(
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

  val timeConfig = new NetworkTimeProviderSettings("experiment", 25 seconds, 1000 seconds)
  val walletConfig = new WalletSettings(ByteStr(byteArray), "experiment", fileKeystore)
  val apiConfig = new RESTApiSettings(socketAddress, None, None, 10 seconds)

  override implicit val settings: ScorexSettings = new ScorexSettings(fileBody, fileBody, networkConfig, apiConfig, walletConfig, timeConfig)

  override val timeProvider = new NetworkTimeProvider(settings.ntp)

  override val additionalMessageSpecs: Seq[MessageSpec[_]] = Seq(ModifiersSpec, PeersSpec)

  val messageHandler = new MessageHandler(additionalMessageSpecs)

  override val peerManagerRef = PeerManagerRef(settings, timeProvider)

  override val networkControllerRef: ActorRef = NetworkControllerRef(
    networkConfig, messageHandler ,
    upnp, peerManagerRef, timeProvider)

  override val apiRoutes = Seq(PeersApiRoute(peerManagerRef, networkControllerRef, apiConfig), UtilsApiRoute(apiConfig))

  override val nodeViewHolderRef: ActorRef = actorSystem.actorOf(Props(classOf[NodeViewHolder[TX, PMOD]], settings))

  override val nodeViewSynchronizer: ActorRef = actorSystem.actorOf(Props(new NodeViewSynchronizer[TX, SI, SIS, PMOD, HR, MP]
  (networkControllerRef, nodeViewHolderRef, this, settings.network, timeProvider)))

  override val swaggerConfig: String = "experiment"

    println("initialised!")

}
