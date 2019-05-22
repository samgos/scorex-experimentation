import akka.actor.{ActorRef, Props}
import api.zApi
import block.zBlock
import consensus.zMiner.MineBlock
import consensus.zMinerRef
import sync.{zRef, zSync, zSyncMessageSpec, zView}
import scorex.core.api.http.{ApiRoute, NodeViewApiRoute, PeersApiRoute, UtilsApiRoute}
import scorex.core.app.Application
import scorex.core.network.PeerFeature
import scorex.core.network.message.MessageSpec
import scorex.core.settings.ScorexSettings
import transaction.zTransaction

import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps

class zInit(configPath: String) extends {
  override implicit val settings: ScorexSettings = ScorexSettings.read(Some(configPath))
  override protected val features: Seq[PeerFeature] = Seq()
} with Application {
  override type TX = zTransaction
  override type PMOD = zBlock
  override type NVHT = zSync

  override protected lazy val additionalMessageSpecs: Seq[MessageSpec[_]] = Seq(zSyncMessageSpec)

  override val nodeViewHolderRef: ActorRef = zRef(settings, timeProvider)

  override val nodeViewSynchronizer: ActorRef =
    actorSystem.actorOf(Props(new zView(networkControllerRef, nodeViewHolderRef,
      zSyncMessageSpec, settings.network, timeProvider)))

  override val swaggerConfig: String = Source.fromResource("api.yaml").getLines.mkString("\n")

  override val apiRoutes: Seq[ApiRoute] = Seq(
    UtilsApiRoute(settings.restApi),
    zApi(settings.restApi, nodeViewHolderRef),
    PeersApiRoute(peerManagerRef, networkControllerRef, timeProvider, settings.restApi)
  )

  if (settings.network.nodeName.contains("mining-node")) {
    val miner = zMinerRef(nodeViewHolderRef, timeProvider)
    actorSystem.scheduler.scheduleOnce(5.minute) {
      miner ! MineBlock(0)
    }
  }
}

object zInit {

  def main(args: Array[String]): Unit = {
    new zInit(args.headOption.getOrElse("src/main/resources/node1.conf")).run()
  }
}