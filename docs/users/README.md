## Users

A user is a real-world person who is using CAVE. To become a user, you must sign up for CAVE, using a valid email address.
After registration, the user can login to obtain a session token, which can then be used to make API requests.

Operation | Description | Who can invoke
:------------ | :------------- | :------------
[`POST /users/register`](register.md) | Begin user registration | Anyone
[`POST /users/confirm`](confirm.md) | Complete user registration | Anyone
[`POST /users/login`](login.md) | Begin user session | Anyone
[`POST /users/forgot-password`](forgot-password.md) | Request password reset | Anyone
[`POST /users/reset-password`](reset-password.md) | Confirm password reset | Anyone
[`GET /users/info`](get-info.md) | Retrieve user information | Any user
[`PATCH /users/info`](patch-info.md) | Modify user information | Any user
[`GET /users/organizations`](get-organizations.md) | Retrieve organizations for this user | Any user
[`GET /users/organizations/{name}/teams`](get-teams.md) | Retrieve teams for this user | Any user

[Back to API Main Page](../api.md)
