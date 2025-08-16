package io.hiis.service.notification.services

import com.twilio.Twilio
import com.twilio.`type`.PhoneNumber
import com.twilio.rest.api.v2010.account.Message
import io.hiis.service.core.models.Config.TwilioConfig
import io.hiis.service.core.models.misc.Notification.SMSNotification
import io.hiis.service.core.utils.Logging
import zio.{ Task, ZIO, ZLayer }

trait SMSConnector {
  def sendSMS(smsNotification: SMSNotification): Task[String]
}

final case class SMSConnectorImpl(twilioConfig: TwilioConfig) extends SMSConnector with Logging {
  override def sendSMS(smsNotification: SMSNotification): Task[String] = {
    ZIO.attemptBlocking {
      Twilio.init(twilioConfig.account, twilioConfig.token)
      Message
        .creator(
          new PhoneNumber(smsNotification.to.toString),
          new PhoneNumber(smsNotification.from.toString),
          smsNotification.body
        )
        .create()
        .getSid
    }
  }
}

object SMSConnector {
  val live: ZLayer[TwilioConfig, Nothing, SMSConnectorImpl] =
    ZLayer.fromFunction(SMSConnectorImpl.apply _)
}
