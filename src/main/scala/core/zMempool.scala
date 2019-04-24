package core

import scorex.core.transaction.MemoryPool
import transaction.zTransaction
import scorex.util.ModifierId
import scala.util.Try

case class zMempool(zPool: Seq[zTransaction] = Seq()) extends MemoryPool[zTransaction, zMempool] {

  override def modifierById(zId: ModifierId): Option[zTransaction] = zPool.find(_.id == zId)
  override def put(zTxs: zTransaction): Try[zMempool] = put(Seq(zTxs))

  override def put(zTxs: Iterable[zTransaction]): Try[zMempool] = Try {
    putWithoutCheck(zTxs)
  }

  override def putWithoutCheck(zTxs: Iterable[zTransaction]): zMempool = {
    val uniqueParents = zTxs.map(tx => tx.id -> tx).toMap.values
    val newTransactions = uniqueParents.filter(x => !zPool.contains(x)).take(zMempool - zPool.size)
    new zMempool(zPool ++ newTransactions)
  }

  override def remove(tx: zTransaction): zMempool = {
    new zMempool(zPool.filter(tx => zPool != tx))
  }

  override def filter(condition: zTransaction => Boolean): zMempool = {
    new zMempool(poolTxs.filter(condition))
  }

  override def getAll(zId: Seq[ModifierId]): Seq[zTransaction] = zPool.filter(tx => zId.contains(tx.id))
  override def contains(zId: ModifierId): Boolean = zPool.exists(_.id == zId)
  override def take(limit: Int): Seq[zTransaction] = poolTxs.take(limit)
  override def size: Int = zPool.size
  override type NVCT = zMempool

}

object BDMempool {

  val empty: zMempool = zMempool(Seq.empty)
  val Limit = 500

}


