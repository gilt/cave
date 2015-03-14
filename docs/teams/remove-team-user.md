# DELETE /organizations/{name}/teams/{team}/users/{email}
This operation can be used to remove a user from a team.

Only administrators of the team can invoke this operation.

### Resource URL

`https://api.cavellc.io/organizations/{name}/teams/{team}/users/{email}`

### Resource Information

The request must be authenticated with a valid user token, as obtained from a login operation. See [POST /users/login](../users/login.md) for details. The token can be passed as the username (with an empty password) following the Basic Authentication scheme of the HTTP protocol. Alternatively, the same token can be accepted as a Bearer Token, similar to the OAuth2 specification.

### Example Request

    curl -i -u 8b896055-c295-4a30-a29c-5a97d15f1818: \
         -X DELETE \
         https://api.cavellc.io/organizations/acme/teams/runners/users/vdumitrescu%40gilt.com

### Example Response

    HTTP/1.1 204 No Content
    Content-length: 0
    Connection: keep-alive
    
### See Also

* [Retrieve teams for organization](get-teams.md)
* [Create team](create-team.md)
* [Retrieve team info](get-team.md)
* [Delete team](delete-team.md)
* [Add user to team](add-team-user.md)
* [Retrieve team users](get-team-users.md)
* [Modify role for user in team](modify-team-user.md)

[Back to Teams](README.md)

[Back to API Main Page](../api.md)
