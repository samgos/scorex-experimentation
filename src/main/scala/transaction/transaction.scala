import supertagged.TaggedType

package object transaction {

  object zHash extends TaggedType[Array[Byte]]
  object zValue extends TaggedType[Long]
  type zValue = zValue.Type
  type zHash = zValue.Type

}
