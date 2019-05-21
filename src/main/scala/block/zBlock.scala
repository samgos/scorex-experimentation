package block

import transaction.{zInputSerializer, zTransaction}
import scorex.util.serialization.{Reader, Writer}
import scorex.crypto.hash.{ Whirlpool}
import scorex.core.serialization.ScorexSerializer
import scorex.core.block.Block.Version
import scorex.core.{ModifierTypeId, bytesToId, idToBytes}
import scorex.core.block.Block
import scorex.util.ModifierId

case class zBlock(zTransactions: Seq[zTransaction], zHashParent: ModifierId, zTarget: Long, zNonce: Long, zVersion: Version, zTime: Long)
extends Block[zTransaction] {
  override val modifierTypeId: ModifierTypeId = zBlock.zBlockModifier
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
    zWriter.putLong(zObject.zTarget)
    zWriter.putLong(zObject.zNonce)
    zWriter.putLong(zObject.zTime)
    zWriter.put(zObject.zVersion)
  }

  override def parse(zReader: Reader): zBlock = {
    val txStack = zReader.getInt()
    val zTxs = (0 until txStack) map (_ => zInputSerializer.parse(zReader))
    val zHashParent = bytesToId(zReader.getBytes(32))
    val zTarget = zReader.getLong()
    val zNonce = zReader.getLong()
    val zVersion = zReader.getByte()
    val zTime = zReader.getLong()
    zBlock(zTxs, zHashParent, zTarget, zNonce, zVersion, zTime)
  }

}

