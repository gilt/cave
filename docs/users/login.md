# POST /users/login
This operation is required for beginning a user session. It takes the email and password of the user, and returns a session token and its expiration date/time. The token can then be used for other API requests.

### Resource URL

`https://api.cavellc.io/users/login`

### Resource Information

The data must be formatted as JSON, with the following fields:

Field | Description | Notes
:---- | :---------- | :----
email | The email of the user | Mandatory
password | The password of the user | Mandatory

### Example Request

    curl -i -X POST \
         -H "Content-Type: application/json" \
         -d '{
               "email": "joe.appleseed@gmail.com",
               "password": "S3cr3t!"
             }' \
         https://api.cavellc.io/users/login


### Example Response

    HTTP/1.1 200 OK
    Content-Type: application/json; charset=utf-8
    Content-Length: 81
    Connection: keep-alive
    
    {"token":"fd2fa7c8-6778-49fe-8f2e-725850cb12c5","expires":"2014-10-23T12:49:41Z"}
    
### See Also

* [Begin user registration](register.md)
* [Complete user registration](confirm.md)
* [Request password reset](forgot-password.md)
* [Confirm password reset](reset-password.md)
* [Retrieve user information](get-info.md)
* [Modify user information](patch-info.md)
* [Retrieve organizations for user](get-organizations.md)
* [Retrieve teams for user](get-teams.md)

[Back to Users](README.md)

[Back to API Main Page](../api.md)
