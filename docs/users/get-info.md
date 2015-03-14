# GET /users/info

This operation retrieves information associated with the current user.

If successful, the request will return `200 OK`, as well as the user data in JSON format.


### Resource URL

`https://api.cavellc.io/users/info`

### Resource Information

The request must be authenticated with a valid user token, as obtained from a login operation. See [Begin user session](login.md) for details. The token can be passed as the username (with an empty password) following the Basic Authentication scheme of the HTTP protocol. Alternatively, the same token can be accepted as a Bearer Token, similar to the OAuth2 specification.


### Example Request

    curl -i -u 5B5D427F-0C63-4938-A87B-D41A6E54E42C: https://api.cavellc.io/users/info


### Example Response

    HTTP/1.1 200 OK
    Content-Type: application/json; charset=utf-8
    Content-Length: 78
    Connection: keep-alive
    
    {"first_name":"Joe","last_name":"Appleseed","email":"joe.appleseed@gmail.com"}
    
### See Also

* [Begin user registration](register.md)
* [Complete user registration](confirm.md)
* [Begin user session](login.md)
* [Request password reset](forgot-password.md)
* [Confirm password reset](reset-password.md)
* [Modify user information](patch-info.md)
* [Retrieve organizations for user](get-organizations.md)
* [Retrieve teams for user](get-teams.md)

[Back to Users](README.md)

[Back to API Main Page](../api.md)
