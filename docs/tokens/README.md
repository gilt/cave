## Tokens

The tokens are strings that are used to secure requests for writing metrics into CAVE. These can be stored inside applications
that are publishing metric data, and are generally known to all members of an organization or team.

A team token can be used to publish metric data for that team. However, organization tokens can be used to publish data
for any team, or at the organization level.

The REST API provides operations for token management, and each team will be responsible for their security tokens.
It is recommended that the tokens are rotated periodically, for security purposes.
There's a limit of 3 tokens per organization, and 3 tokens per team.

Operation | Description | Who can invoke
:------------ | :------------- | :------------
[`GET /organizations/{name}/tokens`](get-org-tokens.md) | Get organization tokens | Org Admin
[`POST /organizations/{name}/tokens`](create-org-token.md) | Create organization token | Org Admin
[`GET /organizations/{name}/tokens/{id}`](get-org-token.md) | Retrieve organization token with given id | Org Admin
[`DELETE /organizations/{name}/tokens/{id}`](delete-org-token.md) | Delete organization token | Org Admin
[`GET /organizations/{name}/teams/{team}/tokens`](get-team-tokens.md) | Get team tokens | Team Admin
[`POST /organizations/{name}/teams/{team}/tokens`](create-team-token.md) | Create team token | Team Admin
[`GET /organizations/{name}/teams/{team}/tokens/{id}`](get-team-token.md) | Retrieve team token with given id | Team Admin
[`DELETE /organizations/{name}/teams/{team}/tokens/{id}`](delete-team-token.md) | Delete team token | Team Admin

[Back to API Main Page](../api.md)
