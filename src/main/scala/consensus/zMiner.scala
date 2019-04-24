package consensus

import scorex.core.network.NodeViewSynchronizer.ReceivableMessages.{ChangedMempool, SemanticallySuccessfulModifier}
import scorex.core.NodeViewHolder.ReceivableMessages.LocallyGeneratedModifier
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import scorex.core.utils.NetworkTimeProvider
import scala.concurrent.duration._
import consensus.zMiner.MineBlock
import scorex.util.ScorexLogging
import scala.math.BigInt
import core.zMempool
import block.zBlock

class zMiner(zRef: ActorRef, zTime: NetworkTimeProvider) extends Actor with ScorexLogging {

    override def preStart(): Unit = {
      context.system.eventStream.subscribe(self, classOf[SemanticallySuccessfulModifier[_]])
      context.system.eventStream.subscribe(self, classOf[ChangedMempool[_]])
    }

    var currentCandidate: zBlock = constructNewBlock(BDBlockchain.GenesisBlock)
    var currentMempool: zMempool = new zMempool

    override def receive: Receive = {
      case SemanticallySuccessfulModifier(mod: zBlock@unchecked) if mod.isInstanceOf[zBlock] =>
        currentCandidate = constructNewBlock(mod)

      case MineBlock(newNonce) =>
        val newBlock = currentCandidate.copy(nonce = newNonce)
        if (zMiner.correctWorkDone(newBlock)) {
          log.info(s"New block ${newBlock.encodedId} found")
          viewHolderRef ! LocallyGeneratedModifier(newBlock)
        }
        context.system.scheduler.scheduleOnce(1.minute) {
          self ! MineBlock(newNonce + 1)
        }

      case ChangedMempool(zPool: zMempool) => currentMempool = zPool

      case m => log.warn(s"Unexpeted message $m")
    }

    private def constructNewBlock(zParent: zBlock): zBlock = {
      val activeMempool = currentMempool.take(1)
      val targetBlock = zParent.currentTarget
      BDBlock(
        activeMempool,
        zParent.id,
        targetBlock,
        0,
        0: Byte,
        timeProvider.time())
    }

  }

  object zMiner {

    case class MineBlock(nonce: Long)
    val MaxTarget: Long = Long.MaxValue

    private def realDifficulty(zTarget: zBlock): BigInt = MaxTarget / BigInt(1, zTarget.hash)

    def correctWorkDone(zTarget: zBlock): Boolean = {
      realDifficulty(zTarget) <= zTarget.currentTarget
    }

  }


  object zMinerRef {

    def apply(zRef: ActorRef, zTime: NetworkTimeProvider)(implicit system: ActorSystem): ActorRef = system.actorOf(props(zRef, zTime))
    def props(zRef: ActorRef, zTime: NetworkTimeProvider): Props = Props(new zMiner(zRef: ActorRef, zTime: NetworkTimeProvider))

  }