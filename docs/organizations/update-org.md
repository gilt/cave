# PATCH /organizations/{name}
This operation can be used to modify email or notification URL for an existing organization.

Only administrators of the organization can invoke this operation.

If the request is successful, the response contains the updated organization, in JSON format.

### Resource URL

`https://api.cavellc.io/organizations/{name}`

### Resource Information

The data must be formatted as JSON, with the following fields:

Field | Description | Notes
:---- | :---------- | :----
email | The email for the organization | Optional
notification_url | The URL where to post alert notifications for this organization | Optional

The request must be authenticated with a valid user token, as obtained from a login operation. See [POST /users/login](../users/login.md) for details. The token can be passed as the username (with an empty password) following the Basic Authentication scheme of the HTTP protocol. Alternatively, the same token can be accepted as a Bearer Token, similar to the OAuth2 specification.

### Example Request

    curl -i -u 8b896055-c295-4a30-a29c-5a97d15f1818: \
         -X PATCH -H "Content-Type: application/json" \         
         -d '{ "email": "cavellc@acme.inc" }' \
         https://api.cavellc.io/organizations/acme

### Example Response

    HTTP/1.1 200 OK
    Content-Type: application/json; charset=utf-8
    Content-Length: 249
    Connection: keep-alive

    {
      "name": "acme",
      "email": "cavellc@acme.inc",
      "notification_url": "https://api.acme.inc/notifications",
      "tokens": [
        {
          "id": "76",
          "description": "default",
          "value": "REKEfGtNqdITKDB0z2Ok7YcmouB5DtaSJhlK9TmruOA3jgW4YcSxsTGq",
          "created": "2014-10-23T14:51:06.087Z"
        }
      ]
    }

### See Also

* [Create organization](create-org.md)
* [Retrieve organization info](get-org.md)
* [Delete organization](delete-org.md)
* [Retrieve organization users](get-org-users.md)
* [Add user to organization](add-org-user.md)
* [Modify role for user in organization](modify-org-user.md)
* [Remove user from organization](remove-org-user.md)

[Back to Organizations](README.md)

[Back to API Main Page](../api.md)
