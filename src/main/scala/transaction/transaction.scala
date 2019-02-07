import supertagged.TaggedType

package object transaction {

  object zHash extends TaggedType[Array[Byte]]
  type zHash = zHash.Type

  object zValue extends TaggedType[Long]
  type zValue = zValue.Type

}
