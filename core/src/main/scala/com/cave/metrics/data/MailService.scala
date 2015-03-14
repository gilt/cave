package com.cave.metrics.data

import scala.concurrent._

class MailService {

  def sendRegistrationEmail(email: String, token: ConfirmationToken)(implicit ec: ExecutionContext): Future[Unit] = future {
    // TODO: implement me
    println(s"Send confirmation token ${token.uuid} to email $email...")
  }

  def sendAlreadyRegisteredEmail(email: String)(implicit ec: ExecutionContext): Future[Unit] = future {
    // TODO: implement me
    println(s"Send already registered email $email...")
  }

  def sendForgotPasswordEmail(email: String, token: ConfirmationToken)(implicit ec: ExecutionContext): Future[Unit] = future {
    // TODO: implement me
    println(s"Send forgot password message with token ${token.uuid} to email $email...")
  }

  def sendPasswordResetEmail(email: String)(implicit ec: ExecutionContext): Future[Unit] = future {
    // TODO: implement me
    println(s"Send password reset email $email...")
  }

  def sendAttemptedAdd(email: String, organization: Organization, user: User)(implicit ec: ExecutionContext) = future {
    // TODO: implement me
    println(s"Sending an email to $email about $user attempted adding to $organization.")
  }
}
