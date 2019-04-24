package sync

import scorex.core.transaction.state.{MinimalState}
import scala.util.{Failure, Try}
import scorex.core.VersionTag
import block.zBlock

case class zState(override val version: VersionTag) extends MinimalState[zBlock, zState] {

  override def applyModifier(mod: zBlock): Try[zState] = Try {
    zState(VersionTag @@ mod.parentId)
  }

  override def rollbackTo(version: VersionTag): Try[zState] = Failure(new Error("Not supported"))
  override def maxRollbackDepth: Int = 0
  override type NVCT = this.type

}


object zState {
  val empty: zState = zState(VersionTag @@ zBlockchain.zGenesis.zId)
}
