# PATCH /organizations/{name}/alerts/{id}
This operation modifies an existing alert for a CAVE organization. See [this page](../alerts.md) for a primer on alert configuration.

The operation can only be invoked by an organization administrator or member.

The response contains the updated alert, in JSON format.

### Resource URL

`https://api.cavellc.io/organizations/{name}/alerts/{id}`

### Resource Information

The data must be formatted as JSON, with the following fields:

Field | Description | Notes
:---- | :---------- | :----
description | A description for the alert | Optional
enabled | A boolean value for the alert status | Optional
period | A string to describe how often to evaluate the condition | Optional
routing | A map of strings, with information on alert notification routing | Optional

The request must be authenticated with a valid user token, as obtained from a login operation. See [POST /users/login](../users/login.md) for details. The token can be passed as the username (with an empty password) following the Basic Authentication scheme of the HTTP protocol. Alternatively, the same token can be accepted as a Bearer Token, similar to the OAuth2 specification.

### Example Request

    curl -X PATCH \
         -H "Content-Type: application/json" \
         -H "Accept: application/json" \
         -u 801f3fcc-0426-4ffd-9c33-8a0e1bae2411: \
         -d '{ "enabled": false }' \
         https://api.cavellc.io/organizations/gilt/alerts/8

### Example Response

    HTTP/1.1 200 OK
    Content-Type: application/json
    Content-Length: 312
    Location: https://api.cavellc.io/organizations/gilt/alerts/8
    {
      "id": "8",
      "description": "svc-important: 5m p99 of responseTime above SLA of 1500ms for 15 minutes",
      "enabled": false,
      "period": "5m",
      "condition": "responseTime [service: svc-important, env: production].p99.5m > 1500 at least 3 times",
      "routing": {
        "pagerduty_service_api_key": "AW823HSJISNSNSJISI1UI00UU0"
      }
    }

    
### See Also

* [Get organization alerts](get-org-alerts.md)
* [Create organization alert](create-org-alert.md)
* [Get organization alert](get-org-alert.md)
* [Delete organization alert](delete-org-alert.md)
* [Get team alerts](get-team-alerts.md)
* [Create team alert](create-team-alert.md)
* [Get team alert](get-team-alert.md)
* [Update team alert](update-team-alert.md)
* [Delete team alert](delete-team-alert.md)

[Back to Alerts](README.md)

[Back to API Main Page](../api.md)

