package block

import transaction.{zInputSerializer, zTransaction}
import scorex.util.serialization.{Reader, Writer}
import scorex.crypto.hash.{Digest32, Digest64, Whirlpool}
import scorex.core.serialization.ScorexSerializer
import scorex.core.block.Block.{Timestamp, Version}
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
  val cipher: Digest64 = Whirlpool(zBlockSerializer.toBytes(this))
  override val id: ModifierId = bytesToId(cipher)
}

object zBlock {
  val zBlockModifier: ModifierTypeId = ModifierTypeId @@ 10.toByte
}

object zBlockSerializer extends ScorexSerializer[zBlock] {

  override def serialize(zObject: zBlock, zWriter: Writer): Unit = {
    zWriter.putInt(zObject.transactions.size)
    zObject.transactions.foreach(tx => zInputSerializer.serialize(tx, zWriter))
    zWriter.putBytes(idToBytes(zObject.parentId))
    zWriter.putLong(zObject.zTarget)
    zWriter.putLong(zObject.zNonce)
    zWriter.put(zObject.zVersion)
    zWriter.putLong(zObject.zTimestamp)
  }

  override def parse(zReader: Reader): zBlock = {
    val txStack = zReader.getInt()
    val zTxs = (0 until txStack) map (_ => zInputSerializer.parse(zReader))
    val zHashParent = bytesToId(zReader.getBytes(32))
    val zTarget = zReader.getLong()
    val zNonce = zReader.getLong()
    val zVersion = zReader.getByte()
    val zTimestamp = zReader.getLong()
    zBlock(zTxs, zHashParent, zTarget, zNonce, zVersion, zTimestamp)
  }

}

