# POST /users/register
This operation begins the process of signing up for CAVE. After receiving this request, CAVE will send an email to the registration address, containing a confirmation token. To complete the sign up process, an additional call must be invoked to confirm the email address. See [POST /users/confirm](confirm.md).

The result of this request is always `202 ACCEPTED`, even if the email address is already signed up for CAVE. This is on purpose, for security reasons.

### Resource URL

`https://api.cavellc.io/users/register`

### Resource Information

The data must be formatted as JSON, with the following fields:

Field | Description | Notes
:---- | :---------- | :----
email | The email address to register | Mandatory


### Example Request

    curl -i -X POST \
         -H "Content-Type: application/json" \
         -d '{ "email": "joe.appleseed@gmail.com" }' \
         https://api.cavellc.io/users/register


### Example Response

    HTTP/1.1 202 Accepted
    Content-Length: 0
    
### See Also

* [Complete user registration](confirm.md)
* [Begin user session](login.md)
* [Request password reset](forgot-password.md)
* [Confirm password reset](reset-password.md)
* [Retrieve user information](get-info.md)
* [Modify user information](patch-info.md)
* [Retrieve organizations for user](get-organizations.md)
* [Retrieve teams for user](get-teams.md)

[Back to Users](README.md)

[Back to API Main Page](../api.md)
