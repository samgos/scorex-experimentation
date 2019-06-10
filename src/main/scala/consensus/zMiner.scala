package consensus

import scorex.core.network.NodeViewSynchronizer.ReceivableMessages.{ChangedHistory, ChangedMempool, SemanticallySuccessfulModifier}
import scorex.core.NodeViewHolder.ReceivableMessages.LocallyGeneratedModifier
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import scorex.core.utils.NetworkTimeProvider
import core.{zBlockchain, zMempool}
import scala.concurrent.duration._
import consensus.zMiner.MineBlock
import scorex.util.ScorexLogging
import scala.math.BigInt
import block.zBlock

class zMiner(zRef: ActorRef, zTime: NetworkTimeProvider) extends Actor with ScorexLogging {

    override def preStart(): Unit = {
      context.system.eventStream.subscribe(self, classOf[ChangedMempool[_]])
      context.system.eventStream.subscribe(self, classOf[ChangedHistory[_]])
    }

  var currentMempool: zMempool = new zMempool
  var currentCandidate: zBlock = constructNewBlock(zBlockchain.GenesisBlock)

    override def receive: Receive = {
      case ChangedHistory(h: zBlockchain@unchecked) =>
        context.system.scheduler.scheduleOnce(30.second) {
          currentCandidate = constructNewBlock(h.bestBlock)
          self ! MineBlock(currentCandidate.zNonce + 1)
        }

      case ChangedMempool(pool: zMempool) =>
        currentMempool = pool

      case MineBlock(newNonce) =>
        val newBlock = currentCandidate.copy(zNonce = newNonce)
        if (zMiner.correctWorkDone(newBlock)) {
          log.info(s"New block ${newBlock.encodedId} found")
          zRef ! LocallyGeneratedModifier(newBlock)
        } else {
          self ! MineBlock(newNonce + 1)
        }

      case m => log.warn(s"Unexpected message $m")
    }

    private def constructNewBlock(zParent: zBlock): zBlock = {
      val activeMempool = currentMempool.take(1)
      activeMempool.foreach(tx => currentMempool.remove(tx))
      val targetBlock = zParent.zTarget
      zBlock(
        activeMempool,
        zParent.id,
        targetBlock,
        0,
        0: Byte,
        zTime.time())
    }

  }

  object zMiner {

    case class MineBlock(zNonce: Long)
    val MaxTarget: Long = Long.MaxValue

    private def realDifficulty(zTarget: zBlock): BigInt = MaxTarget / BigInt(1, zTarget.cipher)

    def correctWorkDone(zBlock: zBlock): Boolean = {
      realDifficulty(zBlock) <= zBlock.zTarget
    }

  }


  object zMinerRef {

    def props(zRef: ActorRef, zTime: NetworkTimeProvider): Props = Props(new zMiner(zRef: ActorRef, zTime: NetworkTimeProvider))
    def apply(zRef: ActorRef, zTime: NetworkTimeProvider)(implicit system: ActorSystem): ActorRef = system.actorOf(props(zRef, zTime))

  }