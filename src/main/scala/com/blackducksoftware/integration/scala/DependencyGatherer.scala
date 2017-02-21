package com.blackducksoftware.integration.scala

import sbt._
import java.util.ArrayList
import com.blackducksoftware.integration.hub.buildtool.DependencyNode
import com.blackducksoftware.integration.hub.buildtool.Gav
import com.blackducksoftware.integration.hub.buildtool.DependencyNodeBuilder
import org.apache.commons.lang3.StringUtils


class DependencyGatherer(logger: ScalaLogger, includedConfigurations : String) {
  
    def getFullyPopulatedRootNode(report: UpdateReport, organization : String, projectName : String, projectVersion : String): DependencyNode = {
        logger.info("creating the dependency graph")
        var projectGav = new Gav(organization, projectName, projectVersion)
        
        var builder = new DependencyNodeBuilder(projectGav)
        var children = new ArrayList[Gav]()

        var configurationHelper = new ConfigurationHelper(logger, includedConfigurations)
        var requestedConfigurations = configurationHelper.getRequestedConfigurations()
        logger.info(s"Requested Configurations : $requestedConfigurations")
        report.configurations.foreach( configurationReport => { 
            var config = configurationReport.configuration
            if (configurationHelper.shouldIncludeConfiguration(config, requestedConfigurations)){
              logger.info(s"Gathering dependencies from Configuration : $config")
              configurationReport.modules.foreach( module => {
                  resolveDependency(builder,module)
                  children.add(new Gav(module.module.organization, module.module.name, module.module.revision))
                }
              )
            } else {
              logger.debug(s"Skipping Configuration : $config")
            }
          }
        )
        builder.addNodeWithChildren(projectGav, children)
        
        builder.buildRootNode()
    }
    
    def resolveDependency(builder: DependencyNodeBuilder,module : ModuleReport) : Unit = {
      var moduleGav = new Gav(module.module.organization, module.module.name, module.module.revision)
      if(module.callers != null && !module.callers.isEmpty){
          module.callers.foreach(caller => {
                var callerGav = new Gav(caller.caller.organization, caller.caller.name, caller.caller.revision)
                builder.addNodeWithChild(callerGav, moduleGav)
            }
          )
      } else {
          builder.addNode(moduleGav)
      }
    }
    
}