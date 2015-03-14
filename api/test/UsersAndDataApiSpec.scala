import com.cave.metrics.data.{User, Role}
import data.UserData
import org.mockito.Mockito
import org.mockito.Mockito._
import org.specs2.mutable.BeforeAfter
import play.api.libs.json.Json
import play.api.mvc.Results
import play.api.test.{FakeHeaders, FakeRequest, PlaySpecification}

import scala.concurrent.Future
import scala.util.{Failure, Success}

class UsersAndDataApiSpec extends PlaySpecification with Results with AbstractUsersApiSpec with UserData with BeforeAfter {

  override def before = Mockito.reset(mockDataManager, mockPasswordHelper)
  override def after = verifyNoMoreInteractions(mockDataManager, mockPasswordHelper)

  "GET /users/info" should {

    "respond with 200 and user as JSON" in {

      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION1)).thenReturn(Future.successful(Some(SOME_USER)))

      val result = new TestController(EXPIRATION1).getUser()(
        FakeRequest("GET", "/users/info").withHeaders(AUTHORIZATION -> AUTH_TOKEN)
      )

      status(result) must equalTo(OK)
      contentAsJson(result) must equalTo(Json.obj(
        "first_name" -> SOME_FIRST,
        "last_name" -> SOME_LAST,
        "email" -> SOME_USER_EMAIL
      ))
    }

    "respond with 200 and user as JSON even when using basic auth" in {

      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION1)).thenReturn(Future.successful(Some(SOME_USER)))

      val result = new TestController(EXPIRATION1).getUser()(
        FakeRequest("GET", "/users/info").withHeaders(AUTHORIZATION -> BASIC_AUTH_TOKEN)
      )

      status(result) must equalTo(OK)
      contentAsJson(result) must equalTo(Json.obj(
        "first_name" -> SOME_FIRST,
        "last_name" -> SOME_LAST,
        "email" -> SOME_USER_EMAIL
      ))
    }

    "respond with 401 if no authorization token provided" in {

      val result = new TestController(EXPIRATION1).getUser()(
        FakeRequest("GET", "/users/info")
      )

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 401 if unsupported authorization token provided" in {

      val result = new TestController(EXPIRATION1).getUser()(
        FakeRequest("GET", "/users/info").withHeaders(AUTHORIZATION -> UNSUPPORTED_TOKEN)
      )

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if token unrecognized" in {

      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION1)).thenReturn(Future.successful(None))

      val result = new TestController(EXPIRATION1).getUser()(
        FakeRequest("GET", "/users/info").withHeaders(AUTHORIZATION -> AUTH_TOKEN)
      )

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if token is expired" in {

      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION2)).thenReturn(Future.successful(None))

      val result = new TestController(EXPIRATION2).getUser()(
        FakeRequest("GET", "/users/info").withHeaders(AUTHORIZATION -> AUTH_TOKEN)
      )

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }
  }

  "PATCH /users/info" should {

    "respond with 200 and the updated user (first_name)" in {
      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION1)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.updateUser(SOME_USER, Some(NEW_FIRST), None, None)).thenReturn(
        Future.successful(Some(getPatchedUser(SOME_USER, Some(NEW_FIRST), None, None))))

      val result = new TestController(EXPIRATION1).updateUser()(
        FakeRequest("PATCH", "/users/info", FakeHeaders(Seq((AUTHORIZATION, Seq(AUTH_TOKEN)))),
        Json.obj("first_name" -> NEW_FIRST)))

      status(result) must equalTo(OK)
      contentAsJson(result) must equalTo(Json.obj(
        "first_name" -> NEW_FIRST,
        "last_name" -> SOME_LAST,
        "email" -> SOME_USER_EMAIL
      ))
    }

    "respond with 200 and the updated user (last_name)" in {
      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION1)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.updateUser(SOME_USER, None, Some(NEW_LAST), None))
        .thenReturn(Future.successful(Some(getPatchedUser(SOME_USER, None, Some(NEW_LAST), None))))

      val result = new TestController(EXPIRATION1).updateUser()(
        FakeRequest("PATCH", "/users/info", FakeHeaders(Seq((AUTHORIZATION, Seq(AUTH_TOKEN)))),
          Json.obj("last_name" -> NEW_LAST)))

      status(result) must equalTo(OK)
      contentAsJson(result) must equalTo(Json.obj(
        "first_name" -> SOME_FIRST,
        "last_name" -> NEW_LAST,
        "email" -> SOME_USER_EMAIL
      ))
    }

    "respond with 200 and the updated user (password)" in {
      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION1)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockPasswordHelper.encryptPassword(NEW_PASSWORD)).thenReturn(NEW_INFO)
      when(mockDataManager.updateUser(SOME_USER, None, None, Some(NEW_INFO)))
        .thenReturn(Future.successful(Some(getPatchedUser(SOME_USER, None, None, Some(NEW_INFO)))))

      val result = new TestController(EXPIRATION1).updateUser()(
        FakeRequest("PATCH", "/users/info", FakeHeaders(Seq((AUTHORIZATION, Seq(AUTH_TOKEN)))),
          Json.obj("password" -> NEW_PASSWORD)))

      status(result) must equalTo(OK)
      contentAsJson(result) must equalTo(Json.obj(
        "first_name" -> SOME_FIRST,
        "last_name" -> SOME_LAST,
        "email" -> SOME_USER_EMAIL
      ))
    }

    "respond with 200 and the updated user (all fields)" in {
      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION1)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockPasswordHelper.encryptPassword(NEW_PASSWORD)).thenReturn(NEW_INFO)
      when(mockDataManager.updateUser(SOME_USER, Some(NEW_FIRST), Some(NEW_LAST), Some(NEW_INFO)))
        .thenReturn(Future.successful(Some(getPatchedUser(SOME_USER, Some(NEW_FIRST), Some(NEW_LAST), Some(NEW_INFO)))))

      val result = new TestController(EXPIRATION1).updateUser()(
        FakeRequest("PATCH", "/users/info", FakeHeaders(Seq((AUTHORIZATION, Seq(AUTH_TOKEN)))),
          Json.obj(
            "first_name" -> NEW_FIRST,
            "last_name" -> NEW_LAST,
            "password" -> NEW_PASSWORD)))

      status(result) must equalTo(OK)
      contentAsJson(result) must equalTo(Json.obj(
        "first_name" -> NEW_FIRST,
        "last_name" -> NEW_LAST,
        "email" -> SOME_USER_EMAIL
      ))
    }

    "respond with 400 if the input cannot be parsed" in {
      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION1)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.updateUser(SOME_USER, Some(NEW_FIRST), None, None)).thenReturn(
        Future.successful(Some(getPatchedUser(SOME_USER, Some(NEW_FIRST), None, None))))

      val result = new TestController(EXPIRATION1).updateUser()(
        FakeRequest("PATCH", "/users/info", FakeHeaders(Seq((AUTHORIZATION, Seq(AUTH_TOKEN)))),
          Json.obj("first" -> NEW_FIRST)))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Cannot parse request body: should contain at least one of 'first_name', 'last_name', or 'password'.")
    }

    "respond with 500 if an error occurs during updateUser" in {
      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION1)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.updateUser(SOME_USER, Some(NEW_FIRST), None, None))
        .thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController(EXPIRATION1).updateUser()(
        FakeRequest("PATCH", "/users/info", FakeHeaders(Seq((AUTHORIZATION, Seq(AUTH_TOKEN)))),
          Json.obj("first_name" -> NEW_FIRST)))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "respond with 500 if we fail to update user" in {
      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION1)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.updateUser(SOME_USER, Some(NEW_FIRST), None, None))
        .thenReturn(Future.successful(None))

      val result = new TestController(EXPIRATION1).updateUser()(
        FakeRequest("PATCH", "/users/info", FakeHeaders(Seq((AUTHORIZATION, Seq(AUTH_TOKEN)))),
          Json.obj("first_name" -> NEW_FIRST)))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }
  }

  "GET /users/organizations" should {

    "respond with 200 and organizations" in {

      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION1)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(SOME_ORG_LIST))

      val result = new TestController(EXPIRATION1).getUserOrganizations()(
        FakeRequest("GET", "/users/organizations").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(OK)
      contentAsJson(result) must equalTo(Json.toJson(SOME_ORG_LIST))
    }

    "respond with 200 and an empty JSON array, if no organizations found" in {
      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION1)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List.empty[(String, Role)]))

      val result = new TestController(EXPIRATION1).getUserOrganizations()(
        FakeRequest("GET", "/users/organizations").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(OK)
      contentAsString(result) must equalTo("[]")
    }

    "respond with 500 if there's an error" in {
      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION1)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController(EXPIRATION1).getUserOrganizations()(
        FakeRequest("GET", "/users/organizations").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }
  }

  "GET /users/organizations/:org/teams" should {

    "respond with 200 and teams" in {

      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION1)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(SOME_ORG_NAME1)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(SOME_TEAM_LIST))

      val result = new TestController(EXPIRATION1).getUserTeams(SOME_ORG_NAME1)(
        FakeRequest("GET", s"/users/organizations/$SOME_ORG_NAME1/teams").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(OK)
      contentAsJson(result) must equalTo(Json.toJson(SOME_TEAM_LIST))
    }

    "respond with 200 and an empty JSON array, if no teams found" in {

      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION1)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(SOME_ORG_NAME1)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List.empty[(String, Role)]))

      val result = new TestController(EXPIRATION1).getUserTeams(SOME_ORG_NAME1)(
        FakeRequest("GET", s"/users/organizations/$SOME_ORG_NAME1/teams").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(OK)
      contentAsString(result) must equalTo("[]")
    }

    "respond with 400 if organization doesn't exist" in {
      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION1)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(SOME_ORG_NAME1)).thenReturn(Success(None))

      val result = new TestController(EXPIRATION1).getUserTeams(SOME_ORG_NAME1)(
        FakeRequest("GET", s"/users/organizations/$SOME_ORG_NAME1/teams").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 500 if an error occurs during getOrganization" in {
      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION1)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(SOME_ORG_NAME1)).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController(EXPIRATION1).getUserTeams(SOME_ORG_NAME1)(
        FakeRequest("GET", s"/users/organizations/$SOME_ORG_NAME1/teams").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "respond with 500 if an error occurs during getTeamsForUser" in {
      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION1)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(SOME_ORG_NAME1)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController(EXPIRATION1).getUserTeams(SOME_ORG_NAME1)(
        FakeRequest("GET", s"/users/organizations/$SOME_ORG_NAME1/teams").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }
  }

  "GET /users/search" should {

    "respond with 200 and users" in {
      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION1)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.findUser("*")).thenReturn(Future.successful(SOME_USER_LIST))

      val result = new TestController(EXPIRATION1).searchUsers("*")(
        FakeRequest("GET", "/users/search").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(OK)
      contentAsJson(result) must equalTo(Json.toJson(SOME_USER_LIST))
    }

    "respond with 200 and empty JSON array, if no matches found" in {
      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION1)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.findUser("*")).thenReturn(Future.successful(List.empty[User]))

      val result = new TestController(EXPIRATION1).searchUsers("*")(
        FakeRequest("GET", "/users/search").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(OK)
      contentAsString(result) must equalTo("[]")
    }

    "respond with 500 if an error occurs during findUser" in {
      when(mockDataManager.findUserByToken(SOME_TOKEN, EXPIRATION1)).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.findUser("*")).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController(EXPIRATION1).searchUsers("*")(
        FakeRequest("GET", "/users/search").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }
  }
}
