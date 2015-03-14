# DELETE /organizations/{name}/teams/{team}
Call this operation to delete the team with the given name from a CAVE organization.
The operation can only be invoked by a team administrator.

### Resource URL

`https://api.cavellc.io/organizations/{name}/teams/{team}`

### Resource Information

The request must be authenticated with a valid user token, as obtained from a login operation. See [POST /users/login](../users/login.md) for details. The token can be passed as the username (with an empty password) following the Basic Authentication scheme of the HTTP protocol. Alternatively, the same token can be accepted as a Bearer Token, similar to the OAuth2 specification.

### Example Request

    curl -i -u 8b896055-c295-4a30-a29c-5a97d15f1818: -X DELETE https://api.cavellc.io/organizations/acme/teams/runners

### Example Response

    HTTP/1.1 204 No Content
    Content-length: 0
    Connection: keep-alive
    
### See Also

* [Retrieve teams for organization](get-teams.md)
* [Create team](create-team.md)
* [Retrieve team info](get-team.md)
* [Add user to team](add-team-user.md)
* [Retrieve team users](get-team-users.md)
* [Modify role for user in team](modify-team-user.md)
* [Remove user from team](remove-team-user.md)

[Back to Teams](README.md)

[Back to API Main Page](../api.md)
