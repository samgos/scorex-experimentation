package transaction

import scorex.core.transaction.box.proposition.{ PublicKey25519Proposition, PublicKey25519PropositionSerializer}
import scorex.util.serialization.{Reader, Writer}
import scorex.core.serialization.Serializer
import scorex.core.transaction.box.Box
import scorex.crypto.authds.ADKey
import scorex.crypto.hash.Whirlpool
import scorex.util.{bytesToId, idToBytes}

case class zOutput(zProposition: PublicKey25519Proposition, zValue: zValue) extends Box[PublicKey25519Proposition] {
  override val id: ADKey = ADKey !@@ Whirlpool(zOutputSerializer.toBytes(this))
}

object zOutputSerializer extends Serializer[zOutput] {

  override def serialize(zObject: zOutput, zWriter: Writer): Unit = {
    PublicKey25519PropositionSerializer.toBytes(zObject.zProposition)
    zWriter.putULong(zObject.zValue)
  }

  override def parse(zReader: Reader): zOutput = {
    zOutput( PublicKey25519PropositionSerializer.parseBytes(zReader.getBytes(32)), zValue @@ zReader.getULong())
  }

}