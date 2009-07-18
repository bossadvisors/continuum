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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.continuum.web.test.parent.AbstractBuildEnvironmentTest;
import org.testng.annotations.Test;

/**
 * @author José Morales Martínez
 * @version $Id$
 */
@Test( groups = { "buildEnvironment" } )
public class BuildEnvironmentTest
    extends AbstractBuildEnvironmentTest
{
    public void testAddBuildEnvironment()
    {
        loginAsAdminIfNeeded();
        String BUIL_ENV_NAME = getProperty( "BUIL_ENV_NAME" ) + getTestId();
        goToAddBuildEnvironment();
        addBuildEnvironment( BUIL_ENV_NAME, new String[] {}, true );
    }

    public void testAddInvalidBuildEnvironment()
    {
        loginAsAdminIfNeeded();
        goToAddBuildEnvironment();
        addBuildEnvironment( "", new String[] {}, false );
        assertTextPresent( "You must define a name" );
    }

    @Test( dependsOnMethods = { "testAddBuildEnvironment" } )
    public void testEditInvalidBuildEnvironment()
    {
        loginAsAdminIfNeeded();
        String BUIL_ENV_NAME = getProperty( "BUIL_ENV_NAME" ) + getTestId();
        goToEditBuildEnvironment( BUIL_ENV_NAME );
        editBuildEnvironment( "", new String[] {}, false );
        assertTextPresent( "You must define a name" );
    }

    @Test( dependsOnMethods = { "testAddBuildEnvironment" } )
    public void testAddDuplicatedBuildEnvironment()
    {
        loginAsAdminIfNeeded();
        String BUIL_ENV_NAME = getProperty( "BUIL_ENV_NAME" ) + getTestId();
        goToAddBuildEnvironment();
        addBuildEnvironment( BUIL_ENV_NAME, new String[] {}, false );
        assertTextPresent( "A Build Environment with the same name already exists" );
    }

    @Test( dependsOnMethods = { "testAddBuildEnvironment" } )
    public void testEditBuildEnvironment()
    {
        loginAsAdminIfNeeded();
        String BUIL_ENV_NAME = getProperty( "BUIL_ENV_NAME" ) + getTestId();
        String newName = "new_name";
        goToEditBuildEnvironment( BUIL_ENV_NAME );
        editBuildEnvironment( newName, new String[] {}, true );
        // TODO: ADD INSTALLATIONS TO ENVIROTMENT
        goToEditBuildEnvironment( newName );
        editBuildEnvironment( BUIL_ENV_NAME, new String[] {}, true );
    }

    @Test( dependsOnMethods = { "testEditInvalidBuildEnvironment", "testEditBuildEnvironment",
        "testAddDuplicatedBuildEnvironment", "testEditInvalidBuildEnvironment" } )
    public void testDeleteBuildEnvironment()
    {
        loginAsAdminIfNeeded();
        String BUIL_ENV_NAME = getProperty( "BUIL_ENV_NAME" ) + getTestId();
        removeBuildEnvironment( BUIL_ENV_NAME );
    }
}