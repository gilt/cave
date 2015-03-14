# CAVE API

The API service exposes all the functionality required to interact with CAVE.

## Core Concepts

CAVE has six core concepts, and the REST-ful API is centred around these:

* [Users](#users)
* [Organizations](#organizations)
* [Teams](#teams)
* [Tokens](#tokens)
* [Metrics](#metrics)
* [Alerts](#alerts)

### [Users](users/README.md)[](id:users)

A user is a real-world person who is using CAVE. To become a user, you must sign up for CAVE, using a valid email address.
After registration, the user can login to obtain a session token, which can then be used to make API requests.

Operation | Description | Who can invoke
:------------ | :------------- | :------------
[`POST /users/register`](users/register.md) | Begin user registration | Anyone
[`POST /users/confirm`](users/confirm.md) | Complete user registration | Anyone
[`POST /users/login`](users/login.md) | Begin user session | Anyone
[`POST /users/forgot-password`](users/forgot-password.md) | Request password reset | Anyone
[`POST /users/reset-password`](users/reset-password.md) | Confirm password reset | Anyone
[`GET /users/info`](users/get-info.md) | Retrieve user information | Any user
[`PATCH /users/info`](users/patch-info.md) | Modify user information | Any user
[`GET /users/organizations`](users/get-organizations.md) | Retrieve organizations for this user | Any user
[`GET /users/organizations/{name}/teams`](users/get-teams.md) | Retrieve teams for this user | Any user

### [Organizations](organizations/README.md)[](id:organizations)

An organization is a group of users using CAVE. Any user can create organizations, as long as the name is unique within CAVE.
After creating an organization, the user becomes its sole administrator, but there are APIs to allow an administrator to add
more users to the organization, remove users from organization, or change the role of an user within the organization.

The supported roles are:

* Administrator: can do all operations on the organization.
* Member: can do most operations on the organization.
* Viewer: can only view data associated with the organization.


Operation | Description | Who can invoke
:------------ | :------------- | :------------
[`POST /organizations`](organizations/create-org.md) | Create organization | Any user
[`GET /organizations/{name}`](organizations/get-org.md) | Retrieve organization with given name | Org Admin or Member
[`PATCH /organizations/{name}`](organizations/update-org.md) | Modify organization with given name | Org Admin
[`DELETE /organizations/{name}`](organizations/delete-org.md) | Delete organization with given name | Org Admin
[`GET /organizations/{name}/users`](organizations/get-org-users.md) | Retrieve organization users | Org Admin
[`POST /organizations/{name}/users`](organizations/add-org-user.md) | Add user to organization | Org Admin
[`PATCH /organizations/{name}/users/{email}`](organizations/modify-org-user.md) | Update role for user in organization | Org Admin
[`DELETE /organizations/{name}/users/{email}`](organizations/remove-org-user.md) | Remove user from organization | Org Admin

### [Teams](teams/README.md)[](id:teams)

A team is another level of grouping users within an organization. Any organization administrator can create teams. The only
restriction is that the team name must be unique within the organization.

Organization and team administrators can add existing CAVE users to a team, remove users from the team or change the role
of a user within the team.

Operation | Description | Who can invoke
:------------ | :------------- | :------------
[`GET /organizations/{name}/teams`](teams/get-teams.md) | Retrieve teams for organization | Org Admin or Member
[`POST /organizations/{name}/teams`](teams/create-team.md) | Create team within organization | Org Admin
[`GET /organizations/{name}/teams/{team}`](teams/get-team.md) | Retrieve team by name for organization | Team Admin or Member
[`DELETE /organizations/{name}/teams/{team}`](teams/delete-team.md) | Delete team with given name | Team Admin
[`GET /organizations/{name}/teams/{team}/users`](teams/get-team-users.md) | Retrieve team users | Team Admin or Member
[`POST /organizations/{name}/teams/{team}/users`](teams/add-team-user.md) | Add user to team | Team Admin
[`PATCH /organizations/{name}/teams/{team}/users/{email}`](teams/modify-team-user.md) | Update role for user in team | Team Admin
[`DELETE /organizations/{name}/teams/{team}/users/{email}`](teams/remove-team-user.md) | Remove user from team | Team Admin

### [Tokens](tokens/README.md)[](id:tokens)

The tokens are strings that are used to secure requests for writing metrics into CAVE. These can be stored inside applications
that are publishing metric data, and are generally known to all members of an organization or team.

A team token can be used to publish metric data for that team. However, organization tokens can be used to publish data
for any team, or at the organization level.

The REST API provides operations for token management, and each team will be responsible for their security tokens.
It is recommended that the tokens are rotated periodically, for security purposes.
There's a limit of 3 tokens per organization, and 3 tokens per team.

Operation | Description | Who can invoke
:------------ | :------------- | :------------
[`GET /organizations/{name}/tokens`](tokens/get-org-tokens.md) | Get organization tokens | Org Admin
[`POST /organizations/{name}/tokens`](tokens/create-org-token.md) | Create organization token | Org Admin
[`GET /organizations/{name}/tokens/{id}`](tokens/get-org-token.md) | Retrieve organization token with given id | Org Admin
[`DELETE /organizations/{name}/tokens/{id}`](tokens/delete-org-token.md) | Delete organization token | Org Admin
[`GET /organizations/{name}/teams/{team}/tokens`](tokens/get-team-tokens.md) | Get team tokens | Team Admin
[`POST /organizations/{name}/teams/{team}/tokens`](tokens/create-team-token.md) | Create team token | Team Admin
[`GET /organizations/{name}/teams/{team}/tokens/{id}`](tokens/get-team-token.md) | Retrieve team token with given id | Team Admin
[`DELETE /organizations/{name}/teams/{team}/tokens/{id}`](tokens/delete-team-token.md) | Delete team token | Team Admin

### [Metrics](metrics/README.md)[](id:metrics)

Sending metrics to CAVE API is done by POSTing data in JSON format to the metrics endpoint. These requests must be
authenticated using valid organization or team tokens, respectively.

Operation | Description | Who can invoke
:------------ | :------------- | :------------
[`POST /organizations/{name}/metrics`](metrics/publish-org-metrics.md) | Publish organization metric data | Application using org token
[`POST /organizations/{name}/teams/{team}/metrics`](metrics/publish-team-metrics.md) | Publish team metric data | Application using team token
[`GET /organizations/{name}/metrics`](metrics/get-org-metric-data.md) | Get organization metric data | Org Admin, Member or Viewer
[`GET /organizations/{name}/teams/{team}/metrics`](metrics/get-team-metric-data.md) | Get team metric data | Team Admin, Member or Viewer
[`GET /organizations/{name}/metric-names`](metrics/list-org-metrics.md) | List organization metrics | Org Admin, Member or Viewer
[`GET /organizations/{name}/teams/{team}/metric-names`](metrics/list-team-metrics.md) | List team metrics | Team Admin, Member or Viewer

### [Alerts](alerts/README.md)[](id:alerts)

Each alert consists of three parts: a condition to be verified, a period of verification, and how to notify the team
when the condition is red. In addition, alerts can be enabled or disabled.

Operation | Description | Who can invoke
:------------ | :------------- | :------------
[`GET /organizations/{name}/alerts`](alerts/get-org-alerts.md) | Retrieve organization alerts for given name | Org Admin or Member
[`POST /organizations/{name}/alerts`](alerts/create-org-alert.md) | Create organization alert  | Org Admin or Member
[`PATCH /organizations/{name}/alerts/{id}`](alerts/update-org-alert.md) | Modify organization alert with given id | Org Admin or Member
[`GET /organizations/{name}/alerts/{id}`](alerts/get-org-alert.md) | Retrieve organization alert with given id | Org Admin or Member
[`DELETE /organizations/{name}/alerts/{id}`](alerts/delete-org-alert.md) | Delete organization alert with given id | Org Admin or Member
[`GET /organizations/{name}/teams/{team}/alerts`](alerts/get-team-alerts.md) | Retrieve team alerts for given name | Team Admin or Member
[`POST /organizations/{name}/teams/{team}/alerts`](alerts/create-team-alert.md) | Create team alert  | Team Admin or Member
[`PATCH /organizations/{name}/teams/{team}/alerts/{id}`](alerts/update-team-alert.md) | Modify team alert with given id | Team Admin or Member
[`GET /organizations/{name}/teams/{team}/alerts/{id}`](alerts/get-team-alert.md) | Retrieve team alert with given id | Team Admin or Member
[`DELETE /organizations/{name}/teams/{team}/alerts/{id}`](alerts/delete-team-alert.md) | Delete team alert with given id | Team Admin or Member