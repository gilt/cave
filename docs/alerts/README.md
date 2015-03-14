## Alerts

Each alert consists of three parts: a condition to be verified, a period of verification, and how to notify the team
when the condition is red. In addition, alerts can be enabled or disabled.

See [this page](../alerts.md) for a primer on alert configuration.

Operation | Description | Who can invoke
:------------ | :------------- | :------------
[`GET /organizations/{name}/alerts`](get-org-alerts.md) | Retrieve organization alerts for given name | Org Admin or Member
[`POST /organizations/{name}/alerts`](create-org-alert.md) | Create organization alert  | Org Admin or Member
[`PATCH /organizations/{name}/alerts/{id}`](update-org-alert.md) | Modify organization alert with given id | Org Admin or Member
[`GET /organizations/{name}/alerts/{id}`](get-org-alert.md) | Retrieve organization alert with given id | Org Admin or Member
[`DELETE /organizations/{name}/alerts/{id}`](delete-org-alert.md) | Delete organization alert with given id | Org Admin or Member
[`GET /organizations/{name}/teams/{team}/alerts`](get-team-alerts.md) | Retrieve team alerts for given name | Team Admin or Member
[`POST /organizations/{name}/teams/{team}/alerts`](create-team-alert.md) | Create team alert  | Team Admin or Member
[`PATCH /organizations/{name}/teams/{team}/alerts/{id}`](update-team-alert.md) | Modify team alert with given id | Team Admin or Member
[`GET /organizations/{name}/teams/{team}/alerts/{id}`](get-team-alert.md) | Retrieve team alert with given id | Team Admin or Member
[`DELETE /organizations/{name}/teams/{team}/alerts/{id}`](delete-team-alert.md) | Delete team alert with given id | Team Admin or Member

[Back to API Main Page](../api.md)