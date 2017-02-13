package com.blackducksoftware.integration.scala

import sbt._
import org.apache.commons.lang3.StringUtils

class ConfigurationHelper (logger : ScalaLogger, includedConfigurations : String) {
    
    def getRequestedConfigurations() : Set[String] = {
        logger.info(s"User includedConfigurations : $includedConfigurations")
        var requestedConfigurations : Set[String] = Set()
        if(includedConfigurations.contains(",")){
          includedConfigurations.split(",").foreach(configuration => requestedConfigurations += configuration.toUpperCase())
        } else {
          requestedConfigurations += includedConfigurations.toUpperCase()
        }
        requestedConfigurations
    }
    
    def shouldIncludeConfiguration(configuration : String, requestedConfigurations : Set[String]) : Boolean = {
        // include all scopes if none were requested
        if (requestedConfigurations == null || requestedConfigurations.isEmpty) {
            return true;
        }
        if (StringUtils.isBlank(configuration)) {
            return false;
        }
        requestedConfigurations.contains(configuration.toUpperCase())
    }
    
}