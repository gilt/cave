# POST /organizations/{name}/users
This operation adds an existing CAVE user to an organization, with a given role. The role must be one of `admin`, `member`, or `viewer`. 

The operation can only be invoked by an administrator of the organization. For security purposes, this operation will return `202 ACCEPTED` even if the email provided does not belong to an existing CAVE user.

### Resource URL

`https://api.cavellc.io/organizations/{name}/users`

### Resource Information

The data must be formatted as JSON, with the following fields:

Field | Description | Notes
:---- | :---------- | :----
email | The email of the user to add | Mandatory
role | The role the user should have for this organization | Mandatory

The request must be authenticated with a valid user token, as obtained from a login operation. See [POST /users/login](../users/login.md) for details. The token can be passed as the username (with an empty password) following the Basic Authentication scheme of the HTTP protocol. Alternatively, the same token can be accepted as a Bearer Token, similar to the OAuth2 specification.

### Example Request

    curl -i -u 8b896055-c295-4a30-a29c-5a97d15f1818: \
         -X POST -H "Content-Type: application/json" \
         -d '{
               "email": "vdumitrescu@gilt.com",
               "role": "member"
             }' \
         https://api.cavellc.io/organizations/acme/users


### Example Response

    HTTP/1.1 202 Accepted
    Content-Length: 0
    Connection: keep-alive

### See Also

* [Create organization](create-org.md)
* [Retrieve organization info](get-org.md)
* [Update organization](update-org.md)
* [Delete organization](delete-org.md)
* [Retrieve organization users](get-org-users.md)
* [Modify role for user in organization](modify-org-user.md)
* [Remove user from organization](remove-org-user.md)

[Back to Organizations](README.md)

[Back to API Main Page](../api.md)
