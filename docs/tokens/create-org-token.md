# POST /organizations/{name}/tokens
This operation creates a new token for a CAVE organization. It requires a description, which can be useful to quickly identify tokens in a list. For example, if you rotate tokens monthly, you could use that in the description, e.g. "November 2014".

The operation can only be invoked by an administrator of the organization.

The response contains the created token, in JSON format.

### Resource URL

`https://api.cavellc.io/organizations/{name}/tokens`

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
         https://api.cavellc.io/organizations/acme/tokens


### Example Response

    HTTP/1.1 201 Created
    Content-Type: application/json; charset=utf-8
    Location: https://cavellc.io/organizations/acme/tokens/80
    Content-Length: 145
    Connection: keep-alive
    
    {
      "id": "80",
      "description": "November 2014",
      "value": "Ie7HVLvEP50EHwy2bzvONQZOi5yxAqhq8vARZ92unTcOrlOSkRMHFaUb",
      "created":"2014-10-23T16:32:33.324Z"
    }
    
### See Also

* [Get organization tokens](get-org-tokens.md)
* [Get organization token](get-org-token.md)
* [Delete organization token](delete-org-token.md)
* [Get team tokens](get-team-tokens.md)
* [Create team token](create-team-token.md)
* [Get team token](get-team-token.md)
* [Delete team token](delete-team-token.md)

[Back to Tokens](README.md)

[Back to API Main Page](../api.md)
