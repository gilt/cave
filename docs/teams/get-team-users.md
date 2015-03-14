# GET /organizations/{name}/teams/{team}/users
This operation retrieves the users associated with a team within CAVE. The response contains the user information and their roles, in JSON format.

The operation can be executed only by users that are administrators or members of the team.

### Resource URL

`https://api.cavellc.io/organizations/{name}/teams/{team}/users`

### Resource Information

The request must be authenticated with a valid user token, as obtained from a login operation. See [POST /users/login](../users/login.md) for details. The token can be passed as the username (with an empty password) following the Basic Authentication scheme of the HTTP protocol. Alternatively, the same token can be accepted as a Bearer Token, similar to the OAuth2 specification.

### Example Request

    curl -i -u 8b896055-c295-4a30-a29c-5a97d15f1818: https://api.cavellc.io/organizations/acme/team/runners/users


### Example Response

    HTTP/1.1 200 OK
    Content-Type: application/json; charset=utf-8
    Content-Length: 104
    Connection: keep-alive
    
    [
      {
        "user": {
          "first_name": "Joe",
          "last_name": "Appleseed",
          "email": "joe.appleseed@gmail.com"
        },
        "role": "admin"
      }
    ]
    
### See Also

* [Retrieve teams for organization](get-teams.md)
* [Create team](create-team.md)
* [Retrieve team info](get-team.md)
* [Delete team](delete-team.md)
* [Add user to team](add-team-user.md)
* [Modify role for user in team](modify-team-user.md)
* [Remove user from team](remove-team-user.md)

[Back to Teams](README.md)

[Back to API Main Page](../api.md)
