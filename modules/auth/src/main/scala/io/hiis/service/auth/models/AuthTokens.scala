package io.hiis.service.auth.models

import io.hiis.service.core.models.auth.JwtToken

final case class AuthTokens(
    auth: JwtToken,
    refresh: String
)
