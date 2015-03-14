# POST /users/reset-password

This operation completes the process of resetting a user's password.

The request must contain a new password, and the confirmation token that was received in the email.

If successful, the request will return `200 OK`.


### Resource URL

`https://api.cavellc.io/users/reset-password`

### Resource Information

The data must be formatted as JSON, with the following fields:

Field | Description | Notes
:---- | :---------- | :----
password | The new password for the user | Mandatory
confirmation_token | The token received in the email | Mandatory


### Example Request

    curl -i -X POST \
         -H "Content-Type: application/json" \
         -d '{
               "password": "S3cr3t!",
               "confirmation_token": "5B5D427F-0C63-4938-A87B-D41A6E54E42C"
             }' \
         https://api.cavellc.io/users/reset-password


### Example Response

    HTTP/1.1 200 OK
    Content-Length: 0
    
### See Also

* [Begin user registration](register.md)
* [Complete user registration](confirm.md)
* [Begin user session](login.md)
* [Request password reset](forgot-password.md)
* [Retrieve user information](get-info.md)
* [Modify user information](patch-info.md)
* [Retrieve organizations for user](get-organizations.md)
* [Retrieve teams for user](get-teams.md)

[Back to Users](README.md)

[Back to API Main Page](../api.md)
