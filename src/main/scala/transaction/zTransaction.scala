package transaction

import scorex.core.transaction.proof.{ Signature25519, Signature25519Serializer }
import scorex.util.serialization.{ Reader, VLQByteBufferWriter, Writer }
import scorex.core.serialization.Serializer
import scorex.core.transaction.Transaction
import scorex.util.ByteArrayBuilder


case class zTransaction(zInputs: IndexedSeq[zHash], zOutputs: IndexedSeq[zOutput], zSigs: IndexedSeq[Signature25519])
extends Transaction {
  override val messageToSign: Array[Byte] = {
    var coSigner = new VLQByteBufferWriter(new ByteArrayBuilder())
    zSerializer.serializeNoSignatures(this, Writer)
    Writer.result().toBytes
    }
  }

object zInputSerializer extends Serializer[zTransaction] {

  def serializeNonSig(zObject: zTransaction, zWriter: Writer): Unit = {
    zWriter.putInt(zObject.inputs.size)
    zObject.inputs.foreach{ i =>
      zWriter.putBytes(i)
    }
    zWriter.putInt(zObject.outputs.size)
    zObject.outputs.foreach{ o =>
      zOutputSerializer.serialize(o, zWriter)
    }

  }

  override def serialize(zObject: zTransaction, zWriter: Writer): Unit = {
    serializeNonSig(zObject, zWriter)
    zWriter.putInt(zObject.signatures.size)
    zObject.signatures.foreach{ s =>
      Signature25519Serializer.serialize(s, zWriter)
    }
  }

  override def parse(zReader: Reader): zTransaction = {
    val inputSize = zReader.getInt()
    val zInput = (0 until inputSize) map { _ =>
      zHash @@ zReader.getBytes(32)
    }
    val outputSize = zReader.getInt()
    val zOutput = (0 until outputSize) map { _ =>
      zOutputSerializer.parse(zReader)
    }
    val sigSize = zReader.getInt()
    val zSig = (0 until sigSize) map { _ =>
      Signature25519Serializer.parse(r)
    }
    zTransaction(zInput, zOutput, zSig)
  }

}


