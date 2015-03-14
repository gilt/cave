# GET /organizations/{name}/alerts
This operation retrieves all alerts for a CAVE organization. See [this page](../alerts.md) for a primer on alert configuration.

The operation can only be invoked by an organization administrator or member.

The response contains the list of alerts, in JSON format. The results can be paginated using two query string parameters:

* `next` the identifier received from a previous call, the next alert to be retrieved
* `limit` the number of alerts to fetch in one call. Defaults to 100. Capped at 1000.

If none of these are specified, the call will return the first 100 alerts. The response will contain a `Link` header with the URL for retrieving the next batch of alerts.

### Resource URL

`https://api.cavellc.io/organizations/{name}/alerts`

### Resource Information

The request must be authenticated with a valid user token, as obtained from a login operation. See [POST /users/login](../users/login.md) for details. The token can be passed as the username (with an empty password) following the Basic Authentication scheme of the HTTP protocol. Alternatively, the same token can be accepted as a Bearer Token, similar to the OAuth2 specification.

### Example Request

    curl -X GET \
         -H "Accept: application/json" \
         -u 801f3fcc-0426-4ffd-9c33-8a0e1bae2411: \
         https://api.cavellc.io/organizations/gilt/alerts?limit=2

### Example Response

    HTTP/1.1 200 OK
    Content-Type: application/json; charset=utf-8
    Link: <https://api.cavellc.io/organizations/gilt/alerts?next=31&limit=2>; rel="next"
    Content-Length: 579
    Connection: keep-alive
    
    [
      {
        "id": "30",
        "description": "svc-loyalty on svc5 has not received rabbit messages for over 5 minutes",
        "enabled":true,
        "period":"5m",
        "condition":"RabbitMsgReceived [host: svc5, environment: production] missing for 5m",
        "routing":{"pagerduty_service_api_key":"2ef64474126d42098d484f7bc27d809f"}
      },
      {
        "id": "31",
        "description": "svc-loyalty on svc6 has not received rabbit messages for over 5 minutes",
        "enabled": true,
        "period": "5m",
        "condition": "RabbitMsgReceived [host: svc6, environment: production] missing for 5m",
        "routing":{"pagerduty_service_api_key":"2ef64474126d42098d484f7bc27d809f"}
      }
    ]

    
### See Also

* [Create organization alert](create-org-alert.md)
* [Get organization alert](get-org-alert.md)
* [Update organization alert](update-org-alert.md)
* [Delete organization alert](delete-org-alert.md)
* [Get team alerts](get-team-alerts.md)
* [Create team alert](create-team-alert.md)
* [Get team alert](get-team-alert.md)
* [Update team alert](update-team-alert.md)
* [Delete team alert](delete-team-alert.md)

[Back to Alerts](README.md)

[Back to API Main Page](../api.md)

