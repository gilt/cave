# GET /organizations/{name}/metric-names
This operation retrieves metric names for a CAVE organization.

The operation can be invoked by any user of the organization (any role).

The response contains the list of metrics (names and tags), in JSON format.

### Resource URL

`https://api.cavellc.io/organizations/{name}/metric-names`

### Resource Information

The request must be authenticated with a valid user token, as obtained from a login operation. See [POST /users/login](../users/login.md) for details. The token can be passed as the username (with an empty password) following the Basic Authentication scheme of the HTTP protocol. Alternatively, the same token can be accepted as a Bearer Token, similar to the OAuth2 specification.

### Example Request

    curl -i -u 8b896055-c295-4a30-a29c-5a97d15f1818: https://api.cavellc.io/organizations/acme/metrics


### Example Response

    HTTP/1.1 200 OK
    Content-Type: application/json; charset=utf-8
    Content-Length: 67
    Connection: keep-alive
    
    [
      {
        "name": "RabbitMsgReceived",
        "tags": [ "host", "environment", "type" ]
      }
    ]
    
### See Also

* [Get organization metric data](get-org-metric-data.md)
* [Publish organization metric data](publish-org-metrics.md)
* [List team metrics](list-team-metrics.md)
* [Get team metric data](get-team-metric-data.md)
* [Publish team metric data](publish-team-metrics.md)

[Back to Metrics](README.md)

[Back to API Main Page](../api.md)
