package com.github.bekisz.multiverse.utils

import org.slf4j.{Logger, LoggerFactory}

trait Logging {

    // Make the log field transient so that objects with Logging can
    // be serialized and used on another machine
    @transient private var log_ : Logger = null
    //@transient  private val log: Logger = LoggerFactory.getLogger(getClass.getName).asInstanceOf[Logger]
    // Method to get the logger name for this object
    protected def logName = {
      // Ignore trailing $'s in the class names for Scala objects
      this.getClass.getName.stripSuffix("$")
    }

    // Method to get or create the logger for this object
    protected def log: Logger = {
      if (log_ == null) {
        log_ = LoggerFactory.getLogger(this.logName).asInstanceOf[Logger]
      }
      log_
    }

}
