package org.apache.maven.continuum.web.action;

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

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.Preparable;
import org.apache.maven.continuum.Continuum;
import org.apache.maven.continuum.execution.ContinuumBuildExecutorConstants;
import org.apache.maven.continuum.security.ContinuumRoleConstants;
import org.apache.maven.continuum.web.exception.AuthenticationRequiredException;
import org.apache.maven.continuum.web.exception.AuthorizationRequiredException;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.redback.authorization.AuthorizationException;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.system.SecuritySystemConstants;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserNotFoundException;
import org.codehaus.plexus.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

/**
 * ContinuumActionSupport
 *
 * @author Jesse McConnell <jesse@codehaus.org>
 */
public class ContinuumActionSupport
    extends PlexusActionSupport
    implements Preparable
{
    private SecuritySession securitySession;

    @Requirement
    private SecuritySystem securitySystem;

    protected static final String REQUIRES_AUTHENTICATION = "requires-authentication";

    protected static final String REQUIRES_AUTHORIZATION = "requires-authorization";

    protected static final String RELEASE_ERROR = "releaseError";

    protected static final String ERROR_MSG_AUTHORIZATION_REQUIRED = "You are not authorized to access this page. " +
        "Please contact your administrator to be granted the appropriate permissions.";

    protected static final String ERROR_MSG_PROCESSING_AUTHORIZATION =
        "An error occurred while performing authorization.";

    @Requirement
    private Continuum continuum;

    protected final SimpleDateFormat dateFormatter = new SimpleDateFormat( "MMM dd, yyyy hh:mm:ss aaa z" );

    public void prepare()
        throws Exception
    {
        if ( securitySession == null )
        {
            securitySession = (SecuritySession) getContext().getSession().get(
                SecuritySystemConstants.SECURITY_SESSION_KEY );
        }
    }

    public Continuum getContinuum()
    {
        return continuum;
    }

    public void setContinuum( Continuum continuum )
    {
        this.continuum = continuum;
    }

    public String doDefault()
        throws Exception
    {
        return REQUIRES_AUTHORIZATION;
    }

    public String input()
        throws Exception
    {
        return REQUIRES_AUTHORIZATION;
    }

    public String execute()
        throws Exception
    {
        return REQUIRES_AUTHORIZATION;
    }

    /**
     * Check if the current user is authorized to do the action
     *
     * @param role the role
     * @throws AuthorizationRequiredException if the user isn't authorized
     */
    protected void checkAuthorization( String role )
        throws AuthorizationRequiredException
    {
        checkAuthorization( role, null, false );
    }

    /**
     * Check if the current user is authorized to do the action
     *
     * @param role     the role
     * @param resource the operation resource
     * @throws AuthorizationRequiredException if the user isn't authorized
     */
    protected void checkAuthorization( String role, String resource )
        throws AuthorizationRequiredException
    {
        checkAuthorization( role, resource, true );
    }

    /**
     * Check if the current user is authorized to do the action
     *
     * @param role             the role
     * @param resource         the operation resource
     * @param requiredResource true if resource can't be null
     * @throws AuthorizationRequiredException if the user isn't authorized
     */
    protected void checkAuthorization( String role, String resource, boolean requiredResource )
        throws AuthorizationRequiredException
    {
        try
        {
            if ( resource != null && StringUtils.isNotEmpty( resource.trim() ) )
            {
                if ( !getSecuritySystem().isAuthorized( getSecuritySession(), role, resource ) )
                {
                    throw new AuthorizationRequiredException( ERROR_MSG_AUTHORIZATION_REQUIRED );
                }
            }
            else
            {
                if ( requiredResource || !getSecuritySystem().isAuthorized( getSecuritySession(), role ) )
                {
                    throw new AuthorizationRequiredException( ERROR_MSG_AUTHORIZATION_REQUIRED );
                }
            }
        }
        catch ( AuthorizationException ae )
        {
            throw new AuthorizationRequiredException( ERROR_MSG_PROCESSING_AUTHORIZATION );
        }
    }

    /**
     * Check if the current user is authorized to view the specified project group
     *
     * @param resource the operation resource
     * @throws AuthorizationRequiredException if the user isn't authorized if the user isn't authorized
     */
    protected void checkViewProjectGroupAuthorization( String resource )
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_VIEW_GROUP_OPERATION, resource );
    }

    /**
     * Check if the current user is authorized to add a project group
     *
     * @throws AuthorizationRequiredException if the user isn't authorized if the user isn't authorized
     */
    protected void checkAddProjectGroupAuthorization()
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_ADD_GROUP_OPERATION );
    }

    /**
     * Check if the current user is authorized to delete the specified project group
     *
     * @param resource the operation resource
     * @throws AuthorizationRequiredException if the user isn't authorized if the user isn't authorized
     */
    protected void checkRemoveProjectGroupAuthorization( String resource )
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_REMOVE_GROUP_OPERATION, resource );
    }

    /**
     * Check if the current user is authorized to build the specified project group
     *
     * @param resource the operation resource
     * @throws AuthorizationRequiredException if the user isn't authorized if the user isn't authorized
     */
    protected void checkBuildProjectGroupAuthorization( String resource )
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_BUILD_GROUP_OPERATION, resource );
    }

    /**
     * Check if the current user is authorized to modify the specified project group
     *
     * @param resource the operation resource
     * @throws AuthorizationRequiredException if the user isn't authorized if the user isn't authorized
     */
    protected void checkModifyProjectGroupAuthorization( String resource )
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_MODIFY_GROUP_OPERATION, resource );
    }

    /**
     * Check if the current user is authorized to add a project to a specific project group
     *
     * @param resource the operation resource
     * @throws AuthorizationRequiredException if the user isn't authorized if the user isn't authorized
     */
    protected void checkAddProjectToGroupAuthorization( String resource )
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_ADD_PROJECT_TO_GROUP_OPERATION, resource );
    }

    /**
     * Check if the current user is authorized to delete a project from a specified group
     *
     * @param resource the operation resource
     * @throws AuthorizationRequiredException if the user isn't authorized if the user isn't authorized
     */
    protected void checkRemoveProjectFromGroupAuthorization( String resource )
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_REMOVE_PROJECT_FROM_GROUP_OPERATION, resource );
    }

    /**
     * Check if the current user is authorized to modify a project in the specified group
     *
     * @param resource the operation resource
     * @throws AuthorizationRequiredException if the user isn't authorized if the user isn't authorized
     */
    protected void checkModifyProjectInGroupAuthorization( String resource )
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_MODIFY_PROJECT_IN_GROUP_OPERATION, resource );
    }

    /**
     * Check if the current user is authorized to build a project in the specified group
     *
     * @param resource the operation resource
     * @throws AuthorizationRequiredException if the user isn't authorized if the user isn't authorized
     */
    protected void checkBuildProjectInGroupAuthorization( String resource )
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_BUILD_PROJECT_IN_GROUP_OPERATION, resource );
    }

    /**
     * Check if the current user is authorized to add a build definition for the specified
     * project group
     *
     * @param resource the operation resource
     * @throws AuthorizationRequiredException if the user isn't authorized if the user isn't authorized
     */
    protected void checkAddGroupBuildDefinitionAuthorization( String resource )
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_ADD_GROUP_BUILD_DEFINTION_OPERATION, resource );
    }

    /**
     * Check if the current user is authorized to delete a build definition in the specified
     * project group
     *
     * @param resource the operation resource
     * @throws AuthorizationRequiredException if the user isn't authorized if the user isn't authorized
     */
    protected void checkRemoveGroupBuildDefinitionAuthorization( String resource )
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_REMOVE_GROUP_BUILD_DEFINITION_OPERATION, resource );
    }

    /**
     * Check if the current user is authorized to modify a build definition in the specified
     * project group
     *
     * @param resource the operation resource
     * @throws AuthorizationRequiredException if the user isn't authorized if the user isn't authorized
     */
    protected void checkModifyGroupBuildDefinitionAuthorization( String resource )
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_MODIFY_GROUP_BUILD_DEFINITION_OPERATION, resource );
    }

    /**
     * Check if the current user is authorized to add a group build definition to a specific
     * project
     *
     * @param resource the operation resource
     * @throws AuthorizationRequiredException if the user isn't authorized if the user isn't authorized
     */
    protected void checkAddProjectBuildDefinitionAuthorization( String resource )
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_ADD_PROJECT_BUILD_DEFINTION_OPERATION, resource );
    }

    /**
     * Check if the current user is authorized to modify a build definition of a specific project
     *
     * @param resource the operation resource
     * @throws AuthorizationRequiredException if the user isn't authorized if the user isn't authorized
     */
    protected void checkModifyProjectBuildDefinitionAuthorization( String resource )
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_MODIFY_PROJECT_BUILD_DEFINITION_OPERATION, resource );
    }

    /**
     * Check if the current user is authorized to delete a build definition of a specific
     * project
     *
     * @param resource the operation resource
     * @throws AuthorizationRequiredException if the user isn't authorized if the user isn't authorized
     */
    protected void checkRemoveProjectBuildDefinitionAuthorization( String resource )
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_REMOVE_PROJECT_BUILD_DEFINITION_OPERATION, resource );
    }

    /**
     * Check if the current user is authorized to add a notifier to the specified
     * project group
     *
     * @param resource the operation resource
     * @throws AuthorizationRequiredException if the user isn't authorized if the user isn't authorized
     */
    protected void checkAddProjectGroupNotifierAuthorization( String resource )
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_ADD_GROUP_NOTIFIER_OPERATION, resource );
    }

    /**
     * Check if the current user is authorized to delete a notifier in the specified
     * project group
     *
     * @param resource the operation resource
     * @throws AuthorizationRequiredException if the user isn't authorized if the user isn't authorized
     */
    protected void checkRemoveProjectGroupNotifierAuthorization( String resource )
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_REMOVE_GROUP_NOTIFIER_OPERATION, resource );
    }

    /**
     * Check if the current user is authorized to modify a notifier in the specified
     * project group
     *
     * @param resource the operartion resource
     * @throws AuthorizationRequiredException if the user isn't authorized if the user isn't authorized
     */
    protected void checkModifyProjectGroupNotifierAuthorization( String resource )
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_MODIFY_GROUP_NOTIFIER_OPERATION, resource );
    }

    /**
     * Check if the current user is authorized to add a notifier to a specific project
     *
     * @param resource the operation resource
     * @throws AuthorizationRequiredException if the user isn't authorized if the user isn't authorized
     */
    protected void checkAddProjectNotifierAuthorization( String resource )
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_ADD_PROJECT_NOTIFIER_OPERATION, resource );
    }

    /**
     * Check if the current user is authorized to delete a notifier in a specific project
     *
     * @param resource the operation resource
     * @throws AuthorizationRequiredException if the user isn't authorized if the user isn't authorized
     */
    protected void checkRemoveProjectNotifierAuthorization( String resource )
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_REMOVE_PROJECT_NOTIFIER_OPERATION, resource );
    }

    /**
     * Check if the current user is authorized to modify a notifier in a specific project
     *
     * @param resource the operation resource
     * @throws AuthorizationRequiredException if the user isn't authorized if the user isn't authorized
     */
    protected void checkModifyProjectNotifierAuthorization( String resource )
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_MODIFY_PROJECT_NOTIFIER_OPERATION, resource );
    }

    /**
     * Check if the current user is authorized to manage the application's configuration
     *
     * @throws AuthenticationRequiredException if the user isn't authorized if the user isn't authenticated
     * @throws AuthorizationRequiredException  if the user isn't authorized if the user isn't authorized
     */
    protected void checkManageConfigurationAuthorization()
        throws AuthenticationRequiredException, AuthorizationRequiredException
    {
        if ( !isAuthenticated() )
        {
            throw new AuthenticationRequiredException( "Authentication required." );
        }

        checkAuthorization( ContinuumRoleConstants.CONTINUUM_MANAGE_CONFIGURATION );
    }

    /**
     * Check if the current user is authorized to manage the project build schedules
     *
     * @throws AuthenticationRequiredException if the user isn't authorized if the user isn't authenticated
     * @throws AuthorizationRequiredException  if the user isn't authorized if the user isn't authorized
     */
    protected void checkManageSchedulesAuthorization()
        throws AuthenticationRequiredException, AuthorizationRequiredException
    {
        if ( !isAuthenticated() )
        {
            throw new AuthenticationRequiredException( "Authentication required." );
        }

        checkAuthorization( ContinuumRoleConstants.CONTINUUM_MANAGE_SCHEDULES );
    }

    /**
     * Check if the current user is authorized to manage queues
     *
     * @throws AuthenticationRequiredException if the user isn't authenticated
     * @throws AuthorizationRequiredException  if the user isn't authorized
     */
    protected void checkManageQueuesAuthorization()
        throws AuthenticationRequiredException, AuthorizationRequiredException
    {
        if ( !isAuthenticated() )
        {
            throw new AuthenticationRequiredException( "Authentication required" );
        }

        checkAuthorization( ContinuumRoleConstants.CONTINUUM_MANAGE_QUEUES );
    }

    protected void checkManageLocalRepositoriesAuthorization()
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_MANAGE_REPOSITORIES );
    }

    protected void checkViewReportsAuthorization()
        throws AuthorizationRequiredException
    {
        checkAuthorization( ContinuumRoleConstants.CONTINUUM_VIEW_REPORT );
    }

    /**
     * Get the security session
     *
     * @return current SecuritySession
     */
    private SecuritySession getSecuritySession()
    {

        return securitySession;
    }

    /**
     * Get the action context
     *
     * @return action context
     */
    private ActionContext getContext()
    {

        return ActionContext.getContext();
    }

    /**
     * Get the security system
     *
     * @return the security system
     */
    protected SecuritySystem getSecuritySystem()
    {
        return securitySystem;
    }

    protected boolean requiresAuthentication()
    {
        return true;
    }

    /**
     * Check if the current user is already authenticated
     *
     * @return true if the user is authenticated
     */
    public boolean isAuthenticated()
    {
        if ( requiresAuthentication() )
        {
            if ( getSecuritySession() == null || !getSecuritySession().isAuthenticated() )
            {
                return false;
            }
        }

        return true;
    }

    protected ResourceBundle getResourceBundle()
    {
        return getTexts( "localization/Continuum" );
    }

    protected String getPrincipal()
    {
        String principal = "guest";

        if ( getSecuritySession() != null )
        {
            if ( getSecuritySession().getUser() != null )
            {
                principal = (String) getSecuritySession().getUser().getPrincipal();
            }
        }
        else
        {
            principal = "unknown-user";
        }
        return principal;
    }

    protected User getUser( String principal )
        throws UserNotFoundException
    {
        return getSecuritySystem().getUserManager().findUser( principal );
    }

    /**
     * Convenience method to determine whether a build is a maven build. We could call the static method directly,
     * but for struts2 validator access, we would need to enable static method invocation.
     *
     * @param buildType
     * @return true if the build type is will result in a maven 1 or 2+ build.
     */
    public boolean isMavenBuildType( String buildType )
    {
        return ContinuumBuildExecutorConstants.isMaven( buildType );
    }
}
