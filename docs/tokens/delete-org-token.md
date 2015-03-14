# DELETE /organizations/{name}/tokens/{id}
This operation deletes a token for a CAVE organization.

The operation can only be invoked by an administrator of the organization.

It is not possible to delete the last remaining token of the organization.

### Resource URL

`https://api.cavellc.io/organizations/{name}/tokens/{id}`

### Resource Information

The request must be authenticated with a valid user token, as obtained from a login operation. See [POST /users/login](../users/login.md) for details. The token can be passed as the username (with an empty password) following the Basic Authentication scheme of the HTTP protocol. Alternatively, the same token can be accepted as a Bearer Token, similar to the OAuth2 specification.

### Example Request

    curl -i -u 8b896055-c295-4a30-a29c-5a97d15f1818: \
         -X DELETE https://api.cavellc.io/organizations/acme/tokens/80


### Example Response

    HTTP/1.1 204 No Content
    Content-Length: 0
    Connection: keep-alive
    
### See Also

* [Get organization tokens](get-org-tokens.md)
* [Create organization token](create-org-token.md)
* [Get organization token](get-org-token.md)
* [Get team tokens](get-team-tokens.md)
* [Create team token](create-team-token.md)
* [Get team token](get-team-token.md)
* [Delete team token](delete-team-token.md)

[Back to Tokens](README.md)

[Back to API Main Page](../api.md)
