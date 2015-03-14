# POST /organizations

This operation creates a new organization within CAVE. It requires a unique name, which must be URL-safe. It also takes an email and a notification URL, which are solely needed for notifying alerts. By default, the alerts are notified on the notification URL, but if this fails, an email will be sent to the organization address.

The user that creates the organization is automatically added as an Administrator of the new organization. This allows the user to execute additional operations for the organization.

The API also creates an organization token named `default` and attaches it to the organization.

The response contains the whole organization, in JSON format.

### Resource URL

`https://api.cavellc.io/organizations`

### Resource Information

The data must be formatted as JSON, with the following fields:

Field | Description | Notes
:---- | :---------- | :----
name | The name of the organization | Mandatory
email | The email for the organization | Mandatory
notification_url | The URL where to post alert notifications for this organization | Mandatory

The request must be authenticated with a valid user token, as obtained from a login operation. See [POST /users/login](../users/login.md) for details. The token can be passed as the username (with an empty password) following the Basic Authentication scheme of the HTTP protocol. Alternatively, the same token can be accepted as a Bearer Token, similar to the OAuth2 specification.

### Example Request

    curl -i -u 8b896055-c295-4a30-a29c-5a97d15f1818: \
         -X POST -H "Content-Type: application/json" \
         -d '{ \
               "name": "acme",
               "email": "cave@acme.inc",
               "notification_url": "https://api.acme.inc/notifications"
             }' \
         https://api.cavellc.io/organizations


### Example Response

    HTTP/1.1 201 Created
    Content-Type: application/json; charset=utf-8
    Location: https://api.cavellc.io/organizations/acme
    Content-Length: 246
    Connection: keep-alive
    
    {
      "name": "acme",
      "email": "cave@acme.inc",
      "notification_url": "https://api.acme.inc/notifications",
      "tokens": [
        {
          "id: "76",
          "description": "default",
          "value": "REKEfGtNqdITKDB0z2Ok7YcmouB5DtaSJhlK9TmruOA3jgW4YcSxsTGq",
          "created": "2014-10-23T14:51:06.101Z"
        }
      ]
    }
    
### See Also

* [Retrieve organization info](get-org.md)
* [Update organization](update-org.md)
* [Delete organization](delete-org.md)
* [Retrieve organization users](get-org-users.md)
* [Add user to organization](add-org-user.md)
* [Modify role for user in organization](modify-org-user.md)
* [Remove user from organization](remove-org-user.md)

[Back to Organizations](README.md)

[Back to API Main Page](../api.md)

