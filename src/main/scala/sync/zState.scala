package sync

import scorex.core.transaction.state.MinimalState

import scala.util.{Failure, Try}
import scorex.core.VersionTag
import core.zBlockchain
import block.zBlock
import com.sun.jdi.Value
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.util.ScorexLogging
import transaction.{zOutput, zValue}


case class zState(override val version: VersionTag, utxo: Seq[zOutput]) extends MinimalState[zBlock, zState]
  with ScorexLogging {
  override def applyModifier(mod: zBlock): Try[zState] = Try {
    log.info(s"Apply block ${mod.id} with ${mod.transactions.size} transactions to state of version $version")

    val inputs = mod.transactions.flatMap(_.zInputs)
    mod.transactions.foreach { tx =>
      require(tx.zInputs.size == tx.zSigs.size, "not enough signatures")
      require(tx.zOutputs.forall(_.value >= 0), "negative amount to transfer")
      val from: Seq[zOutput] = tx.zInputs.map(i => utxo.find(_.id sameElements i).get)
      require(from.map(_.value.toLong).sum == tx.zOutputs.map(_.value.toLong).sum, "Sum of inputs != sum of outputs")

      require(from.zip(tx.zSigs).forall { case (input, proof) =>
        proof.isValid(input.proposition, tx.messageToSign)
      }, "proofs are incorrect")
    }

    val filtered = utxo.filter(o => !inputs.exists(_ sameElements o.id))

    zState(VersionTag @@ mod.id, filtered ++ mod.transactions.flatMap(_.zOutputs))
  }

  override def rollbackTo(version: VersionTag): Try[zState] = Failure(new Error("Not supported"))

  override def maxRollbackDepth: Int = 0

  override type NVCT = this.type

}


object zState {

  val genesisState: Seq[zOutput] = {
    Seq(
      zOutput(PublicKey25519Proposition.validPubKey("01456a08bda264b3e6d4211f2bbb0478c4049b796afb759daace23c4247f72ea71b377262d").get, zValue @@ 1000000L)
    )
  }

  val empty: zState = zState(VersionTag @@ zBlockchain.GenesisBlock.id, genesisState)
}