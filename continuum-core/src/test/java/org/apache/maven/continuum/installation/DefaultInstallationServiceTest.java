package org.apache.maven.continuum.installation;

import java.util.List;

import org.apache.maven.continuum.AbstractContinuumTest;
import org.apache.maven.continuum.model.system.Installation;
import org.apache.maven.continuum.store.ContinuumStore;

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

/**
 * @author <a href="mailto:olamy@codehaus.org">olamy</a>
 * @since 13 juin 07
 * @version $Id$
 */
public class DefaultInstallationServiceTest
    extends AbstractContinuumTest
{
    private static final String DEFAULT_INSTALLATION_NAME = "defaultInstallation";

    private static final String NEW_INSTALLATION_NAME = "newInstallation";

    public Installation defaultInstallation;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        getStore().eraseDatabase();
        if ( getInstallationService().getInstallation( DEFAULT_INSTALLATION_NAME ) == null )
        {
            defaultInstallation = createDefault();
            ContinuumStore store = getStore();
            defaultInstallation = store.addInstallation( defaultInstallation );
        }
    }

    private Installation createDefault()
    {
        Installation installation = new Installation();
        installation.setType( "description" );
        installation.setName( DEFAULT_INSTALLATION_NAME );
        installation.setVarName( "varName" );
        installation.setVarValue( "varValue" );
        return installation;
    }

    private InstallationService getInstallationService()
        throws Exception
    {
        //Continuum continuum = (Continuum) lookup( Continuum.ROLE );
        //return continuum.getInstallationService();
        return (InstallationService) lookup( InstallationService.ROLE );
    }

    private Installation addInstallation( String name, String varName, String varValue, String type )
        throws Exception
    {

        Installation installation = new Installation();
        installation.setType( InstallationService.JDK_TYPE );
        installation.setName( name );
        installation.setVarName( varName );
        installation.setVarValue( varValue );
        return getInstallationService().add( installation );
    }

    public void testAddInstallation()
        throws Exception
    {
        this.addInstallation( NEW_INSTALLATION_NAME, null, "bar", InstallationService.JDK_TYPE );
        Installation getted = getInstallationService().getInstallation( NEW_INSTALLATION_NAME );
        assertNotNull( getted );
        assertEquals( getInstallationService().getEnvVar( InstallationService.JDK_TYPE ), getted.getVarName() );
        assertEquals( "bar", getted.getVarValue() );
    }

    public void testgetOne()
        throws Exception
    {
        Installation getted = getInstallationService().getInstallation( DEFAULT_INSTALLATION_NAME );
        assertNotNull( getted );
        assertEquals( defaultInstallation.getType(), getted.getType() );
        assertEquals( defaultInstallation.getName(), getted.getName() );
        assertEquals( defaultInstallation.getVarName(), getted.getVarName() );
        assertEquals( defaultInstallation.getVarValue(), getted.getVarValue() );
    }

    public void testRemove()
        throws Exception
    {
        String name = "toremove";
        this.addInstallation( name, "foo", "bar", InstallationService.JDK_TYPE );
        Installation getted = getInstallationService().getInstallation( name );
        assertNotNull( getted );
        getInstallationService().delete( getted );
        getted = getInstallationService().getInstallation( name );
        assertNull( getted );

    }

    public void testUpdate()
        throws Exception
    {
        String name = "toupdate";
        this.addInstallation( name, "foo", "bar", InstallationService.JDK_TYPE );
        Installation getted = getInstallationService().getInstallation( name );
        assertNotNull( getted );
        assertEquals( getInstallationService().getEnvVar( InstallationService.JDK_TYPE ), getted.getVarName() );
        assertEquals( "bar", getted.getVarValue() );
        getted.setVarName( "updatefoo" );
        getted.setVarValue( "updatedbar" );
        getInstallationService().update( getted );
        getted = getInstallationService().getInstallation( name );
        assertNotNull( getted );
        assertEquals( getInstallationService().getEnvVar( InstallationService.JDK_TYPE ), getted.getVarName() );
        assertEquals( "updatedbar", getted.getVarValue() );
    }

    public void testgetDefaultJdkInformations()
        throws Exception
    {
        InstallationService installationService = (InstallationService) lookup( InstallationService.ROLE, "default" );
        List<String> infos = installationService.getDefaultJdkInformations();
        assertNotNull( infos );
    }

    public void testgetJdkInformations()
        throws Exception
    {
        InstallationService installationService = (InstallationService) lookup( InstallationService.ROLE, "default" );
        String javaHome = System.getProperty( "JAVA_HOME" );
        Installation installation = new Installation();
        installation.setName( "test" );
        installation.setType( InstallationService.JDK_TYPE );
        installation.setVarValue( javaHome );

        List<String> infos = installationService.getJdkInformations( installation );
        assertNotNull( infos );
    }
}
