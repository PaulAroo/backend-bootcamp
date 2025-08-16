package io.hiis.service.auth.services.security

import com.github.t3hnar.bcrypt._
import zio.{ Task, ULayer, ZIO, ZLayer }

private[services] trait PasswordHashService {

  /**
   * Computes the hash of a password
   * @param password
   *   password to be hashed
   * @return
   *   the hashed password
   */
  def hash(password: String): Task[String]

  /**
   * Validate a password against a hashed password
   * @param password
   *   the plain password
   * @param hashedPassword
   *   the hashed password
   * @return
   *   true if passwords are identical
   */
  def validate(password: String, hashedPassword: String): Task[Boolean]
}

final private[services] case class SecuredPasswordHasher() extends PasswordHashService {

  /**
   * Computes the hash of a password
   *
   * @param password
   *   password to be hashed
   * @return
   *   the hashed password
   */
  override def hash(password: String): Task[String] =
    ZIO.fromTry(password.bcryptSafeBounded)

  /**
   * Validate a password against a hashed password
   *
   * @param password
   *   the plain password
   * @param hashedPassword
   *   the hashed password
   * @return
   *   true if passwords are identical
   */
  override def validate(password: String, hashedPassword: String): Task[Boolean] =
    ZIO.fromTry(password.isBcryptedSafeBounded(hashedPassword))
}

object PasswordHashService {
  val live: ULayer[SecuredPasswordHasher] = ZLayer.succeed(SecuredPasswordHasher())
}
