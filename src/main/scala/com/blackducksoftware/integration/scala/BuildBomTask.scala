package com.blackducksoftware.integration.scala

import sbt.Def.Initialize
import sbt._
import Keys._

import com.blackducksoftware.integration.hub.exception.HubIntegrationException
import com.blackducksoftware.integration.hub.buildtool.BuildToolConstants
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder
import com.blackducksoftware.integration.hub.global.HubServerConfig
import com.blackducksoftware.integration.hub.buildtool.BuildToolHelper
import com.blackducksoftware.integration.hub.buildtool.bdio.BdioDependencyWriter
import com.blackducksoftware.integration.hub.buildtool.FlatDependencyListWriter
import com.blackducksoftware.integration.hub.rest.RestConnection
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection
import com.blackducksoftware.integration.hub.service.HubServicesFactory
import com.blackducksoftware.integration.hub.api.policy.PolicyStatusEnum
import com.blackducksoftware.integration.hub.api.policy.PolicyStatusItem
import com.blackducksoftware.integration.hub.dataservice.policystatus.PolicyStatusDescription

import java.io.IOException

object BuildBom extends AutoPlugin {
   override def requires = empty
   override def trigger = allRequirements
  
    object autoImport {
        val hubIgnoreFailure = SettingKey[Boolean]("hubIgnoreFailure", "Should buildBom ignore failures")
        val hubProjectName = SettingKey[String]("hubProjectName", "The Hub project name")
        val hubVersionName = SettingKey[String]("hubVersionName", "The Hub project version")
        val hubUrl = SettingKey[String]("hubUrl", "The Hub URL")
        val hubUsername = SettingKey[String]("hubUsername", "The Username to connect to the Hub")
        val hubPassword = SettingKey[String]("hubPassword", "The Password to connect to the Hub")
        val hubTimeout = SettingKey[Integer]("hubTimeout", "The amount of time to attempt Hub communications")
        
        val hubProxyHost = SettingKey[String]("hubProxyHost", "The proxy host to connect to the Hub through")
        val hubProxyPort = SettingKey[Integer]("hubProxyPort", "The proxy port to connect to the Hub through")
        val hubNoProxyHosts = SettingKey[String]("hubNoProxyHosts", "The host names that should ignore the proxy")
        val hubProxyUsername = SettingKey[String]("hubProxyUsername", "The proxy username")
        val hubProxyPassword = SettingKey[String]("hubProxyPassword", "The proxy password")
        
        val createFlatDependencyList = SettingKey[Boolean]("createFlatDependencyList", "Should create a file with a flat list of the dependencies")
        val createHubBdio = SettingKey[Boolean]("createHubBdio", "Should create a BDIO file of the dependencies")
        val deployHubBdio = SettingKey[Boolean]("deployHubBdio", "Should deploy the BDIO file to the Hub")
        val createHubReport = SettingKey[Boolean]("createHubReport", "Should wait for the BOM calculations and create a Risk Report")
        val checkPolicies = SettingKey[Boolean]("checkPolicies", "Should check the policy status of the Hub project/version")
        val outputDirectory = SettingKey[File]("outputDirectory", "The directory to create files in")
        
        val hubScanTimeout = SettingKey[Integer]("hubScanTimeout", "The time to wait for the BOM calculations")
        val includedConfigurations = SettingKey[String]("includedConfigurations", "")
        
        val buildBomTask = TaskKey[Unit](BuildToolConstants.BUILD_TOOL_STEP_CAMEL,"Prints test string")
        
       def getHubServerConfigBuilder(hubUrl: String, hubUsername: String, hubPassword: String, hubTimeout: Integer,
           hubProxyHost: String, hubProxyPort: Integer, noProxyHosts: String, hubProxyUser: String, hubProxyPassword: String) : HubServerConfigBuilder = {
          val hubServerConfigBuilder = new HubServerConfigBuilder()
          hubServerConfigBuilder.setHubUrl(hubUrl)
          hubServerConfigBuilder.setUsername(hubUsername)
          hubServerConfigBuilder.setPassword(hubPassword)
          hubServerConfigBuilder.setTimeout(hubTimeout)
          hubServerConfigBuilder.setProxyHost(hubProxyHost)
          hubServerConfigBuilder.setProxyPort(hubProxyPort)
          hubServerConfigBuilder.setIgnoredProxyHosts(noProxyHosts)
          hubServerConfigBuilder.setProxyUsername(hubProxyUser)
          hubServerConfigBuilder.setProxyPassword(hubProxyPassword)
  
          hubServerConfigBuilder
        }

        def getHubServicesFactory(hubServerConfigBuilder : HubServerConfigBuilder) : HubServicesFactory = {
            var restConnection : CredentialsRestConnection = null
            try {
                var hubServerConfig = hubServerConfigBuilder.build()
                restConnection = new CredentialsRestConnection(hubServerConfig)
            } catch {
              case e: Exception =>
                  throw new HubIntegrationException(String.format(BuildToolConstants.BUILD_TOOL_CONFIGURATION_ERROR, e.getMessage()), e)
            } 
            new HubServicesFactory(restConnection)
        }

        def getBdioFilename(projectName : String) : String = {
            BdioDependencyWriter.getFilename(projectName)
        }

        def getFlatFilename(projectName : String) : String = {
            FlatDependencyListWriter.getFilename(projectName)
        }
        
        def createFlatDependencyList(logger: ScalaLogger, report: UpdateReport, buildToolHelper : BuildToolHelper,
            includedConfigurations : String, organization : String, projectName : String, projectVersion : String,
            outputDirectory : File): Unit = {
          logger.info(String.format(BuildToolConstants.CREATE_FLAT_DEPENDENCY_LIST_STARTING, getFlatFilename(projectName)))
           try {
              var dependencyGatherer = new DependencyGatherer(logger, includedConfigurations)
              var rootNode = dependencyGatherer.getFullyPopulatedRootNode(report, organization, projectName, projectVersion)
              
              buildToolHelper.createFlatOutput(rootNode, projectName,
                        projectVersion, outputDirectory)
          } catch {
            case ioe: IOException =>
              throw new HubIntegrationException(String.format(BuildToolConstants.CREATE_FLAT_DEPENDENCY_LIST_ERROR, ioe.getMessage()), ioe)
          }
          logger.info(String.format(BuildToolConstants.CREATE_FLAT_DEPENDENCY_LIST_FINISHED, getFlatFilename(projectName)))
        }

        def createBDIO(logger: ScalaLogger, report: UpdateReport, buildToolHelper : BuildToolHelper, name : String, 
            includedConfigurations : String, organization : String, projectName : String, projectVersion : String,
            outputDirectory : File): Unit = {
          logger.info(String.format(BuildToolConstants.CREATE_HUB_OUTPUT_STARTING, getBdioFilename(projectName)))
           try {
              var dependencyGatherer = new DependencyGatherer(logger, includedConfigurations)
              var rootNode = dependencyGatherer.getFullyPopulatedRootNode(report, organization, projectName, projectVersion)
              
              buildToolHelper.createHubOutput(rootNode, name, projectName,
                        projectVersion, outputDirectory)
          } catch {
            case ioe: IOException =>
              throw new HubIntegrationException(String.format(BuildToolConstants.CREATE_HUB_OUTPUT_ERROR, ioe.getMessage()), ioe)
          }
          logger.info(String.format(BuildToolConstants.CREATE_HUB_OUTPUT_FINISHED, getBdioFilename(projectName)))
        }

        def deployHubBDIO(logger: ScalaLogger, buildToolHelper : BuildToolHelper, hubServicesFactory : HubServicesFactory, projectName : String, 
          outputDirectory : File): Unit = {
          logger.info(String.format(BuildToolConstants.DEPLOY_HUB_OUTPUT_STARTING, getBdioFilename(projectName)))
           try {
              buildToolHelper.deployHubOutput(hubServicesFactory, outputDirectory, projectName)
          } catch {
            case e: Exception =>
              throw new HubIntegrationException(String.format(BuildToolConstants.DEPLOY_HUB_OUTPUT_ERROR, e.getMessage()), e)
          }
          logger.info(String.format(BuildToolConstants.DEPLOY_HUB_OUTPUT_FINISHED, getBdioFilename(projectName)))
        }

        def waitForHub(logger: ScalaLogger, buildToolHelper : BuildToolHelper, hubServicesFactory : HubServicesFactory, hubProjectName :String,
          hubVersionName : String, hubScanTimeout : Long) : Unit = {
            logger.info("Waiting for Hub to finish the Bom calculations")
            try {
                buildToolHelper.waitForHub(hubServicesFactory, hubProjectName, hubVersionName, hubScanTimeout)
            } catch {
              case hie: IllegalArgumentException =>
                throw new HubIntegrationException(String.format(BuildToolConstants.SCAN_ERROR_MESSAGE, hie.getMessage()), hie)
            }
            logger.info("The Hub has finished the Bom calculations")
        }

        def createHubReport(logger: ScalaLogger, buildToolHelper : BuildToolHelper, hubServicesFactory : HubServicesFactory,
          outputDirectory : File, hubProjectName : String, hubVersionName : String, hubScanTimeout : Long): Unit = {
          logger.info(String.format(BuildToolConstants.CREATE_REPORT_STARTING, getBdioFilename(hubProjectName)))
           try {
              buildToolHelper.createRiskReport(hubServicesFactory, outputDirectory, hubProjectName, hubVersionName, hubScanTimeout)
          } catch {
            case e: Exception =>
              throw new HubIntegrationException(String.format(BuildToolConstants.FAILED_TO_CREATE_REPORT, e.getMessage()), e)
          }
          logger.info(String.format(BuildToolConstants.CREATE_REPORT_FINISHED, getBdioFilename(hubProjectName)))
        }

        def checkHubPolicies(logger: ScalaLogger, buildToolHelper : BuildToolHelper, hubServicesFactory : HubServicesFactory,
          hubProjectName : String, hubVersionName : String): Unit = {
          logger.info(String.format(BuildToolConstants.CHECK_POLICIES_STARTING, getBdioFilename(hubProjectName)))
           try {
              var policyStatusItem = buildToolHelper.checkPolicies(hubServicesFactory, hubProjectName, hubVersionName)
              handlePolicyStatusItem(logger, policyStatusItem);
          } catch {
            case e: Exception =>
              throw new HubIntegrationException(String.format(BuildToolConstants.CHECK_POLICIES_ERROR, e.getMessage()), e)
          }
          logger.info(String.format(BuildToolConstants.CHECK_POLICIES_FINISHED, getBdioFilename(hubProjectName)))
        }

        def handlePolicyStatusItem(logger : ScalaLogger, policyStatusItem : PolicyStatusItem): Unit = {
            var policyStatusDescription = new PolicyStatusDescription(policyStatusItem);
            var policyStatusMessage = policyStatusDescription.getPolicyStatusMessage();
            logger.info(policyStatusMessage);
            if (PolicyStatusEnum.IN_VIOLATION == policyStatusItem.getOverallStatus()) {
                throw new HubIntegrationException(policyStatusMessage);
            }
        }
    }
  
  import autoImport._
  
  override def projectSettings: Seq[Def.Setting[_]] = Seq(
        hubIgnoreFailure := { false },
        hubProjectName := { name.value },
        hubVersionName := { version.value },
        hubTimeout := { 120 },
        hubProxyHost := { "" },
        hubProxyPort := { 0 },
        hubNoProxyHosts := { "" },
        hubProxyUsername := { "" },
        hubProxyPassword := { "" },
        createFlatDependencyList := { false },
        createHubBdio := { true },
        deployHubBdio := { true },
        createHubReport := { false },
        checkPolicies := { false },
        hubScanTimeout := { 300 },
        includedConfigurations := { "compile" },
        outputDirectory := { new File("./bdio") },
        buildBomTask :=  { 
            var scalaLogger = new ScalaLogger(streams.value.log)
            var buildToolHelper = new BuildToolHelper(scalaLogger)
            try {
                if (createFlatDependencyList.value){
                    createFlatDependencyList(scalaLogger, update.value, buildToolHelper, includedConfigurations.value, 
                        organization.value, hubProjectName.value, hubVersionName.value, outputDirectory.value)
                }
                if (createHubBdio.value){
                    createBDIO(scalaLogger, update.value, buildToolHelper, name.value, includedConfigurations.value, 
                        organization.value, hubProjectName.value, hubVersionName.value, outputDirectory.value)
                }
                if (deployHubBdio.value || createHubReport.value || checkPolicies.value){
                    var configBuilder = getHubServerConfigBuilder(hubUrl.value, hubUsername.value,hubPassword.value,hubTimeout.value,
                          hubProxyHost.value,hubProxyPort.value,hubNoProxyHosts.value,hubProxyUsername.value,hubProxyPassword.value)
                    var hubServicesFactory = getHubServicesFactory(configBuilder)
                    if (deployHubBdio.value){
                        deployHubBDIO(scalaLogger,buildToolHelper, hubServicesFactory, name.value, outputDirectory.value)
                    }
                    if(createHubReport.value || checkPolicies.value){
                        waitForHub(scalaLogger, buildToolHelper, hubServicesFactory, hubProjectName.value, hubVersionName.value, hubScanTimeout.value.toLong)
                        if(createHubReport.value){
                            createHubReport(scalaLogger, buildToolHelper, hubServicesFactory, outputDirectory.value, hubProjectName.value, hubVersionName.value,
                             hubScanTimeout.value.toLong)
                        }
                        if(checkPolicies.value){
                            checkHubPolicies(scalaLogger, buildToolHelper, hubServicesFactory, hubProjectName.value, hubVersionName.value)
                        }
                    }
                }
            } catch {
                case e: Exception =>
                  if (hubIgnoreFailure.value) {
                      scalaLogger.error(e.getMessage(), e)
                  } else {
                      throw e
                  }
            }
        }
    )
  
}