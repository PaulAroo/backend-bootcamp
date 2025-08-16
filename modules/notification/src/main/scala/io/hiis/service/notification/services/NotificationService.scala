package io.hiis.service.notification.services

import io.hiis.service.core.models.Config.{ MailServerConfig, TwilioConfig }
import io.hiis.service.core.models.misc.Notification.{ EmailNotification, SMSNotification }
import zio.{ Task, ZIO, ZLayer }

trait NotificationService {
  def sendSMS(sms: SMSNotification): Task[Unit]

  def sendEmail(email: EmailNotification): Task[Unit]
}

final case class NotificationServiceImpl(emailConnector: EmailConnector, smsConnector: SMSConnector)
    extends NotificationService {
  override def sendSMS(sms: SMSNotification): Task[Unit] = smsConnector.sendSMS(sms).unit

  override def sendEmail(email: EmailNotification): Task[Unit] =
    emailConnector.sendEmail(email).unit
}

object NotificationService {
  val live: ZLayer[TwilioConfig with MailServerConfig, Nothing, NotificationServiceImpl] =
    ZLayer.fromZIO(
      for {
        mailerConfig <- ZIO.service[MailServerConfig]
        twilioConfig <- ZIO.service[TwilioConfig]
      } yield NotificationServiceImpl(
        EmailConnectorImpl(mailerConfig),
        SMSConnectorImpl(twilioConfig)
      )
    )
}
