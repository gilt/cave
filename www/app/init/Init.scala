package init

import org.apache.commons.logging.LogFactory

object Init {

  private val log = LogFactory.getLog("Init")

  def init() {
    log.debug("Init.init()")
  }

  def shutdown() {
    log.debug("Init.shutdown()")
  }
}
