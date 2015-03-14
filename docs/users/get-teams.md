# GET /users/organizations/{name}/teams

This operation retrieves the teams associated with the current user in the given organization.

If successful, the request will return `200 OK`, as well as the list of team names and the role for each, in JSON format.


### Resource URL

`https://api.cavellc.io/users/organizations/{name}/teams`

### Resource Information

The request must be authenticated with a valid user token, as obtained from a login operation. See [Begin user session](login.md) for details. The token can be passed as the username (with an empty password) following the Basic Authentication scheme of the HTTP protocol. Alternatively, the same token can be accepted as a Bearer Token, similar to the OAuth2 specification.


### Example Request

    curl -i -u 5B5D427F-0C63-4938-A87B-D41A6E54E42C: https://api.cavellc.io/users/organizations/acme/teams


### Example Response

    HTTP/1.1 200 OK
    Content-Type: application/json; charset=utf-8
    Content-Length: 66
    Connection: keep-alive

    [
      {
        "name": "runners",
        "role": "admin"
      },
      {
        "name": "loyalty",
        "role": "member"
      }
    ]
    
### See Also

* [Begin user registration](register.md)
* [Complete user registration](confirm.md)
* [Begin user session](login.md)
* [Request password reset](forgot-password.md)
* [Confirm password reset](reset-password.md)
* [Retrieve user information](get-info.md)
* [Modify user information](patch-info.md)
* [Retrieve organizations for user](get-organizations.md)

[Back to Users](README.md)

[Back to API Main Page](../api.md)
