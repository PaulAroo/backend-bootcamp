package io.hiis.service.auth.models.rest.request

import io.hiis.service.core.models.misc.Email

final case class Credentials(identifier: Email, password: String)
