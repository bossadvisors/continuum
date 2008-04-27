package org.apache.maven.continuum.notification.mail;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.continuum.Continuum;
import org.apache.maven.continuum.configuration.ConfigurationService;
import org.apache.maven.continuum.execution.ExecutorConfigurator;
import org.apache.maven.continuum.execution.ant.AntBuildExecutor;
import org.apache.maven.continuum.execution.maven.m1.MavenOneBuildExecutor;
import org.apache.maven.continuum.execution.maven.m2.MavenTwoBuildExecutor;
import org.apache.maven.continuum.installation.InstallationException;
import org.apache.maven.continuum.installation.InstallationService;
import org.apache.maven.continuum.model.project.BuildDefinition;
import org.apache.maven.continuum.model.project.BuildResult;
import org.apache.maven.continuum.model.project.Project;
import org.apache.maven.continuum.model.project.ProjectNotifier;
import org.apache.maven.continuum.model.system.Installation;
import org.apache.maven.continuum.model.system.Profile;
import org.apache.maven.continuum.notification.AbstractContinuumNotifier;
import org.apache.maven.continuum.notification.ContinuumNotificationDispatcher;
import org.apache.maven.continuum.notification.ContinuumRecipientSource;
import org.apache.maven.continuum.project.ContinuumProjectState;
import org.apache.maven.continuum.reports.surefire.ReportTestResult;
import org.apache.maven.continuum.reports.surefire.ReportTestSuiteGenerator;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.codehaus.plexus.mailsender.MailMessage;
import org.codehaus.plexus.mailsender.MailSender;
import org.codehaus.plexus.mailsender.MailSenderException;
import org.codehaus.plexus.notification.NotificationException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.velocity.VelocityComponent;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MailContinuumNotifier
    extends AbstractContinuumNotifier
    implements Initializable
{
    // ----------------------------------------------------------------------
    // Requirements
    // ----------------------------------------------------------------------

    /**
     * @plexus.requirement
     */
    private VelocityComponent velocity;

    /**
     * @plexus.requirement
     */
    private ConfigurationService configurationService;

    /**
     * @plexus.requirement
     */
    private Continuum continuum;

    /**
     * @plexus.configuration
     */
    private MailSender mailSender;
    
    /**
     * @plexus.requirement
     */    
    private ReportTestSuiteGenerator reportTestSuiteGenerator;

    /**
     * @plexus.requirement role-hint="default"
     */
    //private ShellCommandHelper shellCommandHelper;
    // ----------------------------------------------------------------------
    // Configuration
    // ----------------------------------------------------------------------
    /**
     * @plexus.configuration
     */
    private String fromMailbox;

    /**
     * @plexus.configuration
     */
    private String fromName;

    /**
     * @plexus.configuration
     */
    private String timestampFormat;

    /**
     * @plexus.configuration
     */
    private boolean includeBuildResult = true;

    /**
     * @plexus.configuration
     */
    private boolean includeBuildSummary = true;
    
    /**
     * @plexus.configuration
     */
    private boolean includeTestSummary = true;
    
    /**
     * @plexus.configuration
     */
    private boolean includeOutput = false;    

    /**
     * Customizable mail subject.  Use any combination of literal text, project or build attributes.
     * Examples:
     * "[continuum] BUILD ${state}: ${project.groupId} ${project.name}" results in "[continuum] BUILD SUCCESSFUL: foo.bar Hello World"
     * "[continuum] BUILD ${state}: ${project.name} ${project.scmTag}" results in "[continuum] BUILD SUCCESSFUL: Hello World Branch001"
     * "[continuum] BUILD ${state}: ${project.name} ${build.durationTime}" results in "[continuum] BUILD SUCCESSFUL: Hello World 2 sec"
     * "[continuum] BUILD ${state}: ${project.name}, Build Def - ${build.buildDefinition.description}" results in "[continuum] BUILD SUCCESSFUL: Hello World, Build Def - Nightly Test Build"
     *
     * @plexus.configuration
     */
    private String subjectFormat = "[continuum] BUILD ${state}: ${project.groupId} ${project.name}";

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private String buildHost;

    private FormatterTool formatterTool;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private static final String FALLBACK_FROM_MAILBOX = "continuum@localhost";

    // ----------------------------------------------------------------------
    // Component Lifecycle
    // ----------------------------------------------------------------------

    public void initialize()
    {
        try
        {
            InetAddress address = InetAddress.getLocalHost();

            buildHost = StringUtils.clean( address.getHostName() );

            if ( buildHost == null )
            {
                buildHost = "localhost";
            }
        }
        catch ( UnknownHostException ex )
        {
            fromName = "Continuum";
        }

        // ----------------------------------------------------------------------
        // From mailbox
        // ----------------------------------------------------------------------

        if ( StringUtils.isEmpty( fromMailbox ) )
        {
            getLogger().info( "The from mailbox is not configured, will use the nag email address from the project." );

            fromMailbox = null;
        }
        else
        {
            getLogger().info( "Using '" + fromMailbox + "' as the from mailbox for all emails." );
        }

        if ( StringUtils.isEmpty( fromName ) )
        {
            fromName = "Continuum@" + buildHost;
        }

        getLogger().info( "From name: " + fromName );

        getLogger().info( "Build host name: " + buildHost );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        formatterTool = new FormatterTool( timestampFormat );
    }

    // ----------------------------------------------------------------------
    // Notifier Implementation
    // ----------------------------------------------------------------------

    public void sendNotification( String source, Set recipients, Map configuration, Map context )
        throws NotificationException
    {
        Project project = (Project) context.get( ContinuumNotificationDispatcher.CONTEXT_PROJECT );

        ProjectNotifier projectNotifier = (ProjectNotifier) context
            .get( ContinuumNotificationDispatcher.CONTEXT_PROJECT_NOTIFIER );

        BuildResult build = (BuildResult) context.get( ContinuumNotificationDispatcher.CONTEXT_BUILD );

        String buildOutput = (String) context.get( ContinuumNotificationDispatcher.CONTEXT_BUILD_OUTPUT );

        BuildDefinition buildDefinition = (BuildDefinition) context
            .get( ContinuumNotificationDispatcher.CONTEXT_BUILD_DEFINITION );

        // ----------------------------------------------------------------------
        // If there wasn't any building done, don't notify
        // ----------------------------------------------------------------------

        if ( build == null )
        {
            return;
        }

        // ----------------------------------------------------------------------
        // Generate and send email
        // ----------------------------------------------------------------------

        if ( source.equals( ContinuumNotificationDispatcher.MESSAGE_ID_BUILD_COMPLETE ) )
        {
            buildComplete( project, projectNotifier, build, buildOutput, source, recipients, configuration,
                           buildDefinition );
        }
    }

    private void buildComplete( Project project, ProjectNotifier projectNotifier, BuildResult build, String buildOutput,
                                String source, Set recipients, Map configuration, BuildDefinition buildDefinition )
        throws NotificationException
    {

        // ----------------------------------------------------------------------
        // Check if the mail should be sent at all
        // ----------------------------------------------------------------------

        BuildResult previousBuild = getPreviousBuild( project, buildDefinition, build );

        if ( !shouldNotify( build, previousBuild, projectNotifier ) )
        {
            return;
        }

        // ----------------------------------------------------------------------
        // Generate the mail contents
        // ----------------------------------------------------------------------

        String packageName = getClass().getPackage().getName().replace( '.', '/' );

        String templateName = packageName + "/templates/" + project.getExecutorId() + "/" + source + ".vm";

        StringWriter writer = new StringWriter();

        String content;

        try
        {
            VelocityContext context = new VelocityContext();
            
            context.put( "includeTestSummary", includeTestSummary );
            
            context.put( "includeOutput", includeOutput );

            if ( includeBuildResult )
            {
                context.put( "buildOutput", buildOutput );
            }

            if ( includeBuildSummary )
            {
                context.put( "build", build );

                ReportTestResult reportTestResult = reportTestSuiteGenerator.generateReportTestResult( build.getId(),
                                                                                                       project.getId() );

                context.put( "testResult", reportTestResult );
                
                context.put( "project", project );

                context.put( "changesSinceLastSuccess", continuum.getChangesSinceLastSuccess( project.getId(), build
                    .getId() ) );

                context.put( "previousBuild", previousBuild );

                // ----------------------------------------------------------------------
                // Tools
                // ----------------------------------------------------------------------

                context.put( "formatter", formatterTool );

                // TODO: Make the build host a part of the build

                context.put( "buildHost", buildHost );

                String osName = System.getProperty( "os.name" );

                String osPatchLevel = System.getProperty( "sun.os.patch.level" );

                if ( osPatchLevel != null )
                {
                    osName = osName + "(" + osPatchLevel + ")";
                }

                context.put( "osName", osName );

                context.put( "javaVersion",
                             System.getProperty( "java.version" ) + "(" + System.getProperty( "java.vendor" ) + ")" );

                // TODO only in case of a java project ?
                context.put( "javaHomeInformations", getJavaHomeInformations( buildDefinition ) );

                context.put( "builderVersions", getBuilderVersion( buildDefinition, project ) );
            }

            // ----------------------------------------------------------------------
            // Data objects
            // ----------------------------------------------------------------------

            context.put( "reportUrl", getReportUrl( project, build, configurationService ) );

            // TODO put other profile env var could be a security if they provide passwords ?

            // ----------------------------------------------------------------------
            // Generate
            // ----------------------------------------------------------------------

            velocity.getEngine().mergeTemplate( templateName, context, writer );

            content = writer.getBuffer().toString();
        }
        catch ( ResourceNotFoundException e )
        {
            getLogger().info( "No such template: '" + templateName + "'." );

            return;
        }
        catch ( Exception e )
        {
            throw new NotificationException( "Error while generating mail contents.", e );
        }

        // ----------------------------------------------------------------------
        // Send the mail
        // ----------------------------------------------------------------------

        String subject;
        try
        {
            subject = generateSubject( project, build );
        }
        catch ( Exception e )
        {
            throw new NotificationException( "Error while generating mail subject.", e );
        }

        sendMessage( project, recipients, subject, content, configuration );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private List<String> getJavaHomeInformations( BuildDefinition buildDefinition )
        throws InstallationException
    {
        if ( buildDefinition == null )
        {
            return continuum.getInstallationService().getDefaultJdkInformations();
        }
        Profile profile = buildDefinition.getProfile();
        if ( profile == null )
        {
            return continuum.getInstallationService().getDefaultJdkInformations();
        }
        return continuum.getInstallationService().getJdkInformations( profile.getJdk() );
    }

    private List<String> getBuilderVersion( BuildDefinition buildDefinition, Project project )
        throws InstallationException
    {

        ExecutorConfigurator executorConfigurator = null;
        Installation builder = null;
        Profile profile = null;
        if ( buildDefinition != null )
        {
            profile = buildDefinition.getProfile();
            if ( profile != null )
            {
                builder = profile.getBuilder();
            }
        }
        if ( builder != null )
        {
            executorConfigurator = continuum.getInstallationService().getExecutorConfigurator( builder.getType() );
        }
        else
        {
            // depends on ExecutorId
            if ( MavenTwoBuildExecutor.ID.equals( project.getExecutorId() ) )
            {
                executorConfigurator = continuum.getInstallationService()
                    .getExecutorConfigurator( InstallationService.MAVEN2_TYPE );
            }
            else if ( MavenOneBuildExecutor.ID.equals( project.getExecutorId() ) )
            {
                executorConfigurator = continuum.getInstallationService()
                    .getExecutorConfigurator( InstallationService.MAVEN1_TYPE );
            }
            else if ( AntBuildExecutor.ID.equals( project.getExecutorId() ) )
            {
                executorConfigurator = continuum.getInstallationService()
                    .getExecutorConfigurator( InstallationService.ANT_TYPE );
            }
            else
            {
                return Arrays.asList( new String[]{"No builder defined"} );
            }
        }

        return continuum.getInstallationService().getExecutorConfiguratorVersion( builder == null ? null : builder
            .getVarValue(), executorConfigurator, profile );
    }

    private String generateSubject( Project project, BuildResult build )
        throws Exception
    {
        String state = getState( project, build );

        VelocityContext context = new VelocityContext();
        context.put( "project", project );
        context.put( "build", build );
        context.put( "state", state );

        StringWriter writer = new StringWriter();

        boolean velocityResults = velocity.getEngine().evaluate( context, writer, "subjectPattern", subjectFormat );

        String subject = writer.toString();

        return subject;
    }

    private String getState( Project project, BuildResult build )
    {
        int state = project.getState();

        if ( build != null )
        {
            state = build.getState();
        }

        if ( state == ContinuumProjectState.OK )
        {
            return "SUCCESSFUL";
        }
        else if ( state == ContinuumProjectState.FAILED )
        {
            return "FAILURE";
        }
        else if ( state == ContinuumProjectState.ERROR )
        {
            return "ERROR";
        }
        else
        {
            getLogger().warn( "Unknown build state " + state + " for project " + project.getId() );

            return "ERROR: Unknown build state " + state;
        }
    }

    private void sendMessage( Project project, Set recipients, String subject, String content, Map configuration )
        throws NotificationException
    {
        if ( recipients.size() == 0 )
        {
            // This is a useful message for the users when debugging why they don't
            // receive any mails

            getLogger().info( "No mail recipients for '" + project.getName() + "'." );

            return;
        }

        String fromMailbox = getFromMailbox( configuration );

        if ( fromMailbox == null )
        {
            getLogger()
                .warn( project.getName() +
                    ": Project is missing nag email and global from mailbox is missing, not sending mail." );

            return;
        }

        MailMessage message = new MailMessage();

        message.addHeader( "X-Continuum-Build-Host", buildHost );

        message.addHeader( "X-Continuum-Project-Id", Integer.toString( project.getId() ) );

        message.addHeader( "X-Continuum-Project-Name", project.getName() );

        try
        {
            message.setSubject( subject );

            getLogger().info( "Message Subject: '" + subject + "'." );

            message.setContent( content );

            MailMessage.Address from = new MailMessage.Address( fromMailbox, fromName );

            message.setFrom( from );

            getLogger().info( "Sending message: From '" + from + "'." );

            for ( Iterator it = recipients.iterator(); it.hasNext(); )
            {
                String mailbox = (String) it.next();

                // TODO: set a proper name
                MailMessage.Address to = new MailMessage.Address( mailbox );

                getLogger().info( "Recipient: To '" + to + "'." );

                message.addTo( to );
            }

            mailSender.send( message );
        }
        catch ( MailSenderException ex )
        {
            throw new NotificationException( "Exception while sending message.", ex );
        }
    }

    private String getFromMailbox( Map configuration )
    {
        if ( fromMailbox != null )
        {
            return fromMailbox;
        }

        String address = null;

        if ( configuration != null )
        {
            address = (String) configuration.get( ContinuumRecipientSource.ADDRESS_FIELD );
        }

        if ( StringUtils.isEmpty( address ) )
        {
            return FALLBACK_FROM_MAILBOX;
        }
        // olamy : CONTINUUM-860 if address contains commas we use only the first one
        if ( address.contains( "," ) )
        {
            String[] addresses = StringUtils.split( address, "," );
            return addresses[0];
        }
        return address;
    }

    /**
     * @see org.codehaus.plexus.notification.notifier.Notifier#sendNotification(java.lang.String,java.util.Set,java.util.Properties)
     */
    public void sendNotification( String arg0, Set arg1, Properties arg2 )
        throws NotificationException
    {
        throw new NotificationException( "Not implemented." );
    }
}