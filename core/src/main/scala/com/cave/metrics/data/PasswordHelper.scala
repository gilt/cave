package com.cave.metrics.data

import org.mindrot.jbcrypt.BCrypt

class PasswordHelper {

  private final val Rounds = 10

  /**
   * Encrypt the clear password using b-crypt
   *
   * @param password  the clear password to encrypt
   * @return          the hashed password and the salt used
   */
  def encryptPassword(password: String): PasswordInfo = {
    val salt = BCrypt.gensalt(Rounds)
    val hash = BCrypt.hashpw(password, salt)
    PasswordInfo(hash, Some(salt))
  }

  /**
   * Compares a clear password against a hash
   *
   * @param password  the clear password to validate
   * @param hash      the hash to compare against
   * @param salt      the salt used for this hash
   * @return          true, if password matches; false, otherwise
   */
  def matchPassword(password: String, hash: String, salt: Option[String]): Boolean = {
    BCrypt.checkpw(password, hash)
  }
}
