# PATCH /users/info
This operation updates information associated with the current user. The following fields can be updated: first name, last name and password.

If successful, the request will return `200 OK`, as well as the user data in JSON format.

### Resource URL

`https://api.cavellc.io/users/info`

### Resource Information
The data must be formatted as JSON, with the following fields:

Field | Description | Notes
:---- | :---------- | :----
first_name | The first name of the user | Optional
last_name | The last name of the user | Optional
password | The desired password for the user | Optional

The request must be authenticated with a valid user token, as obtained from a login operation. See [Begin user session](login.md) for details. The token can be passed as the username (with an empty password) following the Basic Authentication scheme of the HTTP protocol. Alternatively, the same token can be accepted as a Bearer Token, similar to the OAuth2 specification.


### Example Request

    curl -i -u 0db4a221-95e4-4d5b-a3bd-b05f6721d15c: \
         -X PATCH -H "Content-Type: application/json" \
         -d '{ "first_name": "Jim" }' \
         https://api.cavellc.io/users/info


### Example Response

    HTTP/1.1 200 OK
    Content-Type: application/json; charset=utf-8
    Content-Length: 78
    Connection: keep-alive
    
    {"first_name":"Jim","last_name":"Appleseed","email":"joe.appleseed@gmail.com"}
    
### See Also

* [Begin user registration](register.md)
* [Complete user registration](confirm.md)
* [Begin user session](login.md)
* [Request password reset](forgot-password.md)
* [Confirm password reset](reset-password.md)
* [Retrieve user information](get-info.md)
* [Retrieve organizations for user](get-organizations.md)
* [Retrieve teams for user](get-teams.md)

[Back to Users](README.md)

[Back to API Main Page](../api.md)
