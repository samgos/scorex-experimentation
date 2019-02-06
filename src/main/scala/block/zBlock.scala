package block

import transaction.{ zTransaction, zInputSerializer }
import scorex.util.serialization.{ Reader, Writer}
import scorex.crypto.hash.{ Whirlpool, Digest32 }
import scorex.util.{ bytesToId, idToBytes }
import scorex.core.serialization.Serializer
import scorex.core.block.Block.Version
import scorex.core.ModifierTypeId
import scorex.core.block.Block
import scorex.core.ModifierId

case class zBlock(zTransactions: Seq[zTransaction], zHashParent: ModifierId, zTarget: Long, zNonce: Long, zV: Version, zTime: Long)
extends Block[zTransaction] {
  override val modifierTypeId: ModifierTypeId = zBlock.zBlockModifier.type
  val cipher = Whirlpool(zBlockSerializer.toBytes(this))
  override val id: ModifierId = bytesToId(cipher)
}

object zBlock {
  val zBlockModifier: ModifierTypeId = ModifierTypeId @@ 10.toByte
}

object zBlockSerializer extends Serializer[zBlock] {

  override def serialize(zObject: zBlock, zWriter: Writer): Unit = {
    zWriter.putInt(zObject.transactions.size)
    zBlock.transactions.foreach(tx => zInputSerializer.serialize(tx, zWriter))
    zWriter.putBytes(idToBytes(zBlock.zHashParent))
    zWriter.putLong(zBlock.zTarget)
    zWriter.putLong(zBlock.zNonce)
    zWriter.putLong(zBlock.zTime)
    zWriter.put(zBlock.zV)
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

