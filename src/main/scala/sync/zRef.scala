package sync

import akka.actor.{ActorRef, ActorSystem, Props}
import block.zBlock
import core.{zBlockchain, zMempool}
import scorex.core.NodeViewHolder
import scorex.core.settings.ScorexSettings
import scorex.core.utils.NetworkTimeProvider
import transaction.{zTransaction, zWallet}

class zRef(val scorexSettings: ScorexSettings,
                       timeProvider: NetworkTimeProvider)
  extends NodeViewHolder[zTransaction, zBlock] {

  override type SI = zSync
  override type HIS = zBlockchain
  override type MS = zState
  override type VL = zWallet
  override type MP = zMempool

  override def restoreState(): Option[(zBlockchain, zState, zWallet, zMempool)] = None

  override protected def genesisState: (zBlockchain, zState, zWallet, zMempool) = {
    // todo seed should come from another source
    val seed = scorexSettings.network.nodeName
    (zBlockchain.empty, zState.empty, zWallet(seed), zMempool.empty)
  }

}


object zRef {
  def props(settings: ScorexSettings,
            timeProvider: NetworkTimeProvider): Props =
    Props(new zRef(settings, timeProvider))

  def apply(settings: ScorexSettings,
            timeProvider: NetworkTimeProvider)
           (implicit system: ActorSystem): ActorRef =
    system.actorOf(props(settings, timeProvider))

}