package io.hiis.service.core.models.misc

import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.{ Decoder, Encoder }
import io.hiis.service.core.models.misc.NotificationTypes.NotificationType

object NotificationTypes extends Enumeration {
  type NotificationType = Value

  val SMS   = Value(0)
  val EMAIL = Value(1)

  implicit val decoder: Decoder[NotificationType] = Decoder.decodeEnumeration(NotificationTypes)
  implicit val encoder: Encoder[NotificationType] = Encoder.encodeEnumeration(NotificationTypes)
}

final case class Notification(key: String, channel: NotificationType, payload: String)
    extends Serializable
    with Product

object Notification {

  implicit val decoder: Decoder[Notification] = deriveDecoder
  implicit val encoder: Encoder[Notification] = deriveEncoder

  final case class SMSNotification(from: PhoneNumber, to: PhoneNumber, body: String)

  final case class EmailNotification(
      from: Email,
      to: Email,
      subject: String,
      body: String,
      html: Option[String] = None
  )
}
