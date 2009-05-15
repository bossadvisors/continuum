package org.apache.maven.continuum.web.action.admin;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.continuum.builder.distributed.executor.DistributedBuildTaskQueueExecutor;
import org.apache.continuum.builder.distributed.manager.DistributedBuildManager;
import org.apache.continuum.buildmanager.BuildManagerException;
import org.apache.continuum.model.project.ProjectScmRoot;
import org.apache.continuum.taskqueue.BuildProjectTask;
import org.apache.continuum.taskqueue.CheckOutTask;
import org.apache.continuum.taskqueue.PrepareBuildProjectsTask;
import org.apache.continuum.taskqueue.manager.TaskQueueManagerException;
import org.apache.maven.continuum.model.project.Project;
import org.apache.maven.continuum.model.project.ProjectGroup;
import org.apache.maven.continuum.project.ContinuumProjectState;
import org.apache.maven.continuum.security.ContinuumRoleConstants;
import org.apache.maven.continuum.web.action.ContinuumActionSupport;
import org.apache.maven.continuum.web.bean.BuildProjectQueue;
import org.apache.maven.continuum.web.bean.CheckoutQueue;
import org.apache.maven.continuum.web.exception.AuthenticationRequiredException;
import org.apache.maven.continuum.web.exception.AuthorizationRequiredException;
import org.apache.maven.continuum.web.model.DistributedBuildSummary;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.codehaus.redback.integration.interceptor.SecureAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:olamy@apache.org">olamy</a>
 * @version $Id$
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="queues"
 * @since 24 sept. 07
 */
public class QueuesAction
    extends ContinuumActionSupport
    implements SecureAction
{
    private static final Logger logger = LoggerFactory.getLogger( QueuesAction.class );

    private static final String DISTRIBUTED_BUILD_SUCCESS = "distributed-build-success";

    private List<String> selectedBuildTaskHashCodes;

    private List<String> selectedCheckOutTaskHashCodes;

    private int buildDefinitionId;

    private int projectId;

    private int trigger;

    private String projectName;

    private List<BuildProjectQueue> currentBuildProjectTasks = new ArrayList<BuildProjectQueue>();

    private List<CheckoutQueue> currentCheckoutTasks = new ArrayList<CheckoutQueue>();

    private List<BuildProjectQueue> buildsInQueue = new ArrayList<BuildProjectQueue>();

    private List<CheckoutQueue> checkoutsInQueue = new ArrayList<CheckoutQueue>();

    /**
     * @plexus.requirement
     */
    DistributedBuildManager distributedBuildManager;

    private List<DistributedBuildSummary> distributedBuildSummary;

    private List<PrepareBuildProjectsTask> distributedBuildQueues;

    private String buildAgentUrl;

    private int projectGroupId;

    private String scmRootAddress;

    private List<String> selectedDistributedBuildTaskHashCodes;

    // -----------------------------------------------------
    //  webwork
    // -----------------------------------------------------

    public String cancelCurrent()
        throws Exception
    {
        try
        {
            checkManageQueuesAuthorization();
        }
        catch ( AuthorizationRequiredException authzE )
        {
            addActionError( authzE.getMessage() );
            return REQUIRES_AUTHORIZATION;
        }
        catch ( AuthenticationRequiredException e )
        {
            addActionError( e.getMessage() );
            return REQUIRES_AUTHENTICATION;
        }

        try
        {
            getContinuum().getBuildsManager().cancelBuild( projectId );
        }
        catch ( BuildManagerException e )
        {
            addActionError( e.getMessage() );
            return ERROR;
        }

        return SUCCESS;
    }

    public String removeCheckout()
        throws Exception
    {
        try
        {
            checkManageQueuesAuthorization();
        }
        catch ( AuthorizationRequiredException authzE )
        {
            addActionError( authzE.getMessage() );
            return REQUIRES_AUTHORIZATION;
        }
        catch ( AuthenticationRequiredException e )
        {
            addActionError( e.getMessage() );
            return REQUIRES_AUTHENTICATION;
        }

        try
        {
            getContinuum().getBuildsManager().removeProjectFromCheckoutQueue( projectId );
        }
        catch ( BuildManagerException e )
        {
            addActionError( e.getMessage() );
            return ERROR;
        }

        return SUCCESS;
    }

    public String cancelCurrentCheckout()
        throws Exception
    {
        try
        {
            checkManageQueuesAuthorization();
        }
        catch ( AuthorizationRequiredException authzE )
        {
            addActionError( authzE.getMessage() );
            return REQUIRES_AUTHORIZATION;
        }
        catch ( AuthenticationRequiredException e )
        {
            addActionError( e.getMessage() );
            return REQUIRES_AUTHENTICATION;
        }
        try
        {
            cancelCheckout( projectId );
        }
        catch ( BuildManagerException e )
        {
            addActionError( e.getMessage() );
            return ERROR;
        }

        return SUCCESS;
    }

    public String display()
        throws Exception
    {
        if ( getContinuum().getConfiguration().isDistributedBuildEnabled() )
        {
            distributedBuildSummary = new ArrayList<DistributedBuildSummary>();

            Map<String, PrepareBuildProjectsTask> map = distributedBuildManager.getDistributedBuildProjects();

            for ( String url : map.keySet() )
            {
                PrepareBuildProjectsTask task = map.get( url );

                ProjectGroup projectGroup = getContinuum().getProjectGroup( task.getProjectGroupId() );

                DistributedBuildSummary summary = new DistributedBuildSummary();
                summary.setUrl( url );
                summary.setProjectGroupId( task.getProjectGroupId() );
                summary.setProjectGroupName( projectGroup.getName() );
                summary.setScmRootAddress( task.getScmRootAddress() );

                ProjectScmRoot scmRoot =
                    getContinuum().getProjectScmRootByProjectGroupAndScmRootAddress( task.getProjectGroupId(),
                                                                                     task.getScmRootAddress() );
                if ( scmRoot.getState() == ContinuumProjectState.UPDATING )
                {
                    summary.setCancelEnabled( false );
                }
                else
                {
                    summary.setCancelEnabled( true );
                }

                distributedBuildSummary.add( summary );
            }

            distributedBuildQueues = aggregateQueues();

            return DISTRIBUTED_BUILD_SUCCESS;
        }
        else
        {
            try
            {
                // current builds
                Map<String, BuildProjectTask> currentBuilds = getContinuum().getBuildsManager().getCurrentBuilds();
                Set<String> keySet = currentBuilds.keySet();
                for ( String key : keySet )
                {
                    BuildProjectTask buildTask = currentBuilds.get( key );
                    BuildProjectQueue queue = new BuildProjectQueue();
                    queue.setName( key );
                    queue.setTask( buildTask );
                    currentBuildProjectTasks.add( queue );
                }
            }
            catch ( BuildManagerException e )
            {
                addActionError( e.getMessage() );
                return ERROR;
            }

            try
            {
                // queued builds
                Map<String, List<BuildProjectTask>> builds =
                    getContinuum().getBuildsManager().getProjectsInBuildQueues();
                Set<String> keySet = builds.keySet();
                for ( String key : keySet )
                {
                    for ( BuildProjectTask task : builds.get( key ) )
                    {
                        BuildProjectQueue queue = new BuildProjectQueue();
                        queue.setName( key );
                        queue.setTask( task );
                        buildsInQueue.add( queue );
                    }
                }
            }
            catch ( BuildManagerException e )
            {
                addActionError( e.getMessage() );
                return ERROR;
            }

            try
            {
                // current checkouts
                Map<String, CheckOutTask> currentCheckouts = getContinuum().getBuildsManager().getCurrentCheckouts();
                Set<String> keySet = currentCheckouts.keySet();
                for ( String key : keySet )
                {
                    CheckOutTask checkoutTask = currentCheckouts.get( key );
                    CheckoutQueue queue = new CheckoutQueue();
                    queue.setName( key );
                    queue.setTask( checkoutTask );
                    currentCheckoutTasks.add( queue );
                }
            }
            catch ( BuildManagerException e )
            {
                addActionError( e.getMessage() );
                return ERROR;
            }

            try
            {
                // queued checkouts
                Map<String, List<CheckOutTask>> checkouts =
                    getContinuum().getBuildsManager().getProjectsInCheckoutQueues();
                Set<String> keySet = checkouts.keySet();
                for ( String key : keySet )
                {
                    for ( CheckOutTask task : checkouts.get( key ) )
                    {
                        CheckoutQueue queue = new CheckoutQueue();
                        queue.setName( key );
                        queue.setTask( task );
                        checkoutsInQueue.add( queue );
                    }
                }
            }
            catch ( BuildManagerException e )
            {
                addActionError( e.getMessage() );
                return ERROR;
            }
        }

        return SUCCESS;
    }

    public String remove()
        throws Exception
    {
        try
        {
            checkManageQueuesAuthorization();
        }
        catch ( AuthorizationRequiredException authzE )
        {
            addActionError( authzE.getMessage() );
            return REQUIRES_AUTHORIZATION;
        }
        catch ( AuthenticationRequiredException e )
        {
            addActionError( e.getMessage() );
            return REQUIRES_AUTHENTICATION;
        }

        getContinuum().getBuildsManager().removeProjectFromBuildQueue( projectId, buildDefinitionId, trigger,
                                                                       projectName );
        Project project = getContinuum().getProject( projectId );
        project.setState( project.getOldState() );
        getContinuum().updateProject( project );

        return SUCCESS;
    }

    public String removeBuildEntries()
        throws Exception
    {
        try
        {
            checkManageQueuesAuthorization();
        }
        catch ( AuthorizationRequiredException authzE )
        {
            addActionError( authzE.getMessage() );
            return REQUIRES_AUTHORIZATION;
        }
        catch ( AuthenticationRequiredException e )
        {
            addActionError( e.getMessage() );
            return REQUIRES_AUTHENTICATION;
        }

        getContinuum().getBuildsManager().removeProjectsFromBuildQueueWithHashcodes(
            listToIntArray( this.getSelectedBuildTaskHashCodes() ) );
        return SUCCESS;
    }

    public String removeCheckoutEntries()
        throws Exception
    {
        try
        {
            checkManageQueuesAuthorization();
        }
        catch ( AuthorizationRequiredException authzE )
        {
            addActionError( authzE.getMessage() );
            return REQUIRES_AUTHORIZATION;
        }
        catch ( AuthenticationRequiredException e )
        {
            addActionError( e.getMessage() );
            return REQUIRES_AUTHENTICATION;
        }

        getContinuum().getBuildsManager().removeProjectsFromCheckoutQueueWithHashcodes(
            listToIntArray( this.getSelectedCheckOutTaskHashCodes() ) );
        return SUCCESS;
    }

    public String cancelDistributedBuild()
        throws Exception
    {
        try
        {
            checkManageQueuesAuthorization();
        }
        catch ( AuthorizationRequiredException authzE )
        {
            addActionError( authzE.getMessage() );
            return REQUIRES_AUTHORIZATION;
        }
        catch ( AuthenticationRequiredException e )
        {
            addActionError( e.getMessage() );
            return REQUIRES_AUTHENTICATION;
        }

        distributedBuildManager.cancelDistributedBuild( buildAgentUrl, projectGroupId, scmRootAddress );

        return SUCCESS;
    }

    public String removeDistributedBuildEntry()
        throws Exception
    {
        try
        {
            checkManageQueuesAuthorization();
        }
        catch ( AuthorizationRequiredException authzE )
        {
            addActionError( authzE.getMessage() );
            return REQUIRES_AUTHORIZATION;
        }
        catch ( AuthenticationRequiredException e )
        {
            addActionError( e.getMessage() );
            return REQUIRES_AUTHENTICATION;
        }

        getContinuum().getTaskQueueManager().removeFromDistributedBuildQueue( projectGroupId, scmRootAddress );

        return SUCCESS;
    }

    public String removeDistributedBuildEntries()
        throws Exception
    {
        try
        {
            checkManageQueuesAuthorization();
        }
        catch ( AuthorizationRequiredException authzE )
        {
            addActionError( authzE.getMessage() );
            return REQUIRES_AUTHORIZATION;
        }
        catch ( AuthenticationRequiredException e )
        {
            addActionError( e.getMessage() );
            return REQUIRES_AUTHENTICATION;
        }

        getContinuum().getTaskQueueManager().removeTasksFromDistributedBuildQueueWithHashCodes(
            listToIntArray( this.getSelectedDistributedBuildTaskHashCodes() ) );

        return SUCCESS;
    }

    private int[] listToIntArray( List<String> strings )
    {
        if ( strings == null || strings.isEmpty() )
        {
            return new int[0];
        }
        int[] array = new int[0];
        for ( String intString : strings )
        {
            array = ArrayUtils.add( array, Integer.parseInt( intString ) );
        }
        return array;
    }

    // -----------------------------------------------------
    //  security
    // -----------------------------------------------------

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();
        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ContinuumRoleConstants.CONTINUUM_VIEW_QUEUES, Resource.GLOBAL );

        return bundle;
    }

    private boolean cancelCheckout( int projectId )
        throws BuildManagerException
    {
        Map<String, CheckOutTask> tasks = getContinuum().getBuildsManager().getCurrentCheckouts();
        if ( tasks != null )
        {
            Set<String> keySet = tasks.keySet();
            for ( String key : keySet )
            {
                CheckOutTask task = tasks.get( key );
                if ( task != null )
                {
                    if ( task.getProjectId() == projectId )
                    {
                        logger.info( "Cancelling checkout for project " + projectId );
                        return getContinuum().getBuildsManager().cancelCheckout( projectId );
                    }
                    else
                    {
                        logger.warn(
                            "Current task is not for the given projectId (" + projectId + "): " + task.getProjectId() +
                                "; not cancelling checkout" );
                    }
                }
            }
        }
        else
        {
            logger.warn( "No task running - not cancelling checkout" );
        }

        return false;
    }

    public int getBuildDefinitionId()
    {
        return buildDefinitionId;
    }

    public void setBuildDefinitionId( int buildDefinitionId )
    {
        this.buildDefinitionId = buildDefinitionId;
    }

    public int getProjectId()
    {
        return projectId;
    }

    public void setProjectId( int projectId )
    {
        this.projectId = projectId;
    }

    public int getTrigger()
    {
        return trigger;
    }

    public void setTrigger( int trigger )
    {
        this.trigger = trigger;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName( String projectName )
    {
        this.projectName = projectName;
    }

    public List<String> getSelectedBuildTaskHashCodes()
    {
        return selectedBuildTaskHashCodes;
    }

    public void setSelectedBuildTaskHashCodes( List<String> selectedBuildTaskHashCodes )
    {
        this.selectedBuildTaskHashCodes = selectedBuildTaskHashCodes;
    }

    public List<String> getSelectedCheckOutTaskHashCodes()
    {
        return selectedCheckOutTaskHashCodes;
    }

    public void setSelectedCheckOutTaskHashCodes( List<String> selectedCheckOutTaskHashCodes )
    {
        this.selectedCheckOutTaskHashCodes = selectedCheckOutTaskHashCodes;
    }

    public List<BuildProjectQueue> getCurrentBuildProjectTasks()
    {
        return currentBuildProjectTasks;
    }

    public void setCurrentBuildProjectTasks( List<BuildProjectQueue> currentBuildProjectTasks )
    {
        this.currentBuildProjectTasks = currentBuildProjectTasks;
    }

    public List<CheckoutQueue> getCurrentCheckoutTasks()
    {
        return currentCheckoutTasks;
    }

    public void setCurrentCheckoutTasks( List<CheckoutQueue> currentCheckoutTasks )
    {
        this.currentCheckoutTasks = currentCheckoutTasks;
    }

    public List<BuildProjectQueue> getBuildsInQueue()
    {
        return buildsInQueue;
    }

    public void setBuildsInQueue( List<BuildProjectQueue> buildsInQueue )
    {
        this.buildsInQueue = buildsInQueue;
    }

    public List<CheckoutQueue> getCheckoutsInQueue()
    {
        return checkoutsInQueue;
    }

    public void setCheckoutsInQueue( List<CheckoutQueue> checkoutsInQueue )
    {
        this.checkoutsInQueue = checkoutsInQueue;
    }

    public List<DistributedBuildSummary> getDistributedBuildSummary()
    {
        return distributedBuildSummary;
    }

    public void setDistributedBuildSummary( List<DistributedBuildSummary> distributedBuildSummary )
    {
        this.distributedBuildSummary = distributedBuildSummary;
    }

    public String getBuildAgentUrl()
    {
        return buildAgentUrl;
    }

    public void setBuildAgentUrl( String buildAgentUrl )
    {
        this.buildAgentUrl = buildAgentUrl;
    }

    public List<PrepareBuildProjectsTask> getDistributedBuildQueues()
    {
        return distributedBuildQueues;
    }

    public void setDistributedBuildQueues( List<PrepareBuildProjectsTask> distributedBuildQueues )
    {
        this.distributedBuildQueues = distributedBuildQueues;
    }

    public List<String> getSelectedDistributedBuildTaskHashCodes()
    {
        return selectedDistributedBuildTaskHashCodes;
    }

    public void setSelectedDistributedBuildTaskHashCodes( List<String> selectedDistributedBuildTaskHashCodes )
    {
        this.selectedDistributedBuildTaskHashCodes = selectedDistributedBuildTaskHashCodes;
    }

    public String getScmRootAddress()
    {
        return scmRootAddress;
    }

    public void setScmRootAddress( String scmRootAddress )
    {
        this.scmRootAddress = scmRootAddress;
    }

    private List<PrepareBuildProjectsTask> aggregateQueues()
        throws TaskQueueManagerException
    {
        List<PrepareBuildProjectsTask> aggregatedQueues = new ArrayList<PrepareBuildProjectsTask>();

        List<PrepareBuildProjectsTask> overallQueues =
            getContinuum().getTaskQueueManager().getDistributedBuildProjectsInQueue();

        Map<String, DistributedBuildTaskQueueExecutor> agentTaskQueueExecutors =
            distributedBuildManager.getTaskQueueExecutors();

        for ( String url : agentTaskQueueExecutors.keySet() )
        {
            try
            {
                logger.debug( "size of each queue snapshot " + url + " : " +
                    agentTaskQueueExecutors.get( url ).getQueue().getQueueSnapshot().size() );
                aggregatedQueues.addAll( agentTaskQueueExecutors.get( url ).getQueue().getQueueSnapshot() );
            }
            catch ( TaskQueueException e )
            {
                //silently ignore error
                logger.error( "Error encountered retrieving queue snapshot from queue :" + url, e );
            }
        }

        logger.debug( "size of agg. queue " + aggregatedQueues.size() );
        aggregatedQueues.addAll( overallQueues );

        return aggregatedQueues;
    }
}
