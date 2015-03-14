import com.cave.metrics.data.{ConfirmationToken, User, UserConfirmation}
import data.UserData
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.Results
import play.api.test.{FakeApplication, FakeHeaders, FakeRequest, PlaySpecification}

import scala.concurrent.{ExecutionContext, Future}

class UsersAndPasswordsApiSpec extends PlaySpecification with Results with AbstractUsersApiSpec with UserData {

  "POST /users/register" should {

    "respond with 202 and send a registration email" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(None))
      when(mockDataManager.createConfirmationToken(any[ConfirmationToken])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_CONFIRMATION_TOKEN)))
      when(mockMailService.sendRegistrationEmail(anyString, any[ConfirmationToken])(any[ExecutionContext])).thenReturn(Future.successful())

      val result = new TestController(EXPIRATION1).registerUser()(
        FakeRequest(POST, "/users/register", FakeHeaders(), Json.obj("email" -> SOME_EMAIL)))


      contentAsString(result) must equalTo("")
      status(result) must equalTo(ACCEPTED)
    }

    "respond with 400 if email not specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      val result = new TestController(EXPIRATION1).registerUser()(
        FakeRequest(POST, "/users/register", FakeHeaders(), Json.obj("bogus" -> SOME_EMAIL)))

      contentAsString(result) must equalTo("Cannot parse request body: email is missing.")
      status(result) must equalTo(BAD_REQUEST)
    }

    "respond with 202 and send an email for already registered address" in running(FakeApplication(withGlobal = mockGlobal)) {
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockMailService.sendAlreadyRegisteredEmail(SOME_USER)).thenReturn(Future.successful())

      val result = new TestController(EXPIRATION1).registerUser()(
        FakeRequest(POST, "/users/register", FakeHeaders(), Json.obj("email" -> SOME_EMAIL)))


      contentAsString(result) must equalTo("")
      status(result) must equalTo(ACCEPTED)
    }

    "respond with 500 if there's an error during createConfirmationToken" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(None))
      when(mockDataManager.createConfirmationToken(any[ConfirmationToken])(any[ExecutionContext])).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController(EXPIRATION1).registerUser()(
        FakeRequest(POST, "/users/register", FakeHeaders(), Json.obj("email" -> SOME_EMAIL)))


      contentAsString(result) must equalTo(InternalErrorMessage)
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
    }

    "respond with 500 if we cannot create confirmation token" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(None))
      when(mockDataManager.createConfirmationToken(any[ConfirmationToken])(any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = new TestController(EXPIRATION1).registerUser()(
        FakeRequest(POST, "/users/register", FakeHeaders(), Json.obj("email" -> SOME_EMAIL)))


      contentAsString(result) must equalTo(InternalErrorMessage)
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
    }

    "respond with 500 if there's an error during getUserByEmail" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController(EXPIRATION1).registerUser()(
        FakeRequest(POST, "/users/register", FakeHeaders(), Json.obj("email" -> SOME_EMAIL)))


      contentAsString(result) must equalTo(InternalErrorMessage)
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
    }
  }

  "POST /users/confirm" should {

    "respond with 201 and user information" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getConfirmationTokenByUUID(SOME_UUID)).thenReturn(Future.successful(Some(SOME_CONFIRMATION_TOKEN)))
      when(mockPasswordHelper.encryptPassword(SOME_PASSWORD)).thenReturn(SOME_INFO)
      when(mockDataManager.createUser(any[User])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.deleteConfirmationToken(SOME_UUID)).thenReturn(Future.successful(Some(SOME_CONFIRMATION_TOKEN)))

      val result = new TestController(EXPIRATION1).confirmUser()(
        FakeRequest(POST, "/users/confirm", FakeHeaders(),
          Json.toJson(UserConfirmation(SOME_FIRST, SOME_LAST, SOME_PASSWORD, SOME_UUID))))


      contentAsJson(result) must equalTo(Json.toJson(SOME_USER))
      status(result) must equalTo(CREATED)
    }

    "respond with 400 if there are missing fields in the request" in running(FakeApplication(withGlobal = mockGlobal)) {

      val result = new TestController(EXPIRATION1).confirmUser()(
        FakeRequest(POST, "/users/confirm", FakeHeaders(),
          Json.obj("first_name" -> SOME_FIRST)))


      contentAsString(result) must equalTo("Cannot parse JSON data: must have first_name, last_name, email, password and confirmation_token.")
      status(result) must equalTo(BAD_REQUEST)
    }

    "respond with 400 if token is invalid" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getConfirmationTokenByUUID(SOME_UUID)).thenReturn(Future.successful(None))

      val result = new TestController(EXPIRATION1).confirmUser()(
        FakeRequest(POST, "/users/confirm", FakeHeaders(),
          Json.toJson(UserConfirmation(SOME_FIRST, SOME_LAST, SOME_PASSWORD, SOME_UUID))))


      contentAsString(result) must equalTo("Invalid confirmation token.")
      status(result) must equalTo(BAD_REQUEST)
    }

    "respond with 400 if token is expired" in running(FakeApplication(withGlobal = mockGlobal)) {
      when(mockDataManager.getConfirmationTokenByUUID(SOME_UUID)).thenReturn(Future.successful(Some(SOME_CONFIRMATION_TOKEN)))
      when(mockDataManager.createUser(any[User])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))

      val result = new TestController(EXPIRATION2).confirmUser()(
        FakeRequest(POST, "/users/confirm", FakeHeaders(),
          Json.toJson(UserConfirmation(SOME_FIRST, SOME_LAST, SOME_PASSWORD, SOME_UUID))))

      contentAsString(result) must equalTo("Confirmation token has expired. Please register again.")
      status(result) must equalTo(BAD_REQUEST)
    }

    "respond with 500 if there is an error during getConfirmationToken" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getConfirmationTokenByUUID(SOME_UUID)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController(EXPIRATION1).confirmUser()(
        FakeRequest(POST, "/users/confirm", FakeHeaders(),
          Json.toJson(UserConfirmation(SOME_FIRST, SOME_LAST, SOME_PASSWORD, SOME_UUID))))


      contentAsString(result) must equalTo(InternalErrorMessage)
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
    }

    "respond with 500 if we fail to create the user" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getConfirmationTokenByUUID(SOME_UUID)).thenReturn(Future.successful(Some(SOME_CONFIRMATION_TOKEN)))
      when(mockPasswordHelper.encryptPassword(SOME_PASSWORD)).thenReturn(SOME_INFO)
      when(mockDataManager.createUser(any[User])(any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = new TestController(EXPIRATION1).confirmUser()(
        FakeRequest(POST, "/users/confirm", FakeHeaders(),
          Json.toJson(UserConfirmation(SOME_FIRST, SOME_LAST, SOME_PASSWORD, SOME_UUID))))


      contentAsString(result) must equalTo(InternalErrorMessage)
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
    }

    "respond with 500 if there is an error during createUser" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getConfirmationTokenByUUID(SOME_UUID)).thenReturn(Future.successful(Some(SOME_CONFIRMATION_TOKEN)))
      when(mockPasswordHelper.encryptPassword(SOME_PASSWORD)).thenReturn(SOME_INFO)
      when(mockDataManager.createUser(any[User])(any[ExecutionContext])).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController(EXPIRATION1).confirmUser()(
        FakeRequest(POST, "/users/confirm", FakeHeaders(),
          Json.toJson(UserConfirmation(SOME_FIRST, SOME_LAST, SOME_PASSWORD, SOME_UUID))))


      contentAsString(result) must equalTo(InternalErrorMessage)
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
    }

    "respond with 500 if there is an error during deleteConfirmationToken" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getConfirmationTokenByUUID(SOME_UUID)).thenReturn(Future.successful(Some(SOME_CONFIRMATION_TOKEN)))
      when(mockPasswordHelper.encryptPassword(SOME_PASSWORD)).thenReturn(SOME_INFO)
      when(mockDataManager.createUser(any[User])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.deleteConfirmationToken(SOME_UUID)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController(EXPIRATION1).confirmUser()(
        FakeRequest(POST, "/users/confirm", FakeHeaders(),
          Json.toJson(UserConfirmation(SOME_FIRST, SOME_LAST, SOME_PASSWORD, SOME_UUID))))


      contentAsString(result) must equalTo(InternalErrorMessage)
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
    }
  }

  "POST /users/login" should {
    "respond with 200 and a valid token" in {

      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockPasswordHelper.matchPassword(SOME_PASSWORD, SOME_HASH, SOME_SALT)).thenReturn(true)
      when(mockDataManager.createSessionToken(anyLong(), any[DateTime], any[DateTime])(any[ExecutionContext])).
        thenReturn(Future.successful(SOME_SESSION_TOKEN))

      val result = new TestController(SOME_DATETIME).loginUser()(
        FakeRequest(POST, "/users/login", FakeHeaders(),
          Json.obj("email" -> SOME_EMAIL, "password" -> SOME_PASSWORD)))

      status(result) must equalTo(OK)
      val response = contentAsJson(result)
      val token = (response \ "token").as[String]
      val expirationDate = ISODateTimeFormat.dateTimeNoMillis.parseDateTime((response \ "expires").as[String])

      token must equalTo(SOME_TOKEN)
      expirationDate must equalTo(SOME_DATETIME_PLUS_1H)
    }

    "respond with 400 if email is missing from input JSON" in {

      val result = new TestController(SOME_DATETIME).loginUser()(
        FakeRequest(POST, "/users/login", FakeHeaders(),
          Json.obj("mail" -> SOME_EMAIL, "password" -> SOME_PASSWORD)))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Cannot parse request body: must have an email and a password.")
    }

    "respond with 400 if password is missing from input JSON" in {

      val result = new TestController(SOME_DATETIME).loginUser()(
        FakeRequest(POST, "/users/login", FakeHeaders(),
          Json.obj("email" -> SOME_EMAIL, "word" -> SOME_PASSWORD)))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Cannot parse request body: must have an email and a password.")
    }

    "respond with 400 if email is unknown" in {
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(None))

      val result = new TestController(SOME_DATETIME).loginUser()(
        FakeRequest(POST, "/users/login", FakeHeaders(),
          Json.obj("email" -> SOME_EMAIL, "password" -> SOME_PASSWORD)))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Login failed.")
    }

    "respond with 400 if password doesn't match" in {
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockPasswordHelper.matchPassword(SOME_PASSWORD, SOME_HASH, SOME_SALT)).thenReturn(false)

      val result = new TestController(SOME_DATETIME).loginUser()(
        FakeRequest(POST, "/users/login", FakeHeaders(),
          Json.obj("email" -> SOME_EMAIL, "password" -> SOME_PASSWORD)))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Login failed.")
    }

    "respond with 500 if there's an error during getUserByEmail" in {
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController(SOME_DATETIME).loginUser()(
        FakeRequest(POST, "/users/login", FakeHeaders(),
          Json.obj("email" -> SOME_EMAIL, "password" -> SOME_PASSWORD)))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "respond with 500 if there's an error during createSessionToken" in {
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockPasswordHelper.matchPassword(SOME_PASSWORD, SOME_HASH, SOME_SALT)).thenReturn(true)
      when(mockDataManager.createSessionToken(anyLong(), any[DateTime], any[DateTime])(any[ExecutionContext])).
        thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController(SOME_DATETIME).loginUser()(
        FakeRequest(POST, "/users/login", FakeHeaders(),
          Json.obj("email" -> SOME_EMAIL, "password" -> SOME_PASSWORD)))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }
  }

  "POST /users/forgot-password" should {
    "respond with 202 and send a password reset email" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.createConfirmationToken(any[ConfirmationToken])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_CONFIRMATION_TOKEN)))
      when(mockMailService.sendForgotPasswordEmail(any[User], any[ConfirmationToken])(any[ExecutionContext])).thenReturn(Future.successful())

      val result = new TestController(EXPIRATION1).forgotPassword()(
        FakeRequest(POST, "/users/forgot-password", FakeHeaders(), Json.obj("email" -> SOME_EMAIL)))


      contentAsString(result) must equalTo("")
      status(result) must equalTo(ACCEPTED)
    }

    "respond with 202 even if the user doesn't exist" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(None))

      val result = new TestController(EXPIRATION1).forgotPassword()(
        FakeRequest(POST, "/users/forgot-password", FakeHeaders(), Json.obj("email" -> SOME_EMAIL)))


      contentAsString(result) must equalTo("")
      status(result) must equalTo(ACCEPTED)
    }

    "respond with 500 if there is an error during getUserByEmail" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController(EXPIRATION1).forgotPassword()(
        FakeRequest(POST, "/users/forgot-password", FakeHeaders(), Json.obj("email" -> SOME_EMAIL)))


      contentAsString(result) must equalTo(InternalErrorMessage)
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
    }

    "respond with 500 if there is an error during createConfirmationToken" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.createConfirmationToken(
        any[ConfirmationToken])(any[ExecutionContext])).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController(EXPIRATION1).forgotPassword()(
        FakeRequest(POST, "/users/forgot-password", FakeHeaders(), Json.obj("email" -> SOME_EMAIL)))


      contentAsString(result) must equalTo(InternalErrorMessage)
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
    }

    "respond with 500 if we fail to create confirmation token" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.createConfirmationToken(
        any[ConfirmationToken])(any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = new TestController(EXPIRATION1).forgotPassword()(
        FakeRequest(POST, "/users/forgot-password", FakeHeaders(), Json.obj("email" -> SOME_EMAIL)))


      contentAsString(result) must equalTo(InternalErrorMessage)
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
    }

    "respond with 500 if there is an error during sendForgotPasswordEmail" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.createConfirmationToken(
        any[ConfirmationToken])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_CONFIRMATION_TOKEN)))
      when(mockMailService.sendForgotPasswordEmail(
        any[User], any[ConfirmationToken])(any[ExecutionContext])).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController(EXPIRATION1).forgotPassword()(
        FakeRequest(POST, "/users/forgot-password", FakeHeaders(), Json.obj("email" -> SOME_EMAIL)))


      contentAsString(result) must equalTo(InternalErrorMessage)
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
    }
  }

  "POST /users/reset-password" should {
    "respond with 200 and send a password reset confirmation email" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getConfirmationTokenByUUID(SOME_TOKEN)).thenReturn(Future.successful(Some(SOME_CONFIRMATION_TOKEN)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockPasswordHelper.encryptPassword(SOME_PASSWORD)).thenReturn(SOME_INFO)
      when(mockDataManager.updateUser(SOME_USER, None, None, Some(SOME_INFO))).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockMailService.sendPasswordResetEmail(SOME_USER)).thenReturn(Future.successful())

      val result = new TestController(EXPIRATION1).resetPassword()(
        FakeRequest(POST, "/users/reset-password", FakeHeaders(),
          Json.obj("token" -> SOME_TOKEN, "password" -> SOME_PASSWORD)))

      contentAsString(result) must equalTo("")
      status(result) must equalTo(OK)
    }

    "respond with 400 if token is missing" in running(FakeApplication(withGlobal = mockGlobal)) {

      val result = new TestController(EXPIRATION1).resetPassword()(
        FakeRequest(POST, "/users/reset-password", FakeHeaders(),
          Json.obj("tolkien" -> SOME_TOKEN, "password" -> SOME_PASSWORD)))

      contentAsString(result) must equalTo("Cannot parse request body: must have a token and a password.")
      status(result) must equalTo(BAD_REQUEST)
    }

    "respond with 400 if password is missing" in running(FakeApplication(withGlobal = mockGlobal)) {

      val result = new TestController(EXPIRATION1).resetPassword()(
        FakeRequest(POST, "/users/reset-password", FakeHeaders(),
          Json.obj("token" -> SOME_TOKEN, "word" -> SOME_PASSWORD)))

      contentAsString(result) must equalTo("Cannot parse request body: must have a token and a password.")
      status(result) must equalTo(BAD_REQUEST)
    }

    "respond with 400 if the token doesn't exist" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getConfirmationTokenByUUID(SOME_TOKEN)).thenReturn(Future.successful(None))

      val result = new TestController(EXPIRATION1).resetPassword()(
        FakeRequest(POST, "/users/reset-password", FakeHeaders(),
          Json.obj("token" -> SOME_TOKEN, "password" -> SOME_PASSWORD)))

      contentAsString(result) must equalTo("Invalid token.")
      status(result) must equalTo(BAD_REQUEST)
    }

    "respond with 400 if the token is expired" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getConfirmationTokenByUUID(SOME_TOKEN)).thenReturn(Future.successful(Some(SOME_CONFIRMATION_TOKEN)))

      val result = new TestController(EXPIRATION2).resetPassword()(
        FakeRequest(POST, "/users/reset-password", FakeHeaders(),
          Json.obj("token" -> SOME_TOKEN, "password" -> SOME_PASSWORD)))

      contentAsString(result) must equalTo("Confirmation token has expired.")
      status(result) must equalTo(BAD_REQUEST)
    }

    "respond with 400 if the user doesn't exist" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getConfirmationTokenByUUID(SOME_TOKEN)).thenReturn(Future.successful(Some(SOME_CONFIRMATION_TOKEN)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(None))

      val result = new TestController(EXPIRATION1).resetPassword()(
        FakeRequest(POST, "/users/reset-password", FakeHeaders(),
          Json.obj("token" -> SOME_TOKEN, "password" -> SOME_PASSWORD)))

      contentAsString(result) must equalTo("Invalid token.")
      status(result) must equalTo(BAD_REQUEST)
    }

    "respond with 500 if there's an error during getConfirmationTokenByUUID" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getConfirmationTokenByUUID(SOME_TOKEN)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController(EXPIRATION1).resetPassword()(
        FakeRequest(POST, "/users/reset-password", FakeHeaders(),
          Json.obj("token" -> SOME_TOKEN, "password" -> SOME_PASSWORD)))

      contentAsString(result) must equalTo(InternalErrorMessage)
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
    }

    "respond with 500 if there's an error during getUserByEmail" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getConfirmationTokenByUUID(SOME_TOKEN)).thenReturn(Future.successful(Some(SOME_CONFIRMATION_TOKEN)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController(EXPIRATION1).resetPassword()(
        FakeRequest(POST, "/users/reset-password", FakeHeaders(),
          Json.obj("token" -> SOME_TOKEN, "password" -> SOME_PASSWORD)))

      contentAsString(result) must equalTo(InternalErrorMessage)
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
    }

    "respond with 500 if there's an error during updatePasswordInfo" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getConfirmationTokenByUUID(SOME_TOKEN)).thenReturn(Future.successful(Some(SOME_CONFIRMATION_TOKEN)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockPasswordHelper.encryptPassword(SOME_PASSWORD)).thenReturn(SOME_INFO)
      when(mockDataManager.updateUser(SOME_USER, None, None, Some(SOME_INFO))).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController(EXPIRATION1).resetPassword()(
        FakeRequest(POST, "/users/reset-password", FakeHeaders(),
          Json.obj("token" -> SOME_TOKEN, "password" -> SOME_PASSWORD)))

      contentAsString(result) must equalTo(InternalErrorMessage)
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
    }

    "respond with 500 if user cannot be updated" in running(FakeApplication(withGlobal = mockGlobal)) {

      when(mockDataManager.getConfirmationTokenByUUID(SOME_TOKEN)).thenReturn(Future.successful(Some(SOME_CONFIRMATION_TOKEN)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockPasswordHelper.encryptPassword(SOME_PASSWORD)).thenReturn(SOME_INFO)
      when(mockDataManager.updateUser(SOME_USER, None, None, Some(SOME_INFO))).thenReturn(Future.successful(None))

      val result = new TestController(EXPIRATION1).resetPassword()(
        FakeRequest(POST, "/users/reset-password", FakeHeaders(),
          Json.obj("token" -> SOME_TOKEN, "password" -> SOME_PASSWORD)))

      contentAsString(result) must equalTo(InternalErrorMessage)
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
    }
  }
}
