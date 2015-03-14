# GET /organizations/{name}/teams
This operation retrieves the teams associated with an organization within CAVE. The response contains the team information, in JSON format.

The operation can be executed only by users that are administrators or members of the organization.

### Resource URL

`https://api.cavellc.io/organizations/{name}/teams`

### Resource Information

The request must be authenticated with a valid user token, as obtained from a login operation. See [POST /users/login](../users/login.md) for details. The token can be passed as the username (with an empty password) following the Basic Authentication scheme of the HTTP protocol. Alternatively, the same token can be accepted as a Bearer Token, similar to the OAuth2 specification.

### Example Request

    curl -i -u 8b896055-c295-4a30-a29c-5a97d15f1818: https://api.cavellc.io/organizations/acme/teams


### Example Response

    HTTP/1.1 200 OK
    Content-Type: application/json; charset=utf-8
    Content-Length: 171
    Connection: keep-alive

    [
      {
        "name": "runners",
        "tokens": [
          {
            "id": "79",
            "description": "default",
            "value": "hqocn64ghDoT3nShKtykBJdd60x6oOqPqoNjkwwLbHq6hRRMfhqa4VWk",
            "created": "2014-10-23T16:17:11.209Z"
          }
        ]
      }
    ]
    
### See Also

* [Create team](create-team.md)
* [Retrieve team info](get-team.md)
* [Delete team](delete-team.md)
* [Add user to team](add-team-user.md)
* [Retrieve team users](get-team-users.md)
* [Modify role for user in team](modify-team-user.md)
* [Remove user from team](remove-team-user.md)

[Back to Teams](README.md)

[Back to API Main Page](../api.md)
