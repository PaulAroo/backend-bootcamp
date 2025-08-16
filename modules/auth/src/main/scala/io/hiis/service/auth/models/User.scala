package io.hiis.service.auth.models

import io.hiis.service.core.models.UserId
import io.hiis.service.core.models.auth.SimpleIdentity
import io.hiis.service.core.models.misc.Sexes.Sex
import io.hiis.service.core.models.misc.{ Email, FullName, PhoneNumber }

import java.net.URI
import java.time.Instant

/**
 * User Database Transfer Object (DTO)
 * @param id
 */
final case class User(
    id: UserId,
    phone: Option[PhoneNumber],
    email: Email,
    name: Option[FullName] = None,
    sex: Option[Sex] = None, // TODO this could be named gender
    image: Option[URI] = None,
    isActivated: Boolean = false,
    lastLoginAt: Option[Instant] = None,
    createdByIp: Option[String] = None,
    createdAt: Option[Instant] = Some(Instant.now())
) extends SimpleIdentity
