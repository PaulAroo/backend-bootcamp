package io.hiis.service.auth.services.security

import org.apache.commons.codec.binary.Base64
import zio.{ UIO, ZIO }

import java.security.Key
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import scala.collection.Seq
import scala.concurrent.duration.{ DurationInt, FiniteDuration }
import scala.math.pow

sealed trait CryptoAlgorithm

case object HmacSHA512 extends CryptoAlgorithm

sealed trait HmacShaTotp {

  protected def TOTP_TIME_INTERVAL: FiniteDuration

  protected def CODE_LENGTH: Int

  protected def CRYPTO: CryptoAlgorithm

  final protected def getTotp(secret: String): UIO[String] =
    getTotpCode(secret, System.currentTimeMillis)

  final protected def validateTotp(otp: String, secret: String): UIO[Boolean] = {
    totpSeq(secret, System.currentTimeMillis).flatMap { tokens =>
      if (tokens.contains(otp.trim)) ZIO.succeed(true)
      else ZIO.succeed(false)
    }
  }

  private def getTotpCode(secret: String, totpGenerationTimeInMillis: Long): UIO[String] = {
    val timeWindow = totpGenerationTimeInMillis / TOTP_TIME_INTERVAL.toMillis

    val msg: Array[Byte] = BigInt(timeWindow).toByteArray.reverse.padTo(8, 0.toByte).reverse

    val hash        = hmacSha(CRYPTO.toString, new Base64().decode(secret), msg)
    val offset: Int = hash(hash.length - 1) & 0xf
    val binary: Long = ((hash(offset) & 0x7f) << 24) |
      ((hash(offset + 1) & 0xff) << 16) |
      ((hash(offset + 2) & 0xff) << 8 |
        (hash(offset + 3) & 0xff))

    val otp: Long = binary % pow(10, CODE_LENGTH).toLong

    ZIO.succeed(("0" * CODE_LENGTH + otp.toString).takeRight(CODE_LENGTH))
  }

  private def hmacSha(crypto: String, keyBytes: Array[Byte], text: Array[Byte]): Array[Byte] = {
    val hmac: Mac   = Mac.getInstance(crypto)
    val macKey: Key = new SecretKeySpec(keyBytes, "RAW")
    hmac.init(macKey)
    hmac.doFinal(text)
  }

  private def totpSeq(secret: String, time: Long): UIO[Seq[String]] = {
    val window: Seq[Long] = -TOTP_TIME_INTERVAL.toSeconds to 0
    ZIO.foreach(window.map(_.toString))(inc =>
      for {
        token <- getTotpCode(secret, time + (inc.toLong * 1000))
      } yield token
    )
  }
}

private[services] trait TotpSha512 extends HmacShaTotp {
  final override val TOTP_TIME_INTERVAL = 300.seconds
  final override val CODE_LENGTH        = 6
  final override val CRYPTO             = HmacSHA512
}
