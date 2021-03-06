package org.apache.continuum.release.distributed.manager;

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

import org.apache.continuum.dao.BuildResultDao;
import org.apache.maven.continuum.model.project.BuildResult;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

import static org.mockito.Mockito.*;

/**
 * DefaultDistributedReleaseManagerTest
 */
public class DefaultDistributedReleaseManagerTest
    extends PlexusInSpringTestCase
{
    private DefaultDistributedReleaseManager distributedReleaseManager;

    private BuildResultDao buildResultDao;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        distributedReleaseManager = new DefaultDistributedReleaseManager();

        buildResultDao = mock( BuildResultDao.class );
        distributedReleaseManager.setBuildResultDao( buildResultDao );
    }

    public void testGetDefaultBuildagent()
        throws Exception
    {
        String defaultBuildagentUrl = "http://localhost:8181/continuum-buildagent/xmlrpc";

        BuildResult buildResult = new BuildResult();
        buildResult.setBuildUrl( defaultBuildagentUrl );

        when( buildResultDao.getLatestBuildResultForProject( 0 ) ).thenReturn( buildResult );

        String returnedBuildagent = distributedReleaseManager.getDefaultBuildagent( 0 );

        assertNotNull( returnedBuildagent );
        assertEquals( returnedBuildagent, defaultBuildagentUrl );
    }

    public void testGetDefaultBuildagentNullBuildResult()
        throws Exception
    {
        BuildResult buildResult = null;

        when( buildResultDao.getLatestBuildResultForProject( 0 ) ).thenReturn( buildResult );

        String returnedBuildagent = distributedReleaseManager.getDefaultBuildagent( 0 );

        assertNull( returnedBuildagent );
    }
}
