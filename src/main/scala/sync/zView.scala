package sync

import akka.actor.ActorRef
import block.{zBlock, zBlockSerializer}
import core.{zBlockchain, zMempool}
import sync.{zSync, zSyncMessageSpec}
import scorex.core.network.NodeViewSynchronizer
import scorex.core.settings.NetworkSettings
import scorex.core.transaction.Transaction
import scorex.core.utils.NetworkTimeProvider
import transaction.{zTransaction, zInputSerializer}

import scala.concurrent.ExecutionContext

class zView(networkControllerRef: ActorRef,
                             viewHolderRef: ActorRef,
                             syncInfoSpec: zSyncMessageSpec.type,
                             networkSettings: NetworkSettings,
                             timeProvider: NetworkTimeProvider)(implicit ex: ExecutionContext) extends
  NodeViewSynchronizer[zBlock, zSync, zSyncMessageSpec.type, zBlock,
    zBlockchain, zMempool](networkControllerRef, viewHolderRef, syncInfoSpec, networkSettings, timeProvider,
    Map(zBlock.zBlockModifier -> zBlockSerializer,  Transaction.ModifierTypeId -> zInputSerializer)
  )