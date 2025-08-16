package io.hiis.service.auth.models

final case class Totp(otp: String, crypto: String)
