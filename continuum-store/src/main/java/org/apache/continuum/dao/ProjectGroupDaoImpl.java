package org.apache.continuum.dao;

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

import org.apache.maven.continuum.model.project.Project;
import org.apache.maven.continuum.model.project.ProjectGroup;
import org.apache.maven.continuum.store.ContinuumObjectNotFoundException;
import org.apache.maven.continuum.store.ContinuumStore;
import org.apache.maven.continuum.store.ContinuumStoreException;
import org.codehaus.plexus.jdo.PlexusJdoUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 * @plexus.component role="org.apache.continuum.dao.ProjectGroupDao"
 */
public class ProjectGroupDaoImpl
    extends AbstractDao
    implements ProjectGroupDao
{
    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ContinuumStore store;

    public ProjectGroup addProjectGroup( ProjectGroup group )
    {
        return (ProjectGroup) addObject( group );
    }

    public void removeProjectGroup( ProjectGroup projectGroup )
    {
        ProjectGroup pg = null;
        try
        {
            pg = getProjectGroupWithProjects( projectGroup.getId() );
        }
        catch ( Exception e )
        {
            // Do nothing
        }

        if ( pg != null )
        {
            // TODO: why do we need to do this? if not - build results are not
            // removed and a integrity constraint is violated. I assume its
            // because of the fetch groups
            for ( Project p : (List<Project>) pg.getProjects() )
            {
                store.removeProject( p );
            }
            removeObject( pg );
        }
    }

    public ProjectGroup getProjectGroup( int projectGroupId )
        throws ContinuumStoreException
    {
        return (ProjectGroup) getObjectById( ProjectGroup.class, projectGroupId );
    }

    public ProjectGroup getProjectGroupByGroupId( String groupId )
        throws ContinuumStoreException
    {
        return (ProjectGroup) getObjectFromQuery( ProjectGroup.class, "groupId", groupId, null );
    }

    public ProjectGroup getProjectGroupByGroupIdWithBuildDetails( String groupId )
        throws ContinuumStoreException
    {
        return (ProjectGroup) getObjectFromQuery( ProjectGroup.class, "groupId", groupId,
                                                  PROJECT_BUILD_DETAILS_FETCH_GROUP );
    }

    public ProjectGroup getProjectGroupByGroupIdWithProjects( String groupId )
        throws ContinuumStoreException
    {
        return (ProjectGroup) getObjectFromQuery( ProjectGroup.class, "groupId", groupId,
                                                  PROJECTGROUP_PROJECTS_FETCH_GROUP );
    }

    public ProjectGroup getProjectGroupByProjectId( int projectId )
        throws ContinuumObjectNotFoundException
    {
        try
        {
            return store.getProject( projectId ).getProjectGroup();
        }
        catch ( ContinuumStoreException e )
        {
            throw new ContinuumObjectNotFoundException(
                "unable to find project group containing project with id: " + projectId );

        }
    }

    public ProjectGroup getProjectGroupByProject( Project project )
        throws ContinuumObjectNotFoundException
    {
        return getProjectGroupByProjectId( project.getId() );
    }

    public void updateProjectGroup( ProjectGroup group )
        throws ContinuumStoreException
    {
        updateObject( group );
    }

    public ProjectGroup getProjectGroupWithProjects( int projectGroupId )
        throws ContinuumStoreException
    {
        return (ProjectGroup) getObjectById( ProjectGroup.class, projectGroupId, PROJECTGROUP_PROJECTS_FETCH_GROUP );
    }

    public Collection<ProjectGroup> getAllProjectGroupsWithProjects()
    {
        return getAllObjectsDetached( ProjectGroup.class, "name ascending", PROJECTGROUP_PROJECTS_FETCH_GROUP );
    }

    public List<ProjectGroup> getAllProjectGroupsWithBuildDetails()
    {
        return getAllObjectsDetached( ProjectGroup.class, "name ascending", PROJECT_BUILD_DETAILS_FETCH_GROUP );
    }

    public List<ProjectGroup> getAllProjectGroups()
    {
        return getAllObjectsDetached( ProjectGroup.class, "name ascending", null );
    }

    public List<ProjectGroup> getAllProjectGroupsWithTheLot()
    {
        List fetchGroups = Arrays.asList( new String[]{PROJECT_WITH_BUILDS_FETCH_GROUP,
            PROJECTGROUP_PROJECTS_FETCH_GROUP, BUILD_RESULT_WITH_DETAILS_FETCH_GROUP,
            PROJECT_WITH_CHECKOUT_RESULT_FETCH_GROUP, PROJECT_ALL_DETAILS_FETCH_GROUP,
            PROJECT_BUILD_DETAILS_FETCH_GROUP} );
        return PlexusJdoUtils.getAllObjectsDetached( getPersistenceManager(), ProjectGroup.class, "name ascending",
                                                     fetchGroups );
    }

    public ProjectGroup getProjectGroupWithBuildDetailsByProjectGroupId( int projectGroupId )
        throws ContinuumStoreException
    {
        return (ProjectGroup) getObjectById( ProjectGroup.class, projectGroupId, PROJECT_BUILD_DETAILS_FETCH_GROUP );
    }
}