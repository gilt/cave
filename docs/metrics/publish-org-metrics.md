# POST /organizations/{name}/metrics
This operation sends data for a metric in CAVE for a given organization.

The operation can be invoked by an application that has an organization token.

### Resource URL

`https://api.cavellc.io/organizations/{name}/metrics`

### Resource Information
The data must be formatted as JSON. There must be a single field, called `metrics` which is an array of items, each with the following fields:

Field | Description | Notes
:---- | :---------- | :----
metric | A name for the metric | Mandatory
tags | An object containing the tag information of the metric | Mandatory
timestamp | The timestamp of the data point, in Epoch format[^1] | Mandatory
value | A numeric value of the metric | Mandatory

[^1]: Epoch is the number of seconds since Jan 1st 1970.

The request must be authenticated with a valid organization token. See [Tokens](../tokens/README.md) for details. The token can be passed as the username (with an empty password) following the Basic Authentication scheme of the HTTP protocol. Alternatively, the same token can be accepted as a Bearer Token, similar to the OAuth2 specification.

### Example Request

    curl -X POST -H "Content-Type: application/json" \
         -u 8b896055-c295-4a30-a29c-5a97d15f1818: \
         -d '{
               "metrics": [
                 { "name": "http.cache.hits", "timestamp": 1398788625,
                   "value": 123, "tags": { "service": "svc-important", "host": "svc12.prod.iad" }
                 },
                 { "name": "http.cache.misses", "timestamp": 1398788625,
                   "value": 12, "tags": { "service": "svc-important", "host": "svc12.prod.iad" }
                 }
               ]
             }' \
         https://api.cavellc.io/organizations/gilt/metrics


### Example Response

    HTTP/1.1 202 ACCEPTED
    Content-Length: 0
    
### See Also

* [List organization metrics](list-org-metrics.md)
* [Get organization metric data](get-org-metric-data.md)
* [List team metrics](list-team-metrics.md)
* [Get team metric data](get-team-metric-data.md)
* [Publish team metric data](publish-team-metrics.md)

[Back to Metrics](README.md)

[Back to API Main Page](../api.md)
