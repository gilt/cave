package com.cave.metrics.data

import org.joda.time.DateTime

case class ConfirmationToken(id: Option[Long] = None,
                             uuid: String,
                             email: String,
                             creationTime: DateTime,
                             expirationTime: DateTime,
                             isSignUp: Boolean)
