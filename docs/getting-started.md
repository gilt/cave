## Getting Started

This guide explains the steps that you need to take to begin using CAVE, and provides links to additional documentation.

### Registration

Navigate to the CAVE homepage, and click the Sign Up link.

Input a valid email address and click the **Sign Up** button.

Check your mailbox for a registration email from CAVE. Click the link in the email, and complete the registration process by inputting your first name, last name and a password.

Log in using the email and password provided during the registration process. You are now at your CAVE homepage.

### Data Grouping

The data you send to CAVE can be grouped into organizations and teams. For more information on these, see [this page](grouping.md).

Before you can begin sending metric data to CAVE, you need to create a new organization. Click the **Create Organization** button, input a name, an email for this organization, and a notification URL. This URL is where CAVE posts notifications about alerts. You cannot leave this blank, but you can put anything here, and you can change it later as needed. If you are using PagerDuty, you should put the PagerDuty API URL here, `https://events.pagerduty.com/generic/2010-04-15/create_event.json`.

After creating the organization, it should appear under **My Organizations**, and it should be automatically opened in the details view. This view shows that there are no teams yet, that you are the only user in this brand new organization, and it also shows that there is a default token which was created automatically for this organization. More information on security tokens [here](tokens.md).

If you need to group data into more granular groups, you can create one or more teams under your organization. To do so, you need to click the **Create New Team** button and provide a team name. To access a team details, including its security tokens, click the team name in the list.

### Sending Data

Before you start sending data to CAVE, please take a moment to read [this page](metrics.md) about metrics naming.

If you are ready to begin sending data for your organization or team, whether you use the apidoc generated client, or curl, or a client in the language of your choice, all you need is to make HTTP POST calls and provide the metric data in JSON format. For further details, see the API documentation for sending [organization metrics](metrics/publish-org-metrics.md) and [team metrics](metrics/publish-team-metrics.md).

Data received by CAVE will show up under **Metrics** for the organization or team that it was published for. Clicking the **Metrics** section at the top of the detail page for an organization or team will open the metric list for that group.

### Plotting Data

Each metric received has a **Graph** button attached. This button opens the metric graph page, which allows you to create a ploy of your metric. First, you need to specify an aggregator and the aggregation period. Next, you select a time period for which the plot should be created. Then click the **Plot** button to generate the graph.

In addition to these controls, there's a window that contains all the tag names that have been associated with this metric. By inputting values for specific tags, you can create a plot of data filtered for that particular tag value.

The plot area contains two graphs. The larger area at the top constitutes the detail view, while the narrow area at the bottom is the thumbnail view. Clicking and draging smaller areas across the thumbnail view will provide a zoom of the selected period into the detail view. Single click in the thumbnail view will reset the detail view to the whole graph period.

The bottom part of the graph page is related to Alert conditions. Prior to creating an alert, you can check the grammar on existing data, without actually triggering any events. This is possible by inputting an alert condition, selecting a period for the alert evaluation and clicking the **Evaluate Alert** button. The result should be over imposed on the existing graph, showing a value of zero for alert condition GREEN (alert condition not satisfied) and a value of one for alert condition RED (alert condition satisfied). See the [this page](alerts.md) for more details.

### Creating Alerts

To create an alert, you must provide an alert description, the condition to test, the test period (how often to check the condition). You can optionally provide a link to the alert handbook, and a PagerDuty service API key for notifications. If you are not using PagerDuty, you can leave this blank. Finally, you may choose to enable the alert immediately, or leave it disabled to be enabled at a later time. An alert that is disabled will not be evaluated, and it will not generate an alert history.

For a complete description of the alert condition DSL, please see [this page](alerts.md).