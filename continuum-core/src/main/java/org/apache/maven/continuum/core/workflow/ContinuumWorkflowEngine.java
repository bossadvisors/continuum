package org.apache.maven.continuum.core.workflow;

/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import org.apache.maven.continuum.ContinuumException;

import java.util.Map;
import java.util.List;

import com.opensymphony.workflow.WorkflowException;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public interface ContinuumWorkflowEngine
{
    String ROLE = ContinuumWorkflowEngine.class.getName();

    long addProjectsFromMetadata( String username, String builderId, String metadataUrl, String workingDirectory,
                                  boolean userInteractive )
        throws ContinuumException;

    Map getContext( long workflowId )
        throws ContinuumException;

    void waitForWorkflow( long workflowId )
        throws ContinuumException;

    List getCurrentSteps( long workflowId )
        throws ContinuumException;

    void executeAction( long workflowId, int actionId, Map context )
        throws ContinuumException;
}
