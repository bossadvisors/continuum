package org.apache.continuum.web.action.admin;

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

import com.opensymphony.xwork2.Preparable;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.continuum.configuration.BuildAgentConfiguration;
import org.apache.continuum.model.repository.AbstractPurgeConfiguration;
import org.apache.continuum.model.repository.DirectoryPurgeConfiguration;
import org.apache.continuum.model.repository.DistributedDirectoryPurgeConfiguration;
import org.apache.continuum.purge.ContinuumPurgeManager;
import org.apache.continuum.purge.PurgeConfigurationService;
import org.apache.maven.continuum.model.project.Schedule;
import org.apache.maven.continuum.security.ContinuumRoleConstants;
import org.apache.maven.continuum.web.action.ContinuumConfirmAction;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.redback.integration.interceptor.SecureAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component( role = com.opensymphony.xwork2.Action.class, hint = "distributedPurgeConfiguration", instantiationStrategy = "per-lookup"  )
public class DistributedPurgeConfigurationAction
    extends ContinuumConfirmAction
    implements Preparable, SecureAction
{
    private static final Logger logger = LoggerFactory.getLogger( DistributedPurgeConfigurationAction.class );

    private static final String PURGE_TYPE_DIRECTORY = "directory";

    private static final String PURGE_DIRECTORY_RELEASES = "releases";

    private static final String PURGE_DIRECTORY_WORKING = "working";

    private static final int DEFAULT_RETENTION_COUNT = 2;

    private static final int DEFAULT_DAYS_OLDER = 100;

    private String purgeType;

    private String directoryType;

    private String description;

    private String message;

    private boolean deleteAll;

    private boolean deleteReleasedSnapshots;

    private boolean enabled;

    private boolean confirmed;

    private boolean defaultPurgeConfiguration;

    private int retentionCount;

    private int daysOlder;

    private int scheduleId;

    private int purgeConfigId;

    private String buildAgentUrl;

    private AbstractPurgeConfiguration purgeConfig;

    private Map<Integer, String> schedules;

    private List<String> directoryTypes;

    private List<String> buildAgentUrls;

    @Requirement
    private PurgeConfigurationService purgeConfigService;

    @Override
    public void prepare()
        throws Exception
    {
        super.prepare();

        // build schedules
        if ( schedules == null )
        {
            schedules = new HashMap<Integer, String>();

            Collection<Schedule> allSchedules = getContinuum().getSchedules();

            for ( Schedule schedule : allSchedules )
            {
                schedules.put( schedule.getId(), schedule.getName() );
            }
        }

        // build repositories
        if ( buildAgentUrls == null )
        {
            List<BuildAgentConfiguration> buildAgents = getContinuum().getConfiguration().getBuildAgents();
            buildAgentUrls = new ArrayList<String>( buildAgents.size() );
            for ( BuildAgentConfiguration buildAgent : buildAgents )
            {
                buildAgentUrls.add( buildAgent.getUrl() );
            }
            Collections.sort( buildAgentUrls );
        }

        directoryTypes = new ArrayList<String>();
        directoryTypes.add( PURGE_DIRECTORY_RELEASES );
        directoryTypes.add( PURGE_DIRECTORY_WORKING );
    }

    @Override
    public String input()
        throws Exception
    {
        if ( purgeConfigId != 0 )
        {
            purgeConfig = purgeConfigService.getPurgeConfiguration( purgeConfigId );

            if ( purgeConfig instanceof DistributedDirectoryPurgeConfiguration )
            {
                DistributedDirectoryPurgeConfiguration dirPurge = (DistributedDirectoryPurgeConfiguration) purgeConfig;

                this.purgeType = PURGE_TYPE_DIRECTORY;
                this.daysOlder = dirPurge.getDaysOlder();
                this.retentionCount = dirPurge.getRetentionCount();
                this.directoryType = dirPurge.getDirectoryType();
                this.deleteAll = dirPurge.isDeleteAll();
                this.enabled = dirPurge.isEnabled();
                this.defaultPurgeConfiguration = dirPurge.isDefaultPurge();
                this.description = dirPurge.getDescription();
                this.buildAgentUrl = dirPurge.getBuildAgentUrl();
                if ( dirPurge.getSchedule() != null )
                {
                    this.scheduleId = dirPurge.getSchedule().getId();
                }
            }
        }
        else
        {
            this.retentionCount = DEFAULT_RETENTION_COUNT;
            this.daysOlder = DEFAULT_DAYS_OLDER;
        }

        return INPUT;
    }

    public String save()
        throws Exception
    {
        if ( purgeConfigId == 0 )
        {
            purgeConfig = new DistributedDirectoryPurgeConfiguration();

            purgeConfig = setupPurgeConfiguration( purgeConfig );

            purgeConfig = purgeConfigService.addPurgeConfiguration( purgeConfig );
        }
        else
        {
            purgeConfig = purgeConfigService.getPurgeConfiguration( purgeConfigId );
            purgeConfig = setupPurgeConfiguration( purgeConfig );

            purgeConfigService.updatePurgeConfiguration( purgeConfig );
        }

        /*if ( purgeConfig.isDefaultPurge() )
        {
            updateDefaultPurgeConfiguration();
        }*/

        if ( purgeConfig.isEnabled() && purgeConfig.getSchedule() != null )
        {
            getContinuum().activePurgeSchedule( purgeConfig.getSchedule() );
        }

        return SUCCESS;
    }

    public String remove()
        throws Exception
    {

        if ( confirmed )
        {
            purgeConfigService.removePurgeConfiguration( purgeConfigId );
        }
        else
        {
            return CONFIRM;
        }

        return SUCCESS;
    }

    public String purge()
        throws Exception
    {

        ContinuumPurgeManager purgeManager = getContinuum().getPurgeManager();

        if ( purgeConfigId > 0 )
        {
            purgeConfig = purgeConfigService.getPurgeConfiguration( purgeConfigId );

            DistributedDirectoryPurgeConfiguration dirPurge = (DistributedDirectoryPurgeConfiguration) purgeConfig;
            purgeManager.purgeDistributedDirectory( dirPurge );
        }

        return SUCCESS;
    }

    public String getPurgeType()
    {
        return this.purgeType;
    }

    public void setPurgeType( String purgeType )
    {
        this.purgeType = purgeType;
    }

    public String getDirectoryType()
    {
        return this.directoryType;
    }

    public void setDirectoryType( String directoryType )
    {
        this.directoryType = directoryType;
    }

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public String getMessage()
    {
        return this.message;
    }

    public void setMessage( String message )
    {
        this.message = message;
    }

    public boolean isDeleteAll()
    {
        return this.deleteAll;
    }

    public void setDeleteAll( boolean deleteAll )
    {
        this.deleteAll = deleteAll;
    }

    public boolean isDeleteReleasedSnapshots()
    {
        return this.deleteReleasedSnapshots;
    }

    public void setDeleteReleasedSnapshots( boolean deleteReleasedSnapshots )
    {
        this.deleteReleasedSnapshots = deleteReleasedSnapshots;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }

    @Override
    public boolean isConfirmed()
    {
        return this.confirmed;
    }

    @Override
    public void setConfirmed( boolean confirmed )
    {
        this.confirmed = confirmed;
    }

    public boolean isDefaultPurgeConfiguration()
    {
        return this.defaultPurgeConfiguration;
    }

    public void setDefaultPurgeConfiguration( boolean defaultPurgeConfiguration )
    {
        this.defaultPurgeConfiguration = defaultPurgeConfiguration;
    }

    public int getRetentionCount()
    {
        return this.retentionCount;
    }

    public void setRetentionCount( int retentionCount )
    {
        this.retentionCount = retentionCount;
    }

    public int getDaysOlder()
    {
        return this.daysOlder;
    }

    public void setDaysOlder( int daysOlder )
    {
        this.daysOlder = daysOlder;
    }

    public int getScheduleId()
    {
        return this.scheduleId;
    }

    public void setScheduleId( int scheduleId )
    {
        this.scheduleId = scheduleId;
    }

    public int getPurgeConfigId()
    {
        return purgeConfigId;
    }

    public void setPurgeConfigId( int purgeConfigId )
    {
        this.purgeConfigId = purgeConfigId;
    }

    public AbstractPurgeConfiguration getPurgeConfig()
    {
        return this.purgeConfig;
    }

    public void setPurgeConfig( AbstractPurgeConfiguration purgeConfig )
    {
        this.purgeConfig = purgeConfig;
    }

    public Map<Integer, String> getSchedules()
    {
        return this.schedules;
    }

    public void setSchedules( Map<Integer, String> schedules )
    {
        this.schedules = schedules;
    }

    public List<String> getDirectoryTypes()
    {
        return this.directoryTypes;
    }

    public void setDirectoryTypes( List<String> directoryTypes )
    {
        this.directoryTypes = directoryTypes;
    }

    private AbstractPurgeConfiguration setupPurgeConfiguration( AbstractPurgeConfiguration purgeConfiguration )
        throws Exception
    {
        return buildDirPurgeConfiguration();
    }

    public String getBuildAgentUrl()
    {
        return buildAgentUrl;
    }

    public void setBuildAgentUrl( String buildAgentUrl )
    {
        this.buildAgentUrl = buildAgentUrl;
    }

    public List<String> getBuildAgentUrls()
    {
        return buildAgentUrls;
    }

    public void setBuildAgentUrls( List<String> buildAgentUrls )
    {
        this.buildAgentUrls = buildAgentUrls;
    }

    private DistributedDirectoryPurgeConfiguration buildDirPurgeConfiguration()
        throws Exception
    {
        DistributedDirectoryPurgeConfiguration dirPurge = (DistributedDirectoryPurgeConfiguration) purgeConfig;
        dirPurge.setDeleteAll( this.deleteAll );
        dirPurge.setEnabled( this.enabled );
        dirPurge.setDaysOlder( this.daysOlder );
        dirPurge.setRetentionCount( this.retentionCount );
        dirPurge.setDirectoryType( this.directoryType );
        dirPurge.setDefaultPurge( this.defaultPurgeConfiguration );
        dirPurge.setBuildAgentUrl( buildAgentUrl );

        // escape xml to prevent xss attacks
        dirPurge.setDescription( StringEscapeUtils.escapeXml( StringEscapeUtils.unescapeXml( this.description ) ) );

        if ( scheduleId > 0 )
        {
            Schedule schedule = getContinuum().getSchedule( scheduleId );
            dirPurge.setSchedule( schedule );
        }

        return dirPurge;
    }

    private void updateDefaultPurgeConfiguration()
        throws Exception
    {
        DirectoryPurgeConfiguration dirPurge = purgeConfigService.getDefaultPurgeConfigurationForDirectoryType(
            directoryType );

        if ( dirPurge != null && dirPurge.getId() != purgeConfig.getId() )
        {
            dirPurge.setDefaultPurge( false );
            purgeConfigService.updateDirectoryPurgeConfiguration( dirPurge );
        }
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();
        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ContinuumRoleConstants.CONTINUUM_MANAGE_PURGING, Resource.GLOBAL );

        return bundle;
    }
}
