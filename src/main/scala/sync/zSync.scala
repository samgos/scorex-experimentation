package sync

import scorex.util.{ ModifierId, bytesToId, idToBytes }
import scorex.core.network.message.SyncInfoMessageSpec
import scorex.util.serialization.{ Reader, Writer }
import scorex.core.serialization.Serializer
import scorex.core.consensus.SyncInfo

case class zSync(zId: Seq[ModifierId]) extends SyncInfo {
  override val startingPoints: ModifierId = zId.head
}

object zSyncSerializer extends Serializer[zSync] {

  override def serialize(zObject: zSync, zWriter: Writer): Unit = {
    zWriter.putInt(zObject.zId.size)
    zObject.zId.foreach(i => zWriter.putBytes(idToBytes(i)))
  }

  override def parse(zReader: Reader): zSync = {
    val idz = (0 until zReader.getInt()) map { _ =>
      bytesToId(zReader.getBytes(32))
    }
    zSync(idz)
  }

}

object BDSyncInfo {
  val idzStack = 100
}

object zSyncMessageSpec extends SyncInfoMessageSpec[zSync](zSyncSerializer.parseBytes)


