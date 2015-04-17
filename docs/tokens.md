## Security Tokens

Most of the CAVE security is account based: each user is authenticated with a password, and this allows the user to create organizations and/or teams, be added to organizations and/or teams, and perform operations on them. 

However, one particular operation is not suited for account based security: publishing of data. This is always done by applications, and not by a human operator. As such, we need a different authentication for these, so that we do not put usernames and passwords in the application code or configuration. This is where the security tokens come in.

For every organization and team, we support up to three security tokens. One is created automatically with the organization/team, so that it is immediately available for publishing data. The organization or team Admin can manage the tokens: he can create new tokens, or delete existing tokens.

The idea is that a security token will be placed somewhere in code or configuration, and it will be used to authenticate requests to store metric data. To improve the security of this process, a team can choose to rotate tokens periodically. For example, a new token can be created every month, deployed to the applications that use the old token, then the old token can be deleted.

### Organization Tokens

A special note must be made with regard to Organization tokens. These can be used to store data not only at the organization level, but also for any team under that organization. This is particularly useful if a distributed data model is used, but there are applications that need to publish metrics for different teams.

For example, at Gilt we have developed a service that checks the health of all of our services at regular intervals (by calling each service's healthcheck endpoint). It publishes the results of these checks as a healthcheck metric to the CAVE team that owns the service. Thus, every service owner will get healthcheck metrics for every service they own. It is then up to them to create appropriate alerts based on these metrics.
