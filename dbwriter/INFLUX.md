Create Database
===============

    curl -X POST \
      -d '{
            "name": "site_development"
          }' \
      "http://localhost:8086/db?u=root&p=root"


Add Database User
=================

    curl -X POST \
      -d '{
            "name": "paul",
            "password": "i write teh docz"
          }' \
      "http://localhost:8086/db/site_dev/users?u=root&p=root"


Delete Database User
====================

    curl -X DELETE \
      "http://localhost:8086/db/site_dev/users/paul?u=root&p=root"


Drop Database
=============

    curl -X DELETE \
      "http://localhost:8086/db/site_development?u=root&p=root"
