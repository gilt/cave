## Metrics

Sending metrics to CAVE API is done by POSTing data in JSON format to the metrics endpoint. These requests must be
authenticated using valid organization or team tokens, respectively.

Data can be sent in one of two ways. If no teams are created, all data can be stored at the organization level, using an active organization token. A finer grained control can be achieved by creating teams and storing data at team level, by using a team token. The actual data is sent as JSON in the body of the request, and it is the same, regardless of the way it is sent.
A data point requires a metric name, a timestamp, a value, and an optional map of tags, as key-value pairs.

You may want to jump right in and start throwing data into CAVE, but to really take advantage of CAVE's power and flexibility, you may want to pause and think about your naming schema.

#### Naming Schema
Many metrics administrators are used to supplying a single name for their time series. For example, systems administrators used to RRD-style systems may name their time series `webserver01.sys.cpu.0.user`. The name tells us that the time series is recording the amount of time in user space for `cpu 0` on `webserver01`. This works great if you want to retrieve just the user time for that cpu core on that particular web server later on.

But what if the web server has 64 cores and you want to get the average time across all of them? Some systems allow you to specify a wild card such as `webserver01.sys.cpu.*.user` that would read all 64 files and aggregate the results. Alternatively, you could record a new time series called `webserver01.sys.cpu.user.all` that represents the same aggregate but you must now write '64 + 1' different time series. What if you had a thousand web servers and you wanted the average cpu time for all of your servers? You could craft a wild card query like `*.sys.cpu.*.user` and the system would open all 64,000 files, aggregate the results and return the data. Or you setup a process to pre-aggregate the data and write it to `webservers.sys.cpu.user.all`.

CAVE handles things a bit differently by introducing the idea of _tags_. Each time series still has a _metric name_, but it's much more generic, something that can be shared by many unique time series. Instead, the uniqueness comes from a combination of tag key/value pairs that allows for flexible queries with very fast aggregations.

Take the previous example where the metric was `webserver01.sys.cpu.0.user`. In CAVE, this may become

`sys.cpu.user [host:webserver01, cpu:0]`

where `sys.cpu.user` is the _metric name_, and there are two tags, named _host_ and _cpu_, with `webserver01` and `0` as their respective values.

Now if we want the data for an individual core, we can specify a metric like this:

`sys.cpu.user [host:webserver01, cpu: 42]`

If we want all of the cores, we simply drop the `cpu` tag and ask for:

`sys.cpu.user [host: webserver01]`

This will give us the aggregated results for all cores. If we want the results for all 1,000 servers, we simply request:

`sys.cpu.user`

The underlying data schema will store all of the `sys.cpu.user` time series next to each other so that aggregating the individual values is very fast and efficient. CAVE was designed to make these aggregate queries as fast as possible since most users start out at a high level, then drill down for detailed information.

#### Time Series Cardinality
A critical aspect of any naming schema is to consider the cardinality of your time series. Cardinality is defined as the number of unique items in a set. In the case of CAVE, this means the number of items associated with a metric, i.e. all of the possible tag name and value combinations, as well as the number of unique metric names, tag names and tag values.

You should avoid high cardinality time series, and only user tags where the number of values is known, finite and low.

High cardinality puts a lot of pressure on the database, and may lead to long time when inserting or querying data.

#### Naming Conclusion
When you design your naming schema, keep these suggestions in mind:

* Be consistent with your naming to reduce duplication.
* Always use the same case for metrics, tag names and values.
* Use the same number and type of tags for each metric. e.g. don't store `my.metric host=foo` and `my.metric datacenter=lga`.
* Think about the most common queries you'll be executing and optimize your schema for those queries.
* Think about how you may want to drill down when querying.
* Don't use too many tags, keep it to a fairly small number, usually up to 4 or 5 tags.
* The supported grammar for metric names, tag names and tag values is: `[a-zA-Z][_a-zA-Z0-9.-]*` (letters, digits, underscore, dot and dash, starting with a letter).

#### Aggregations
To obtain a single time series from multiple that match a metric name and a given set of tags, CAVE must user an aggregator, which is a function, and a time range for the aggregation. The end result is a single time series that has data points at the specified time range, and each value is obtained from the multiple time series that matched the query.

In the example above, if `sys.cpu.user` has a value between `0` and `100` for each `cpu` and each `host`, then the time series for one host might be obtained as an average of the metric for the individual cores. The aggregation period could be one minute, and then the resulting time series will have a value for every minute.

`sys.cpu.user [host: webserver01].avg.1m`

The following aggregators are supported:

* `count`: number of events
* `min`, `max`, `mean`, `mode`, `median`, `sum`, `stddev`: statistical aggregators
* `p99`, `p999`, `p95`, `p90`: percentile aggregators



Operation | Description | Who can invoke
:------------ | :------------- | :------------
[`POST /organizations/{name}/metrics`](publish-org-metrics.md) | Publish organization metric data | Application using org token
[`POST /organizations/{name}/teams/{team}/metrics`](publish-team-metrics.md) | Publish team metric data | Application using team token
[`GET /organizations/{name}/metrics`](get-org-metric-data.md) | Get organization metric data | Org Admin, Member or Viewer
[`GET /organizations/{name}/teams/{team}/metrics`](get-team-metric-data.md) | Get team metric data | Team Admin, Member or Viewer
[`GET /organizations/{name}/metric-names`](list-org-metrics.md) | List organization metrics | Org Admin, Member or Viewer
[`GET /organizations/{name}/teams/{team}/metric-names`](list-team-metrics.md) | List team metrics | Team Admin, Member or Viewer

[Back to API Main Page](../api.md)
