package com.cave.metrics.data

import org.joda.time.DateTime

case class SessionToken(id: Option[Long] = None,
                        userId: Long,
                        token: String,
                        creationTime: DateTime,
                        expirationTime: DateTime)