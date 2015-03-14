# GET /organizations/{name}/teams/{team}/metrics
This operation retrieves data for a metric in a CAVE team.

The operation can be invoked by any user of the team (any role).

The response contains the metric data points (timestamps and values), in JSON format.

### Resource URL

`https://api.cavellc.io/organizations/{name}/teams/{team}/metrics`

### Resource Information

The request must contain the following query string parameters:

Parameter | Description | Notes
:-------- | :---------- | :----
metric | The name of the metric to query | Mandatory
tags | The list of tags for the metric | Optional, Special format [^1]
start | The start date/time for the query | Optional [^2]
end | The end date/time for the query | Optional
limit | The maximum number of datapoints to return | Optional [^3] 

[^1]: Comma-separated list of colon-separated pairs of keys and values, e.g. `host:service01,env:production`
[^2]: Use `start` and `end` to paginate through the data.
[^3]: It defaults to 60, and it is capped at 60.

The request must be authenticated with a valid user token, as obtained from a login operation. See [POST /users/login](../users/login.md) for details. The token can be passed as the username (with an empty password) following the Basic Authentication scheme of the HTTP protocol. Alternatively, the same token can be accepted as a Bearer Token, similar to the OAuth2 specification.

### Example Request

    curl -i -u 8b896055-c295-4a30-a29c-5a97d15f1818: \
         "https://api.cavellc.io/organizations/acme/teams/runners/metrics?metric=RabbitMsgReceived&tags=environment:production,host:svc5&limit=5"


### Example Response

    HTTP/1.1 200 OK
    Content-Type: application/json; charset=utf-8
    Content-Length: 253
    Connection: keep-alive
    
    {
      "metrics": [
        { "time": "2014-10-24T12:37:28.000Z", "value": 1.0 },
        { "time": "2014-10-24T12:37:27.000Z", "value": 1.0 },
        { "time": "2014-10-24T12:37:33.000Z", "value": 1.0 },
        { "time": "2014-10-24T12:37:32.000Z", "value": 1.0 },
        { "time": "2014-10-24T12:37:30.000Z", "value": 1.0 }
      ]
    }
    
### See Also

* [List organization metrics](list-org-metrics.md)
* [Get organization metric data](get-org-metric-data.md)
* [Publish organization metric data](publish-org-metrics.md)
* [List team metrics](list-team-metrics.md)
* [Publish team metric data](publish-team-metrics.md)

[Back to Metrics](README.md)

[Back to API Main Page](../api.md)
