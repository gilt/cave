package init

import com.cave.metrics.data.{Team, ConfirmationToken, Organization, User}
import com.typesafe.plugin._

import scala.concurrent._

class MailService {

  def sendRegistrationEmail(email: String, token: ConfirmationToken)
                           (implicit ec: ExecutionContext): Future[Unit] =
    sendEmail(
      recipient = email,
      subject = "CAVE Sign Up: Please confirm your email address",
      body = views.html.emailSignUp.render(token.uuid).body
    )

  def sendWelcomeEmail(user: User)
                      (implicit ex: ExecutionContext): Future[Unit] =
    sendEmail(
      recipient = user.email,
      subject = "Welcome to CAVE",
      body = views.html.emailWelcome.render(user).body
    )

  def sendAlreadyRegisteredEmail(user: User)
                                (implicit ec: ExecutionContext): Future[Unit] =
    sendEmail(
      recipient = user.email,
      subject = "CAVE reminder!",
      body = views.html.emailAlreadyRegistered.render(user).body
    )

  def sendForgotPasswordEmail(user: User, token: ConfirmationToken)
                             (implicit ec: ExecutionContext): Future[Unit] =
    sendEmail(
      recipient = user.email,
      subject = "Reset your CAVE password!",
      body = views.html.emailForgotPassword.render(user, token.uuid).body
    )

  def sendPasswordResetEmail(user: User)
                            (implicit ec: ExecutionContext): Future[Unit] =
    sendEmail(
      recipient = user.email,
      subject = "Your CAVE password has been changed!",
      body = views.html.emailPasswordReset.render(user).body
    )

  def sendAttemptedOrganizationAdd(email: String, organization: Organization, user: User)
                                  (implicit ec: ExecutionContext): Future[Unit] =
    sendEmail(
      recipient = email,
      subject = "A message from CAVE",
      body = views.html.emailAttemptedOrganizationAdd.render(user, organization.name).body
    )

  def sendAttemptedTeamAdd(email: String, organization: Organization, team: Team, user: User)
                          (implicit ec: ExecutionContext): Future[Unit] =
    sendEmail(
      recipient = email,
      subject = "A message from CAVE",
      body = views.html.emailAttemptedTeamAdd.render(user, organization.name, team.name).body
    )

  /**
   * Send an email using the Mailer plugin
   *
   * @param recipient   the recipient email address
   * @param subject     the subject for this email
   * @param body        the message body
   * @return            nothing
   */
  private[this] def sendEmail(recipient: String, subject: String, body: String)
                             (implicit ec: ExecutionContext): Future[Unit] = future {
    import play.api.Play.current
    val mail = use[MailerPlugin].email

    mail.setRecipient(recipient)
    mail.setFrom("Do Not Reply <do-not-reply@cavellc.io>")
    mail.setSubject(subject)
    mail.sendHtml(body)
  }
}
