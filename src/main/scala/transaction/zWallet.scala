package transaction

import block.zBlock
import scorex.core.VersionTag
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.proof.Signature25519
import scorex.core.transaction.state.{PrivateKey25519, PrivateKey25519Companion}
import scorex.core.transaction.wallet.Vault
import scorex.util.ScorexLogging
import sync.zState

import scala.util.Try

class zWallet(val zSecret: PrivateKey25519, var zBoxes: Seq[zOutput]) extends Vault[zTransaction, zBlock, zWallet] with ScorexLogging {

  override type NVCT = zWallet

  // we don't care about transactions in mempool
  override def scanOffchain(zTx: zTransaction): zWallet = this

  // we don't care about transactions in mempool
  override def scanOffchain(zTxs: Seq[zTransaction]): zWallet = this

  override def scanPersistent(zModifier: zBlock): zWallet = {
    val txs = zModifier.zTransactions
    val spentIds = txs.flatMap(_.zInputs)
    val remainingBoxes = zBoxes.filter(b => !spentIds.exists(_ sameElements b.id))
    val newBoxes = txs.flatMap(_.zOutputs).filter(_.zProposition == zSecret.publicImage)
    zBoxes = remainingBoxes ++ newBoxes
    this
  }

  def balance: Long = zBoxes.map(_.zValue.toLong).sum

  def generateTx(zAmount: zValue, zRecipent: PublicKey25519Proposition): Try[zTransaction] = Try {
    val inputs = zBoxes.map(z => zHash !@@ z.id).toIndexedSeq
    val remaining = zBoxes.map(_.zValue.toLong).sum - zAmount
    val outputs = IndexedSeq(zOutput(zRecipent, zAmount), zOutput(zSecret.publicImage, zValue @@ remaining))

    val unsigned = zTransaction(inputs, outputs, IndexedSeq[Signature25519]())
    val msg = unsigned.messageToSign
    val signatures = inputs.map(_ => PrivateKey25519Companion.sign(zSecret, msg))
    zTransaction(inputs, outputs, signatures)
  }

  override def rollback(to: VersionTag): Try[zWallet] = Try {
    // todo not implemented
    this
  }

}

object zWallet {

  def apply(zSeed: String): zWallet = {
    val secret = PrivateKey25519Companion.generateKeys(zSeed.getBytes())._1
    val boxes = zState.genesisState.filter(_.zProposition == secret.publicImage)
    new zWallet(secret, boxes)
  }

}