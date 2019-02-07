package sync

import scorex.core.transaction.state.{MinimalState, PrivateKey25519Companion}
import transaction.{zOutput, zValue}

import scala.util.{Failure, Try}
import scorex.util.ScorexLogging
import scorex.core.VersionTag
import block.zBlock

case class zState(override val zVersion: VersionTag, zUtxo: IndexedSeq[zOutput]) extends MinimalState[zBlock, zState] with ScorexLogging {

  override def applyModifier(zMod: zBlock): Try[zState] = {
    var injectedInputs = zMod.zTransactions.flatMap(_.zInputs)
    zMod.zTransactions.foreach( _tx => {
      require(_tx.zInputs.size == _tx.zSigs.size, "not enough signatures")
      require(_tx.zOutputs.forall(_.zValue >= 0), "negative amount to transfer")
      val from: Seq[zOutput] = _tx.zInputs.map(_i => zUtxo.find(_.id sameElements _i).get)
      require(from.map(_.zValue.toLong).sum == _tx.zOutputs.map(_.zValue.toLong).sum, "Sum of inputs != sum of outputs")

      require(from.zip(_tx.zSigs).forall { case (input, proof) =>
        proof.isValid(input.zProposition, _tx.messageToSign)
      }, "proofs are incorrect")
    })

    val filteredData = zUtxo.filter(o => !injectedInputs.exists(_ sameElements o.id))

    zState(VersionTag @@ zMod.parentId, filteredData ++ zMod.zTransactions.flatMap(_.zOutputs))

  }

  override def rollbackTo(zVersion: VersionTag): Try[zState] = Failure(new Error("Not supported"))

  override def maxRollbackDepth: Int = 0

  override type NVCT = this.type

}

object zState {

  private val genesisState: Seq[zOutput] = Seq(
    zOutput(PrivateKey25519Companion.generateKeys("scorex is funky".getBytes())._2, zValue @@ 1000000L)
  )

  val empty: zState = zState(zVersion @@ zChain.zBlockGenesis.id, genesisState)
}

