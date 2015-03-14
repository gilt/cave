# POST /organizations/{name}/teams/{team}/users
This operation adds an existing CAVE user to a team, with a given role. The role must be one of `admin`, `member`, or `viewer`. 

The operation can only be invoked by an administrator of the team. For security purposes, this operation will return `202 ACCEPTED` even if the email provided does not belong to an existing CAVE user.

### Resource URL

`https://api.cavellc.io/organizations/{name}/teams/{team}/users`

### Resource Information

The data must be formatted as JSON, with the following fields:

Field | Description | Notes
:---- | :---------- | :----
email | The email of the user to add | Mandatory
role | The role the user should have for this team | Mandatory

The request must be authenticated with a valid user token, as obtained from a login operation. See [POST /users/login](../users/login.md) for details. The token can be passed as the username (with an empty password) following the Basic Authentication scheme of the HTTP protocol. Alternatively, the same token can be accepted as a Bearer Token, similar to the OAuth2 specification.

### Example Request

    curl -i -u 8b896055-c295-4a30-a29c-5a97d15f1818: \
         -X POST -H "Content-Type: application/json" \
         -d '{
               "email": "vdumitrescu@gilt.com",
               "role": "member"
             }' \
         https://api.cavellc.io/organizations/acme/teams/runners/users


### Example Response

    HTTP/1.1 202 Accepted
    Content-Length: 0
    Connection: keep-alive

### See Also

* [Retrieve teams for organization](get-teams.md)
* [Create team](create-team.md)
* [Retrieve team info](get-team.md)
* [Delete team](delete-team.md)
* [Retrieve team users](get-team-users.md)
* [Modify role for user in team](modify-team-user.md)
* [Remove user from team](remove-team-user.md)

[Back to Teams](README.md)

[Back to API Main Page](../api.md)
