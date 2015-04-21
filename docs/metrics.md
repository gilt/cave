## Metrics

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
