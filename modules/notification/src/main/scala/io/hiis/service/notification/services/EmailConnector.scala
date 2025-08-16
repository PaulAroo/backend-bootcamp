package io.hiis.service.notification.services

import com.resend.Resend
import com.resend.services.emails.model.CreateEmailOptions
import io.hiis.service.core.models.Config.MailServerConfig
import io.hiis.service.core.models.misc.Notification.EmailNotification
import io.hiis.service.core.utils.Logging
import zio.{ Task, ZIO, ZLayer }

sealed trait EmailConnector {
  def sendEmail(emailNotification: EmailNotification): Task[String]
}

final case class EmailConnectorImpl(mailServerConfig: MailServerConfig)
    extends EmailConnector
    with Logging {
  val resend = new Resend(mailServerConfig.password)

  override def sendEmail(emailNotification: EmailNotification): Task[String] = {

    logInfo(s"Sending Email Notification to: [${emailNotification.to}]") *> (for {
      params <- ZIO.succeed(
        CreateEmailOptions
          .builder()
          .from(mailServerConfig.sender)
          .to(emailNotification.to.value)
          .subject(emailNotification.subject)
          .text(emailNotification.body)
          .html(emailNotification.html.getOrElse(emailNotification.body))
          .build()
      )
      messageId <- ZIO.attemptBlocking(resend.emails().send(params))
    } yield messageId.getId).tap(_ =>
      logInfo(s"Email Notification sent to: [${emailNotification.to}]")
    )
  }
}

object EmailConnector {
  val live: ZLayer[MailServerConfig, Nothing, EmailConnectorImpl] =
    ZLayer.fromFunction(EmailConnectorImpl.apply _)
}
