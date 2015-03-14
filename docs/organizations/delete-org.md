# DELETE /organizations/{name}
Call this operation to delete the organization with the given name.
The operation can only be invoked by an organization administrator, and it will only succeed if there are no teams in the organization.

### Resource URL

`https://api.cavellc.io/organizations/{name}`

### Resource Information

The request must be authenticated with a valid user token, as obtained from a login operation. See [POST /users/login](../users/login.md) for details. The token can be passed as the username (with an empty password) following the Basic Authentication scheme of the HTTP protocol. Alternatively, the same token can be accepted as a Bearer Token, similar to the OAuth2 specification.

### Example Request

    curl -i -u 8b896055-c295-4a30-a29c-5a97d15f1818: -X DELETE https://api.cavellc.io/organizations/acme

### Example Response

    HTTP/1.1 204 No Content
    Content-length: 0
    Connection: keep-alive
    
### See Also

* [Create organization](create-org.md)
* [Retrieve organization info](get-org.md)
* [Update organization](update-org.md)
* [Retrieve organization users](get-org-users.md)
* [Add user to organization](add-org-user.md)
* [Modify role for user in organization](modify-org-user.md)
* [Remove user from organization](remove-org-user.md)

[Back to Organizations](README.md)

[Back to API Main Page](../api.md)
