## Organizations

[Create Organization](#create-organization)

[Get Organization](#get-organization)

[Update Organization](#update-organization)

[Delete Organization](#delete-organization)

[Get Teams for Organization](#get-teams-for-organization)

## Teams

[Create Team](#create-team)

[Get Team](#get-team)

[Delete Team](#delete-team)

## Tokens

[Create Organization Token](#create-organization-token)

[Delete Organization Token](#delete-organization-token)

[Get Organization Tokens](#get-organization-tokens)

[Create Team Token](#create-team-token)

[Delete Team Token](#delete-team-token)

[Get Team Tokens](#get-team-tokens)

## Metrics

[Create Organization Metrics](#create-organization-metrics)

[Create Organization Metrics - Shortcut](#create-organization-metrics--shortcut)

[Create Team Metrics](#create-team-metrics)

[Create Team Metrics - Shortcut](#create-team-metrics--shortcut)


Create Organization
===================

Request

    curl -i -X POST \
      -H "Content-Type: application/json" \
      -H "Accept: application/json" \
      -d '{
         "name": "gilt",
         "email": "cave@gilt.com",
         "notification_url": "https://cave.gilt.com/incoming"
         }' \
      https://cavellc.io/organizations

Response

    HTTP/1.1 201 Created
    Content-Type: application/json; charset=utf-8
    Location: https://cavellc.io/organizations/gilt
    Content-Length: 270

    {
      "name": "gilt",
      "email": "cave@gilt.com",
      "notification_url": "https://cave.gilt.com/incoming",
      "tokens":[
        {
          "id": "145c5d4d65f6SNGP842zxVDs2WMWQ3",
          "description": "default",
          "value": "KvZVNEQNaRstBTDPJtnKj7omzTq03ZEkOgVSevWfxQmJMA42upXph17R",
          "created": "2014-05-16T13:14:58.892Z"
        }
      ]
    }


Create Team
===========

Request

    curl -i -X POST \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer KvZVNEQNaRstBTDPJtnKj7omzTq03ZEkOgVSevWfxQmJMA42upXph17R" \
      -H "Accept: application/json" \
      -d '{
         "name": "ouroboros"
         }' \
      https://cavellc.io/organizations/gilt/teams

Response

    HTTP/1.1 201 Created
    Content-Type: application/json; charset=utf-8
    Location: https://cavellc.io/organizations/gilt/teams/ouroboros
    Content-Length: 199

    {
      "name": "ouroboros",
      "tokens": [
        {
          "id": "145c5d6c0bdBYnc1BLROqlwVDZiINW",
          "description": "default",
          "value": "zHDHdk36OXRk81pOMIupyfNcwG3Z22fWXP40SWnjGhdMFiEOeBF8UvN4",
          "created": "2014-05-16T13:17:04.426Z"
        }
      ]
    }


Create Organization Token
=========================

Request

    curl -i -X POST \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer KvZVNEQNaRstBTDPJtnKj7omzTq03ZEkOgVSevWfxQmJMA42upXph17R" \
      -H "Accept: application/json" \
      -d '{
         "description": "June 2014"
         }' \
      https://cavellc.io/organizations/gilt/tokens

Response

    HTTP/1.1 201 Created
    Content-Type: application/json; charset=utf-8
    Location: https://cavellc.io/organizations/gilt/tokens/145c5d831d3qQWM3tG1lhrVvmAOj4C
    Content-Length: 169

    {
      "id": "145c5d831d3qQWM3tG1lhrVvmAOj4C",
      "description": "June 2014",
      "value": "cfrrXmxWy5pwlnh9aZwbQa2wvNqpohHnc3eOEMc8ucA5flvSMqLNfaWE",
      "created": "2014-05-16T13:18:38.912Z"
    }


Get Organization
================

Request

    curl -i -X GET \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer KvZVNEQNaRstBTDPJtnKj7omzTq03ZEkOgVSevWfxQmJMA42upXph17R" \
      -H "Accept: application/json" \
      https://cavellc.io/organizations/gilt

Response

    HTTP/1.1 200 OK
    Content-Type: application/json; charset=utf-8
    Content-Length: 440

    {
      "name": "gilt",
      "email": "cave@gilt.com",
      "notification_url": "https://cave.gilt.com/incoming",
      "tokens":[
        {
          "id": "145c5d4d65f6SNGP842zxVDs2WMWQ3",
          "description": "default",
          "value": "KvZVNEQNaRstBTDPJtnKj7omzTq03ZEkOgVSevWfxQmJMA42upXph17R",
          "created": "2014-05-16T13:14:58.892Z"
        },{
          "id": "145c5d831d3qQWM3tG1lhrVvmAOj4C",
          "description": "June 2014",
          "value": "cfrrXmxWy5pwlnh9aZwbQa2wvNqpohHnc3eOEMc8ucA5flvSMqLNfaWE",
          "created": "2014-05-16T13:18:38.912Z"
        }
      ]
    }


Delete Organization Token
=========================

Request

    curl -i -X DELETE \
      -H "Authorization: Bearer cfrrXmxWy5pwlnh9aZwbQa2wvNqpohHnc3eOEMc8ucA5flvSMqLNfaWE" \
      https://cavellc.io/organizations/gilt/tokens/145c5d4d65f6SNGP842zxVDs2WMWQ3

Response

    HTTP/1.1 204 No Content
    Content-Length: 0


Get Organization Tokens
=======================

Request

    curl -i -X GET \
      -H "Authorization: Bearer cfrrXmxWy5pwlnh9aZwbQa2wvNqpohHnc3eOEMc8ucA5flvSMqLNfaWE" \
      https://cavellc.io/organizations/gilt/tokens

Response

    HTTP/1.1 200 OK
    Content-Type: application/json; charset=utf-8
    Content-Length: 171

    [
      {
        "id": "145c5d831d3qQWM3tG1lhrVvmAOj4C",
        "description": "June 2014",
        "value": "cfrrXmxWy5pwlnh9aZwbQa2wvNqpohHnc3eOEMc8ucA5flvSMqLNfaWE",
        "created": "2014-05-16T13:18:38.912Z"
      }
    ]


Create Team Token
=================

Request

    curl -i -X POST \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer zHDHdk36OXRk81pOMIupyfNcwG3Z22fWXP40SWnjGhdMFiEOeBF8UvN4" \
      -H "Accept: application/json" \
      -d '{
         "description": "Ouroboros June 2014"
         }' \
      https://cavellc.io/organizations/gilt/teams/ouroboros/tokens

Response

    HTTP/1.1 201 Created
    Content-Type: application/json; charset=utf-8
    Location: https://cavellc.io/organizations/gilt/teams/ouroboros/tokens/145c5ede532HfKAk9PKpwrBTb936SN
    Content-Length: 179

    {
      "id": "145c5ede532HfKAk9PKpwrBTb936SN",
      "description": "Ouroboros June 2014",
      "value": "xZxKaJTVChsdCTC2dLkyVKGPGRwIFFhpLdOpvxXbvdKGws4r6MuXasGy",
      "created":"2014-05-16T13:42:21.087Z"
    }

Get Team
========

Request

    curl -i -X GET \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer xZxKaJTVChsdCTC2dLkyVKGPGRwIFFhpLdOpvxXbvdKGws4r6MuXasGy" \
      -H "Accept: application/json" \
      https://cavellc.io/organizations/gilt/teams/ouroboros

Response

    HTTP/1.1 200 OK
    Content-Type: application/json; charset=utf-8
    Content-Length: 379

    {
      "name":"ouroboros",
      "tokens":[
        {
          "id": "145c5d6c0bdBYnc1BLROqlwVDZiINW",
          "description": "default",
          "value": "zHDHdk36OXRk81pOMIupyfNcwG3Z22fWXP40SWnjGhdMFiEOeBF8UvN4",
          "created": "2014-05-16T13:17:04.426Z"
        },{
          "id": "145c5ede532HfKAk9PKpwrBTb936SN",
          "description": "Ouroboros June 2014",
          "value": "xZxKaJTVChsdCTC2dLkyVKGPGRwIFFhpLdOpvxXbvdKGws4r6MuXasGy",
          "created": "2014-05-16T13:42:21.087Z"
        }
      ]
    }


Delete Team Token
=================

Request

    curl -i -X DELETE \
      -H "Authorization: Bearer xZxKaJTVChsdCTC2dLkyVKGPGRwIFFhpLdOpvxXbvdKGws4r6MuXasGy" \
      https://cavellc.io/organizations/gilt/teams/ouroboros/tokens/145c5d6c0bdBYnc1BLROqlwVDZiINW

Response

    HTTP/1.1 204 No Content
    Content-Length: 0

Get Team Tokens
=======================

Request

    curl -i -X GET \
      -H "Authorization: Bearer xZxKaJTVChsdCTC2dLkyVKGPGRwIFFhpLdOpvxXbvdKGws4r6MuXasGy" \
      https://cavellc.io/organizations/gilt/teams/ouroboros/tokens

Response

    HTTP/1.1 200 OK
    Content-Type: application/json; charset=utf-8
    Content-Length: 181

    [
      {
        "id": "145c5ede532HfKAk9PKpwrBTb936SN",
        "description": "Ouroboros June 2014",
        "value": "xZxKaJTVChsdCTC2dLkyVKGPGRwIFFhpLdOpvxXbvdKGws4r6MuXasGy",
        "created": "2014-05-16T13:42:21.087Z"
      }
    ]


Create Organization Metrics
===========================

Request

    curl -i -X POST \
      -H "Content-Type: application/json" \
      -H "Accept: application/json" \
      -H "Authorization: Bearer cfrrXmxWy5pwlnh9aZwbQa2wvNqpohHnc3eOEMc8ucA5flvSMqLNfaWE" \
      -d '{
        "name": "orders",
        "timestamp": 1400251578,
        "value": 21,
        "tags": {
          "shipTo": "US"
        }
      }' \
      https://cavellc.io/organizations/gilt/metrics

Response

    HTTP/1.1 201 Created
    Content-Length: 0


Create Team Metrics
===================

    curl -i -X POST \
      -H "Content-Type: application/json" \
      -H "Accept: application/json" \
      -H "Authorization: Bearer xZxKaJTVChsdCTC2dLkyVKGPGRwIFFhpLdOpvxXbvdKGws4r6MuXasGy" \
      -d '[
      {
        "name": "responseTime",
        "timestamp": 1400251578,
        "value": 300,
        "tags": {
          "service": "svc-important",
          "host": "svc10"
        }
      },{
        "name": "requests",
        "timestamp": 1400251578,
        "value": 12,
        "tags": {
          "service": "svc-important",
          "host": "svc10"
        }
      }]' \
      https://cavellc.io/organizations/gilt/teams/ouroboros/metrics

Response

    HTTP/1.1 201 Created
    Content-Length: 0


Create Organization Metrics / Shortcut
======================================

Request

    curl -i -X POST \
      -H "Content-Type: application/json" \
      -H "Accept: application/json" \
      -H "Authorization: Bearer cfrrXmxWy5pwlnh9aZwbQa2wvNqpohHnc3eOEMc8ucA5flvSMqLNfaWE" \
      -d '{
        "name": "orders",
        "timestamp": 1410251578,
        "value": 21,
        "tags": {
          "shipTo": "US"
        }
      }' \
      https://gilt.cavellc.io/metrics

Response

    HTTP/1.1 201 Created
    Content-Length: 0


Create Team Metrics / Shortcut
==============================

Request

    curl -i -X POST \
      -H "Content-Type: application/json" \
      -H "Accept: application/json" \
      -H "Authorization: Bearer xZxKaJTVChsdCTC2dLkyVKGPGRwIFFhpLdOpvxXbvdKGws4r6MuXasGy" \
      -d '[
      {
        "name": "responseTime",
        "timestamp": 1410251578,
        "value": 300,
        "tags": {
          "service": "svc-important",
          "host": "svc10"
        }
      },{
        "name": "requests",
        "timestamp": 1410251578,
        "value": 12,
        "tags": {
          "service": "svc-important",
          "host": "svc10"
        }
      }]' \
      https://ouroboros.gilt.cavellc.io/metrics

Response

    HTTP/1.1 201 Created
    Content-Length: 0

Update Organization
===================

Request

    curl -i -X PUT \
      -H "Content-Type: application/json" \
      -H "Accept: application/json" \
      -H "Authorization: Bearer cfrrXmxWy5pwlnh9aZwbQa2wvNqpohHnc3eOEMc8ucA5flvSMqLNfaWE" \
      -d '{
        "email": "cavellc@gilt.com",
        "notification_url": "https://cavellc.gilt.com/incoming"
      }' \
      https://cavellc.io/organizations/gilt

Response

    HTTP/1.1 200 OK
    Content-Type: application/json; charset=utf-8
    Content-Length: 272

    {
      "name": "gilt",
      "email": "cavellc@gilt.com",
      "notification_url": "https://cavellc.gilt.com/incoming",
      "tokens":[
        {
          "id":"145c5d831d3qQWM3tG1lhrVvmAOj4C",
          "description":"June 2014",
          "value":"cfrrXmxWy5pwlnh9aZwbQa2wvNqpohHnc3eOEMc8ucA5flvSMqLNfaWE",
          "created":"2014-05-16T13:18:38.912Z"
        }
      ]
    }


Get Teams for Organization
==========================

Request

    curl -i -X GET \
      -H "Accept: application/json" \
      -H "Authorization: Bearer cfrrXmxWy5pwlnh9aZwbQa2wvNqpohHnc3eOEMc8ucA5flvSMqLNfaWE" \
      https://cavellc.io/organizations/gilt/teams

Response

    HTTP/1.1 200 OK
    Content-Type: application/json; charset=utf-8
    Content-Length: 213

    [
      {
        "name":"ouroboros",
        "tokens":[
          {
            "id":"145c5ede532HfKAk9PKpwrBTb936SN",
            "description":"Ouroboros June 2014",
            "value":"xZxKaJTVChsdCTC2dLkyVKGPGRwIFFhpLdOpvxXbvdKGws4r6MuXasGy",
            "created":"2014-05-16T13:42:21.087Z"
          }
        ]
      }
    ]


Delete Team
===========

Request

    curl -i -X DELETE \
      -H "Authorization: Bearer cfrrXmxWy5pwlnh9aZwbQa2wvNqpohHnc3eOEMc8ucA5flvSMqLNfaWE" \
      https://cavellc.io/organizations/gilt/teams/ouroboros

Response

    HTTP/1.1 204 No Content
    Content-Length: 0


Delete Organization
===================

Request

    curl -i -X DELETE \
      -H "Authorization: Bearer cfrrXmxWy5pwlnh9aZwbQa2wvNqpohHnc3eOEMc8ucA5flvSMqLNfaWE" \
      https://cavellc.io/organizations/gilt

Response

    HTTP/1.1 204 No Content
    Content-Length: 0

