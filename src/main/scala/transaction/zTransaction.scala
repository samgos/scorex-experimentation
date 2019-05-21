package transaction

import block.zBlockSerializer
import scorex.core.transaction.proof.{Signature25519, Signature25519Serializer}
import scorex.util.serialization.{Reader, VLQByteBufferWriter, Writer}
import scorex.core.serialization.ScorexSerializer
import scorex.core.transaction.Transaction
import scorex.util.ByteArrayBuilder


case class zTransaction(zInputs: IndexedSeq[zHash], zOutputs: IndexedSeq[zOutput], zSigs: IndexedSeq[Signature25519])
extends Transaction {
  override val messageToSign: Array[Byte] = {
    var coSigner = new VLQByteBufferWriter(new ByteArrayBuilder())
    zInputSerializer.serializeNonSig(this, coSigner)
    coSigner.result().toBytes
    }
  }

object zInputSerializer extends ScorexSerializer[zTransaction] {

  def serializeNonSig(zObject: zTransaction, zWriter: Writer): Unit = {
    zWriter.putInt(zObject.zInputs.size)
    zObject.zInputs.foreach{ i =>
      zWriter.putBytes(i)
    }
    zWriter.putInt(zObject.zOutputs.size)
    zObject.zOutputs.foreach{ o =>
      zOutputSerializer.serialize(o, zWriter)
    }

  }

  override def serialize(zObject: zTransaction, zWriter: Writer): Unit = {
    serializeNonSig(zObject, zWriter)
    zWriter.putInt(zObject.zSigs.size)
    zObject.zSigs.foreach{ s =>
      Signature25519Serializer.toBytes(s)
    }
  }

  override def parse(zReader: Reader): zTransaction = {
    val inputSize = zReader.getInt()
    val zInput = (0 until inputSize) map { _ =>
      zHash @@ zReader.getBytes(32)
    }
    val outputSize = zReader.getInt()
    val zOutput = (0 until outputSize) map { _ =>
      zOutputSerializer.parseBytes(zReader.getBytes(32)).get
    }
    val sigSize = zReader.getInt()
    val zSig = (0 until sigSize) map { _ =>
      Signature25519Serializer.parseBytes(zReader.getBytes(32)).get
    }
    zTransaction(zInput, zOutput, zSig)
  }

}


