package com.gilt.cavellc.models

import org.joda.time.DateTime

object Joda {
  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
}