# DELETE /organizations/{name}/alerts/{id}
This operation deletes an alert for a CAVE organization. See [this page](../alerts.md) for a primer on alert configuration.

The operation can only be invoked by an organization administrator or member.

### Resource URL

`https://api.cavellc.io/organizations/{name}/alerts/{id}`

### Resource Information

The request must be authenticated with a valid user token, as obtained from a login operation. See [POST /users/login](../users/login.md) for details. The token can be passed as the username (with an empty password) following the Basic Authentication scheme of the HTTP protocol. Alternatively, the same token can be accepted as a Bearer Token, similar to the OAuth2 specification.

### Example Request

    curl -X DELETE \
         -u 801f3fcc-0426-4ffd-9c33-8a0e1bae2411: \
         https://api.cavellc.io/organizations/gilt/alerts/8

### Example Response

    HTTP/1.1 204 No Content
    Content-Length: 0

    
### See Also

* [Get organization alerts](get-org-alerts.md)
* [Create organization alert](create-org-alert.md)
* [Get organization alert](get-org-alert.md)
* [Update organization alert](update-org-alert.md)
* [Get team alerts](get-team-alerts.md)
* [Create team alert](create-team-alert.md)
* [Get team alert](get-team-alert.md)
* [Update team alert](update-team-alert.md)
* [Delete team alert](delete-team-alert.md)

[Back to Alerts](README.md)

[Back to API Main Page](../api.md)
