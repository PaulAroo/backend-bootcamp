package io.hiis.service.auth.models.security

import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.{ Decoder, Encoder }
import io.hiis.service.core.models.UserId

import java.time.Instant
import java.util.UUID

final case class RefreshToken(
    id: String,
    userId: UserId,
    createdAt: Instant,
    expiresAt: Instant,
    maxLiveDate: Instant,
    lastUsedAt: Instant,
    revokedByIp: Option[String] = None,
    revokedAt: Option[Instant] = None
) extends Serializable

object RefreshToken {

  val DAY_IN_SECONDS: Long = 60 * 60 * 24
  val WINDOW_DAYS: Int =
    3 // Increase this window if you want the refresh token to be valid for x days even when not used
  val MAX_LIVE_DAYS: Int = 90

  def apply(
      userId: UserId
  ): RefreshToken = {
    val id  = UUID.randomUUID().toString
    val now = Instant.now()
    RefreshToken(
      id = id,
      userId = userId,
      createdAt = now,
      expiresAt = now.plusSeconds(DAY_IN_SECONDS * WINDOW_DAYS),
      maxLiveDate = now.plusSeconds(DAY_IN_SECONDS * MAX_LIVE_DAYS),
      lastUsedAt = now
    )
  }

  implicit val decoder: Decoder[RefreshToken] = deriveDecoder
  implicit val encoder: Encoder[RefreshToken] = deriveEncoder
}
