package org.apache.continuum.web.test;

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

import org.apache.continuum.web.test.parent.AbstractAdminTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test( groups = { "report" } )
public class ReportTest
    extends AbstractAdminTest
{
    private String projectGroupName;

    private String failedProjectGroupName;

    @BeforeClass
    public void createProject()
    {
        projectGroupName = getProperty( "REPORT_PROJECT_GROUP_NAME" );
        String projectGroupId = getProperty( "REPORT_PROJECT_GROUP_ID" );
        String projectGroupDescription = getProperty( "REPORT_PROJECT_GROUP_DESCRIPTION" );

        String projectName = getProperty( "MAVEN2_POM_PROJECT_NAME" );
        String projectPomUrl = getProperty( "MAVEN2_POM_URL" );
        String pomUsername = getProperty( "MAVEN2_POM_USERNAME" );
        String pomPassword = getProperty( "MAVEN2_POM_PASSWORD" );

        loginAsAdmin();
        addProjectGroup( projectGroupName, projectGroupId, projectGroupDescription, true, false );
        clickLinkWithText( projectGroupName );
        if ( !isLinkPresent( projectName ) )
        {
            addMavenTwoProject( projectPomUrl, pomUsername, pomPassword, projectGroupName, true );

            buildProjectGroup( projectGroupName, projectGroupId, projectGroupDescription, projectName, true );
        }
    }

    @BeforeMethod
    protected void setUp()
        throws Exception
    {
        failedProjectGroupName = getProperty( "MAVEN2_FAILING_PROJECT_POM_PROJECT_GROUP_NAME" );
    }

    @AfterMethod
    public void tearDown()
    {
        removeProjectGroup( failedProjectGroupName, false );
    }

    public void testViewBuildsReportWithSuccessfulBuild()
        throws Exception
    {
        goToProjectBuildsReport();
        selectValue( "buildStatus", "Ok" );
        clickButtonWithValue( "View Report" );

        assertProjectBuildReportWithResult();
        assertTextPresent( projectGroupName );
        assertImgWithAlt( "Success" );
    }

    /*@Test( dependsOnMethods = { "testProjectGroupAllBuildSuccess" } )
    public void testBuildsReportPagination()
        throws Exception
    {
        String M2_PROJ_GRP_NAME = getProperty( "M2_PROJ_GRP_NAME" );
        String M2_PROJ_GRP_ID = getProperty( "M2_PROJ_GRP_ID" );
        String M2_PROJ_GRP_DESCRIPTION = getProperty( "M2_PROJ_GRP_DESCRIPTION" );

        for ( int ctr = 0; ctr < 10; ctr++ )
        {
            buildProjectGroup( M2_PROJ_GRP_NAME, M2_PROJ_GRP_ID, M2_PROJ_GRP_DESCRIPTION, M2_PROJ_GRP_NAME, true );
            clickButtonWithValue( "Release" );
            assertReleaseSuccess();
        }

        goToProjectBuildsReport();
        setFieldValue( "rowCount", "10" );
        clickButtonWithValue( "View Report" );

        assertProjectBuildReportWithResult();
        assertLinkNotPresent( "Prev" );
        assertLinkNotPresent( "1" );
        assertLinkPresent( "2" );
        assertLinkPresent( "Next" );

        clickLinkWithText( "2" );
        assertProjectBuildReportWithResult();
        assertLinkNotPresent( "Next" );
        assertLinkNotPresent( "2" );
        assertLinkPresent( "1" );
        assertLinkPresent( "Prev" );

        clickLinkWithText( "Prev" );
        assertProjectBuildReportWithResult();
        assertLinkNotPresent( "Prev" );
        assertLinkNotPresent( "1" );
        assertLinkPresent( "2" );
        assertLinkPresent( "Next" );
    }*/

    public void testBuildsReportWithInvalidRowCount()
    {
        goToProjectBuildsReport();
        setFieldValue( "rowCount", "1" );
        clickButtonWithValue( "View Report" );

        assertProjectBuildReportWithFieldError();
        assertTextPresent( "Row count should be at least 10." );
    }

    public void testBuildsReportWithInvalidDates()
    {
        goToProjectBuildsReport();
        setFieldValue( "startDate", "05/25/2010" );
        setFieldValue( "endDate", "05/24/2010" );
        clickButtonWithValue( "View Report" );

        assertProjectBuildReportWithFieldError();
        assertTextPresent( "Start Date must be earlier than the End Date" );
    }

    public void testViewBuildsReportWithFailedBuild()
        throws Exception
    {
        String pomUrl = getProperty( "MAVEN2_FAILING_PROJECT_POM_URL" );
        String pomUsername = "";
        String pomPassword = "";

        String failedProjectGroupId = getProperty( "MAVEN2_FAILING_PROJECT_POM_PROJECT_GROUP_ID" );
        String failedProjectGroupDescription = getProperty( "MAVEN2_FAILING_PROJECT_POM_PROJECT_GROUP_DESCRIPTION" );

        addMavenTwoProject( pomUrl, pomUsername, pomPassword, null, true );
        assertProjectGroupSummaryPage( failedProjectGroupName, failedProjectGroupId, failedProjectGroupDescription );

        buildProjectGroup( failedProjectGroupName, failedProjectGroupId, failedProjectGroupDescription,
                           failedProjectGroupName, false );

        goToProjectBuildsReport();
        selectValue( "buildStatus", "Failed" );
        clickButtonWithValue( "View Report" );

        assertProjectBuildReportWithResult();
        assertImgWithAlt( "Failed" );
        assertTextPresent( failedProjectGroupName );
    }

    public void testViewBuildsReportWithErrorBuild()
    {
        goToProjectBuildsReport();
        selectValue( "buildStatus", "Error" );
        clickButtonWithValue( "View Report" );

        assertProjectBuildReportWithNoResult();
    }
}
