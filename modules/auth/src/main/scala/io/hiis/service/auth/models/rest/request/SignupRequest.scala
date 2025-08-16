package io.hiis.service.auth.models.rest.request

import io.hiis.service.core.models.misc.Sexes.Sex
import io.hiis.service.core.models.misc.{ Email, FullName, PhoneNumber }

case class SignupRequest(
    email: Email,
    name: FullName,
    sex: Sex,
    phone: PhoneNumber,
    password: String
)
