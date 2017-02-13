package com.blackducksoftware.integration.scala

import com.blackducksoftware.integration.log._
import com.blackducksoftware.integration.util.CIEnvironmentVariables

class ScalaLogger(logger: sbt.Logger) extends IntLogger {
  
    def alwaysLog(txt : String) : Unit = { logger.success(txt) }

    def info(txt : String) : Unit = { logger.info(txt) }

    def error(t : Throwable) : Unit = { 
      logger.error(t.getMessage()) 
      logger.trace(t) 
    }

    def error(txt : String, t : Throwable) : Unit = { 
      logger.error(txt) 
      logger.trace(t) 
    }

    def error(txt : String) : Unit = { logger.error(txt) }

    def warn(txt : String) : Unit = { logger.warn(txt) }

    def trace(txt : String) : Unit = { logger.verbose(txt) }

    def trace(txt : String, t : Throwable) : Unit = {
      logger.verbose(txt) 
      logger.trace(t) 
    }

    def debug(txt : String) : Unit = { logger.debug(txt) }

    def debug(txt : String, t : Throwable) : Unit = {
      logger.debug(txt) 
      logger.trace(t) 
    }

    def setLogLevel(logLevel : LogLevel) : Unit = {

    }
    
    override def setLogLevel(variables : CIEnvironmentVariables) : Unit = {

    }

    def getLogLevel() : LogLevel = {
      LogLevel.INFO
    }
  
}