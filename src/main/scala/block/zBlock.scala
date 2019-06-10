package block

import transaction.{zInputSerializer, zTransaction}
import scorex.util.serialization.{Reader, Writer}
import scorex.crypto.hash.{ Whirlpool}
import scorex.core.serialization.ScorexSerializer
import scorex.core.block.Block.{ Timestamp, Version }
import scorex.core.{ModifierTypeId, bytesToId, idToBytes}
import scorex.core.block.Block
import scorex.util.ModifierId

case class zBlock(zTransactions: Seq[zTransaction], zHashParent: ModifierId, zTarget: Long, zNonce: Long, zVersion: Version, zTimestamp: Long)
extends Block[zTransaction] {
  override val modifierTypeId: ModifierTypeId = zBlock.zBlockModifier
  override val transactions = zTransactions
  override val parentId = zHashParent
  override val timestamp = zTimestamp
  override val version = zVersion
  val cipher = Whirlpool(zBlockSerializer.toBytes(this))
  override val id: ModifierId = bytesToId(cipher)
}

object zBlock {
  val zBlockModifier: ModifierTypeId = ModifierTypeId @@ 10.toByte
}

object zBlockSerializer extends ScorexSerializer[zBlock] {

  override def serialize(zObject: zBlock, zWriter: Writer): Unit = {
    zWriter.putInt(zObject.transactions.size)
    zObject.zTransactions.foreach(tx => zInputSerializer.serialize(tx, zWriter))
    zWriter.putBytes(idToBytes(zObject.zHashParent))
    zWriter.putLong(zObject.zTimestamp)
    zWriter.putLong(zObject.zTarget)
    zWriter.putLong(zObject.zNonce)
    zWriter.put(zObject.zVersion)
  }

  override def parse(zReader: Reader): zBlock = {
    val txStack = zReader.getInt()
    val zTxs = (0 until txStack) map (_ => zInputSerializer.parse(zReader))
    val zHashParent = bytesToId(zReader.getBytes(32))
    val zTarget = zReader.getLong()
    val zNonce = zReader.getLong()
    val zTimestamp = zReader.getLong()
    val zVersion = zReader.getByte()
    zBlock(zTxs, zHashParent, zTarget, zNonce, zVersion, zTimestamp)
  }

}

