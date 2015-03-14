# POST /organizations/{name}/teams/{team}/tokens
This operation creates a new token for a team. It requires a description, which can be useful to quickly identify tokens in a list. For example, if you rotate tokens monthly, you could use that in the description, e.g. "November 2014".

The operation can only be invoked by an administrator of the team.

The response contains the created token, in JSON format.

### Resource URL

`https://api.cavellc.io/organizations/{name}/teams/{team}/tokens`

### Resource Information

The data must be formatted as JSON, with the following fields:

Field | Description | Notes
:---- | :---------- | :----
description | A description for the token | Mandatory

The request must be authenticated with a valid user token, as obtained from a login operation. See [POST /users/login](../users/login.md) for details. The token can be passed as the username (with an empty password) following the Basic Authentication scheme of the HTTP protocol. Alternatively, the same token can be accepted as a Bearer Token, similar to the OAuth2 specification.

### Example Request

    curl -i -u 8b896055-c295-4a30-a29c-5a97d15f1818: \
         -X POST -H "Content-Type: application/json" \
         -d '{ "description": "November 2014" }' \
         https://api.cavellc.io/organizations/acme/teams/runners/tokens


### Example Response

    HTTP/1.1 201 Created
    Content-Type: application/json; charset=utf-8
    Location: https://cavellc.io/organizations/acme/teams/runners/tokens/Some(81)
    Content-Length: 145
    Connection: keep-alive

    {
      "id": "81",
      "description": "November 2014",
      "value": "KEqN2BxvvNOmR7tzlUfHWqFhfJ5MVaYTdPr4YafUWfr9omeWBT5gpgTo",
      "created": "2014-10-23T16:37:23.097Z"
    }
    
### See Also

* [Get organization tokens](get-org-tokens.md)
* [Create organization token](create-org-token.md)
* [Get organization token](get-org-token.md)
* [Delete organization token](delete-org-token.md)
* [Get team tokens](get-team-tokens.md)
* [Get team token](get-team-token.md)
* [Delete team token](delete-team-token.md)

[Back to Tokens](README.md)

[Back to API Main Page](../api.md)
