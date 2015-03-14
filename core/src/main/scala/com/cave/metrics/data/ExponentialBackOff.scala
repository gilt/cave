package com.cave.metrics.data

import org.apache.commons.logging.LogFactory


trait ExponentialBackOff {

  /**
   * To be injected
   *
   * @return maximum back off time in ms
   */
  protected def MaxBackOffTimeInMillis: Long

  /**
   * Override as needed
   * @return if we should log errors or not
   */
  protected def ShouldLogErrors: Boolean

  private[this] var currentSleepTimeInMillis = 1L
  private[this] val log = LogFactory.getLog(this.getClass)

  /**
   * Loops while condition evaluates to true
   * It reports errors, and backs off before next iteration
   *
   * @param condition the condition to be evaluated
   * @param body  the body of the loop
   * @return
   */
  protected[this] final def loopWithBackOffOnErrorWhile(condition: => Boolean)(body: => Unit) {
    while (condition) {
      try {
        body
        backOffReset()
      } catch {
        case e: Throwable =>
          if (ShouldLogErrors) {
            log.error(e.getMessage)
          }
          backOffOnError()
      }
    }
  }

  protected[this] final def retry[T](operation: => T): T = {
    retryUpTo(Long.MaxValue)(operation)
  }

  protected[this] final def retryUpTo[T](maxRetry: Long)(operation: => T): T = {
    var numRetries = 0L
    var result = Option.empty[T]

    loopWithBackOffOnErrorWhile(!result.isDefined && numRetries < maxRetry) {
      numRetries += 1
      result = Some(operation)
    }

    result getOrElse {
      sys.error(s"Max number of retries reached [$maxRetry], operation aborted.")
    }
  }

  protected[this] final def backOffReset() {
    currentSleepTimeInMillis = 1L
  }

  protected[this] final def backOffOnError() {
    try {
      Thread.sleep(currentSleepTimeInMillis)
    } catch {
      case _: InterruptedException =>
      // ignore interrupted exception
    }

    currentSleepTimeInMillis *= 2
    if (currentSleepTimeInMillis > MaxBackOffTimeInMillis) {
      currentSleepTimeInMillis = MaxBackOffTimeInMillis
    }
  }
}