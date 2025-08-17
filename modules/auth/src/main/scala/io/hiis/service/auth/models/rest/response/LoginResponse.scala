package io.hiis.service.auth.models.rest.response

import io.hiis.service.core.models.auth.{ JwtToken, SimpleIdentity }

final case class LoginResponse(
    user: SimpleIdentity,
    authToken: JwtToken,
    refreshToken: String
)
