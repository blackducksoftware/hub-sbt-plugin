package com.blackducksoftware.integration.scala

import sbt._
import java.util.ArrayList
import com.blackducksoftware.integration.hub.buildtool.DependencyNode;
import com.blackducksoftware.integration.hub.buildtool.Gav;
import org.apache.commons.lang3.StringUtils


class DependencyGatherer(logger: ScalaLogger, includedConfigurations : String) {
  
    def getFullyPopulatedRootNode(report: UpdateReport, organization : String, projectName : String, projectVersion : String): DependencyNode = {
        logger.info("creating the dependency graph")
        var projectGav = new Gav(organization, projectName, projectVersion)
        
        var children = new ArrayList[DependencyNode]()
        
        var configurationHelper = new ConfigurationHelper(logger, includedConfigurations)
        var requestedConfigurations = configurationHelper.getRequestedConfigurations()
        logger.info(s"Requested Configurations : $requestedConfigurations")
        report.configurations.foreach( configurationReport => { 
            var config = configurationReport.configuration
            if (configurationHelper.shouldIncludeConfiguration(config, requestedConfigurations)){
              logger.info(s"Gathering dependencies from Configuration : $config")
              configurationReport.modules.foreach( module =>
                children.add(getDependencyNode(module))
              )
            } else {
              logger.info(s"Skipping Configuration : $config")
            }
          }
        )
        
        new DependencyNode(projectGav, children);
    }
    
    def getDependencyNode(module : ModuleReport) : DependencyNode = {
      //logger.info(s"module : $module")
      //logger.info(s"callers : ${module.callers}")
      var moduleGav = new Gav(module.module.organization, module.module.name, module.module.revision)
      new DependencyNode(moduleGav, new ArrayList[DependencyNode]());
    }
    
}