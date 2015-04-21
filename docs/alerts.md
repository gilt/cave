## Alert Configuration
An alert configuration consists of several pieces of information.

First, __a condition__. This is the logical condition to be evaluated, and has a special [grammar](#grammar).

Next, __a description__. This is a string that should describe what the alert is looking for. The best description explains what _has happened_, i.e. in words that assume the _condition_ we were looking for has occured. Examples:

* _"No heartbeat received from svc-important for 5 minutes"_
* _"The 5 minutes p99 of Response Time for svc-important is above SLA of 1.5 seconds for more than 15 minutes"_.

Also, __a period__. This tells CAVE how often to evaluate the _condition_. This can be expressed in one of two ways.

* To specify the period between each evaluation use a number followed by a letter: `s` for seconds, `m` for minutes, `h` for hours, or `d` for days. For example, a value of `5m` means `Evaluate every 5 minutes`.
* To specify a particular time every day when the alert should be evaluated use @hh:mm:ss. For example  `@13:30:00` means `Evaluate every day at 13:30:00 UTC`

Next, a boolean value for __enabled__. A value of `false` for this flag allows an alert to be defined, but not evaluated. If `true`, the alert is evaluated periodically, with the specified _period_.

Finally, the __routing__ is a metadata container that instructs CAVE how to deliver the alert when the _condition_ is met. This field is optional and the value depends on the delivery mechanism being integrated with. By default, CAVE will simply POST an alert as JSON to the organization URL. In addition to that we support [PagerDuty integration](#pagerduty).

### [Alert Grammar](id:grammar)
The _condition_ string specifies what the alert is for, what metric or metrics should be evaluated to detect a problem. There are 2 formats that we can parse: the comparison of two __data sources__ and the missing data condition.

We allow three types of __data sources__:

* _value data source_, consisting of a single numeric value, e.g. `2500`
* _metric data source_, consisting of a single metric, with or without tags, e.g. `orders [shipTo: US]`
* _aggregated metric data source_, consisting of a metric, an aggregator and a period of aggregation, e.g. `orders [shipTo: US].sum.5m`

The following aggregators are supported:

* `count`: number of events
* `min`, `max`, `mean`, `mode`, `median`, `sum`, `stddev`: statistical aggregators
* `p99`, `p999`, `p95`, `p90`: percentile aggregators

A comparison of two data sources uses any boolean operator (<, >, <=, >=, == or !=) and two sources, e.g.

* `orders [shipTo: US] < 10`  comparison between a metric and a value data sources.
* `orders [shipTo: US].sum.5m <= ordersPredictedLow5m [shipTo: US]` comparison between a metric data source and an aggregated metric data source.

For all comparisons, a number of data points can be specified to only create an alert when a condition is repeatedly detected. To do so, the following can be added to the condition: `at least N times`, where N is a positive integer. If it’s not specified, N defaults to 1, and the alert is triggered for every threshold breach, which sometimes leads to false alarms.

A missing data condition can only take a metric data source and a duration, and the alert is triggered if there has been no data for at least the given duration. The syntax for such an alert condition is as follows:

`heartbeat [service: svc-important] missing for 10m`

This alert will be triggered when the heartbeat metric of svc-important hasn’t been seen for at least 10 minutes.

### [PagerDuty integration](id:pagerduty)
If you're using PagerDuty, you need to do two things. First, you need to set the organization's notification URL to be the PagerDuty API URL:
`https://events.pagerduty.com/generic/2010-04-15/create_event.json`

Next, you need to create PagerDuty API Services and use the Service API keys in the `routing` field when defining alerts in CAVE. I.e.

```
{
  "description": "...",
  "condition": "...",
  "enabled": ...,
  "period": "...",
  "routing": { "pagerduty_service_api_key": "20a15998d0da43e7b203056b922d6c8a" }
}
```

When an alert fires, CAVE will create an incident for the given Service API key, which should trigger the escalation policy attached to that PagerDuty Service.
