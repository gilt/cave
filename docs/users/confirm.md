# POST /users/confirm

This operation completes the process of signing up for CAVE, and creates the user.

The request must contain the user information, a password, and the confirmation token that was received in the registration email.

If successful, the request will return `201 CREATED`, as well as the user data in JSON format.


### Resource URL

`https://api.cavellc.io/users/confirm`

### Resource Information

The data must be formatted as JSON, with the following fields:

Field | Description | Notes
:---- | :---------- | :----
first_name | The first name of the user | Mandatory
last_name | The last name of the user | Mandatory
password | The desired password for the user | Mandatory
confirmation_token | The token received in the email | Mandatory



### Example Request

    curl -i -X POST \
         -H "Content-Type: application/json" \
         -d '{
               "first_name": "Joe",
               "last_name": "Appleseed",
               "password": "S3cr3t!",
               "confirmation_token": "5B5D427F-0C63-4938-A87B-D41A6E54E42C"
             }' \
         https://api.cavellc.io/users/confirm


### Example Response

    HTTP/1.1 201 Created
    Content-Type: application/json; charset=utf-8
    Content-Length: 78
    Connection: keep-alive
    
    {"first_name":"Joe","last_name":"Appleseed","email":"joe.appleseed@gmail.com"}
    
### See Also

* [Begin user registration](register.md)
* [Begin user session](login.md)
* [Request password reset](forgot-password.md)
* [Confirm password reset](reset-password.md)
* [Retrieve user information](get-info.md)
* [Modify user information](patch-info.md)
* [Retrieve organizations for user](get-organizations.md)
* [Retrieve teams for user](get-teams.md)

[Back to Users](README.md)

[Back to API Main Page](../api.md)
