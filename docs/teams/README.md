## Teams

A team is another level of grouping users within an organization. Any organization administrator can create teams. The only
restriction is that the team name must be unique within the organization.

Organization and team administrators can add existing CAVE users to a team, remove users from the team or change the role
of a user within the team.

Operation | Description | Who can invoke
:------------ | :------------- | :------------
[`GET /organizations/{name}/teams`](get-teams.md) | Retrieve teams for organization | Org Admin or Member
[`POST /organizations/{name}/teams`](create-team.md) | Create team within organization | Org Admin
[`GET /organizations/{name}/teams/{team}`](get-team.md) | Retrieve team by name for organization | Team Admin or Member
[`DELETE /organizations/{name}/teams/{team}`](delete-team.md) | Delete team with given name | Team Admin
[`GET /organizations/{name}/teams/{team}/users`](get-team-users.md) | Retrieve team users | Team Admin or Member
[`POST /organizations/{name}/teams/{team}/users`](add-team-user.md) | Add user to team | Team Admin
[`PATCH /organizations/{name}/teams/{team}/users/{email}`](modify-team-user.md) | Update role for user in team | Team Admin
[`DELETE /organizations/{name}/teams/{team}/users/{email}`](remove-team-user.md) | Remove user from team | Team Admin

[Back to API Main Page](../api.md)
