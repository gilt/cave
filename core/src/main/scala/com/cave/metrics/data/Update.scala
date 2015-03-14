package com.cave.metrics.data

import play.api.libs.json._
import play.api.libs.functional.syntax._

object Operation extends Enumeration {
  type Operation = Value

  val Create, Update, Delete = Value

  implicit val opReads = __.read[String].map(Operation.withName)
  implicit val opWrites = new Writes[Operation] {
    def writes(op: Operation) = JsString(op.toString)
  }
}

object Entity extends Enumeration {
  type Entity = Value

  val Organization, Team, Alert = Value

  implicit val entityReads = __.read[String].map(Entity.withName)
  implicit val entityWrites = new Writes[Entity] {
    def writes(entity: Entity) = JsString(entity.toString)
  }
}

import com.cave.metrics.data.Entity._
import com.cave.metrics.data.Operation._

/**
 * Class to encapsulate a notification that configuration has changed
 *
 * @param entityType  the type of changed entity
 * @param operation   the type of change that the entity has suffered
 * @param id          the identifier of the entity
 * @param extra       additional data for this notification event
 */
case class Update(entityType: Entity, operation: Operation, id: String, extra: String)

object Update {

  final val KeyEntity = "entity"
  final val KeyOperation = "operation"
  final val KeyId = "id"
  final val KeyExtra = "extra"

  implicit val updateReads: Reads[Update] = (
    (__ \ KeyEntity).read[Entity] and
    (__ \ KeyOperation).read[Operation] and
    (__ \ KeyId).read[String] and
    (__ \ KeyExtra).read[String]
  )(Update.apply _)

  implicit val updateWrites: Writes[Update] = (
    (__ \ KeyEntity).write[Entity] and
    (__ \ KeyOperation).write[Operation] and
    (__ \ KeyId).write[String] and
    (__ \ KeyExtra).write[String]
  )(unlift(Update.unapply))
}
