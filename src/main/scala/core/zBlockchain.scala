package core

import java.util.Calendar

import scorex.core.consensus.{BlockChain, ModifierSemanticValidity}
import scorex.core.consensus.BlockChain.Score
import scorex.core.block.Block.{Timestamp, Version}
import scorex.core.utils.ScorexEncoding
import scorex.core.consensus.History._
import transaction.zTransaction
import scorex.core.ModifierId
import scorex.util.bytesToId
import consensus.zMiner
import block.zBlock
import scorex.core.validation.RecoverableModifierError
import sync.zSync

import scala.util.Try

case class zBlockchain(zBlocks: Map[Int, zBlock], zMap: Map[String, Int], zValidity: Map[zBlock, Boolean])
  extends BlockChain[zTransaction, zBlock, zSync, zBlockchain] with ScorexEncoding {

  def bestBlock: zBlock = zBlocks.maxBy(_._1)._2

  override def height(): Int = zBlocks.keys.max

  override def heightOf(zId: ModifierId): Option[Int] = zMap.get(zId)

  override def blockAt(zHeight: Int): Option[zBlock] = zBlocks.get(zHeight)

  override def children(zId: ModifierId): Seq[zBlock] = heightOf(zId).map(_ + 1).flatMap(blockAt).toSeq

  // TODO this is simplified version
  override def score(zInput: zBlock): Score = BigInt(heightOf(zInput).getOrElse(0))

  override def chainScore(): Score = score(bestBlock)

  override def append(zInput: zBlock): Try[(zBlockchain, ProgressInfo[zBlock])] = Try {
    val blockHeight = height() + 1
    val progressInfo = ProgressInfo(None, Seq.empty, Seq(zInput), Seq.empty)
    log.info(s"Appended block ${zInput.id} with height $blockHeight")
    (zBlockchain(zBlocks + (blockHeight -> zInput), zMap + (zInput.id -> blockHeight), zValidity), progressInfo)
  }

  override def reportModifierIsValid(zModifier: zBlock): zBlockchain = {
    zBlockchain(zBlocks, zMap, zValidity + (zModifier -> true))
  }

  override def reportModifierIsInvalid(zModifier: zBlock, zProgress: ProgressInfo[zBlock]): (zBlockchain, ProgressInfo[zBlock]) = {
    (zBlockchain(zBlocks - zMap(zModifier.encodedId), zMap - zModifier.encodedId, zValidity + (zModifier -> false)), ProgressInfo[zBlock](None, Seq.empty, Seq.empty, Seq.empty))
  }

  override def applicableTry(zInput: zBlock): Try[Unit] = Try {
    if (!zMiner.correctWorkDone(zInput)) throw new Error(s"Incorrect target for ${zInput.encodedId}")
    //TODO forks are not supported in this implementation
    if (bestBlock.id != zInput.parentId) throw new RecoverableModifierError(s"Incorrect parentId for ${zInput.encodedId}")
  }

  override def modifierById(zId: ModifierId): Option[zBlock] = zMap.get(zId).flatMap(i => zBlocks.get(i))

  override def isSemanticallyValid(zId: ModifierId): ModifierSemanticValidity = zMap.get(zId) match {
    case Some(_) => ModifierSemanticValidity.Valid
    case _ => ModifierSemanticValidity.Unknown
  }

  override def syncInfo: zSync = {
    zSync(lastBlockIds(zSync.idzStack))
  }

  override def compare(zOther: zSync): HistoryComparisonResult = {
    val theirIds = zOther.zId
    theirIds.reverse.find(id => contains(id)) match {
      case Some(common) =>
        val commonHeight = heightOf(common).get
        val theirTotalHeight = theirIds.indexWhere(_ sameElements common) + commonHeight
        val ourHeight = height()
        if (theirTotalHeight == ourHeight) {
          Equal
        } else if (theirTotalHeight > ourHeight) {
          Older
        } else {
          Younger
        }
      case _ => Unknown
    }
  }

  override type NVCT = zBlockchain

}

object zBlockchain {

  val startingOirgin = 1478164225796L

  val GenesisBlock: zBlock = zBlock(
    startingOirgin,
    Seq.empty,
    bytesToId(Array.fill(32)(0: Byte)),
    zMiner.MaxTarget,
    0,
    0: Byte,
    1517329800000L)

  val empty: zBlockchain = zBlockchain(Map(1 -> GenesisBlock), Map(GenesisBlock.encodedId -> 1), Map(GenesisBlock -> true))

}
