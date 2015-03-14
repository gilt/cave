# GET /organizations/{name}/teams/{team}/tokens
This operation retrieves all tokens for a team.

The operation can only be invoked by an administrator of the team.

The response contains the list of tokens, in JSON format.

### Resource URL

`https://api.cavellc.io/organizations/{name}/teams/{team}/tokens`

### Resource Information

The request must be authenticated with a valid user token, as obtained from a login operation. See [POST /users/login](../users/login.md) for details. The token can be passed as the username (with an empty password) following the Basic Authentication scheme of the HTTP protocol. Alternatively, the same token can be accepted as a Bearer Token, similar to the OAuth2 specification.

### Example Request

    curl -i -u 8b896055-c295-4a30-a29c-5a97d15f1818: https://api.cavellc.io/organizations/acme/teams/runners/tokens


### Example Response

    HTTP/1.1 200 OK
    Content-Type: application/json; charset=utf-8
    Location: https://cavellc.io/organizations/acme/teams/runners/tokens/Some(81)
    Content-Length: 147
    Connection: keep-alive

    [
      {
        "id": "81",
        "description": "November 2014",
        "value": "KEqN2BxvvNOmR7tzlUfHWqFhfJ5MVaYTdPr4YafUWfr9omeWBT5gpgTo",
        "created": "2014-10-23T16:37:23.097Z"
      }
    ]
    
### See Also

* [Get organization tokens](get-org-tokens.md)
* [Create organization token](create-org-token.md)
* [Get organization token](get-org-token.md)
* [Delete organization token](delete-org-token.md)
* [Create team token](create-team-token.md)
* [Get team token](get-team-token.md)
* [Delete team token](delete-team-token.md)

[Back to Tokens](README.md)

[Back to API Main Page](../api.md)
