# POST /users/forgot-password

This operation begins the process of resetting a user's password.

The request must contain the registered email for which the password should be reset.

For security reasons, this request will always respond with `202 ACCEPTED`, even if the email doesn't exist in CAVE.

### Resource URL

`https://api.cavellc.io/users/forgot-password`

### Resource Information

The data must be formatted as JSON, with the following fields:

Field | Description | Notes
:---- | :---------- | :----
email | The email of the user | Mandatory


### Example Request

    curl -i -X POST \
         -H "Content-Type: application/json" \
         -d '{ "email": "jow.appleseed@gmail.com" }' \
         https://api.cavellc.io/users/forgot-password

### Example Response

    HTTP/1.1 202 Accepted
    Content-Length: 0
    Connection: keep-alive
    
### See Also

* [Begin user registration](register.md)
* [Complete user registration](confirm.md)
* [Begin user session](login.md)
* [Confirm password reset](reset-password.md)
* [Retrieve user information](get-info.md)
* [Modify user information](patch-info.md)
* [Retrieve organizations for user](get-organizations.md)
* [Retrieve teams for user](get-teams.md)

[Back to Users](README.md)

[Back to API Main Page](../api.md)
