## Organizations

An organization is a group of users using CAVE. Any user can create organizations, as long as the name is unique within CAVE.
After creating an organization, the user becomes its sole administrator, but there are APIs to allow an administrator to add
more users to the organization, remove users from organization, or change the role of an user within the organization.

The supported roles are:

* Administrator: can do all operations on the organization.
* Member: can do most operations on the organization.
* Viewer: can only view data associated with the organization.


Operation | Description | Who can invoke
:------------ | :------------- | :------------
[`POST /organizations`](create-org.md) | Create organization | Any user
[`GET /organizations/{name}`](get-org.md) | Retrieve organization with given name | Org Admin or Member
[`PATCH /organizations/{name}`](update-org.md) | Modify organization with given name | Org Admin
[`DELETE /organizations/{name}`](delete-org.md) | Delete organization with given name | Org Admin
[`GET /organizations/{name}/users`](get-org-users.md) | Retrieve organization users | Org Admin
[`POST /organizations/{name}/users`](add-org-user.md) | Add user to organization | Org Admin
[`PATCH /organizations/{name}/users/{email}`](modify-org-user.md) | Update role for user in organization | Org Admin
[`DELETE /organizations/{name}/users/{email}`](remove-org-user.md) | Remove user from organization | Org Admin

[Back to API Main Page](../api.md)
