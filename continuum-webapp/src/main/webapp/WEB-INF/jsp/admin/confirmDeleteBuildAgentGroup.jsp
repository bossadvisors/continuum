<%--
  ~ Licensed to the Apache Software Foundation (ASF) under one
  ~ or more contributor license agreements.  See the NOTICE file
  ~ distributed with this work for additional information
  ~ regarding copyright ownership.  The ASF licenses this file
  ~ to you under the Apache License, Version 2.0 (the
  ~ "License"); you may not use this file except in compliance
  ~ with the License.  You may obtain a copy of the License at
  ~
  ~   http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  --%>

<%@ taglib uri="/struts-tags" prefix="s" %>

<html>
  <s:i18n name="localization.Continuum">
    <head>
      <title><s:text name="deleteBuildAgentGroup.page.title"/></title>
    </head>
    <body>
      <div id="axial" class="h3">
        <h3><s:text name="deleteBuildAgentGroup.section.title"/></h3>

        <div class="warningmessage">
          <p>
            <strong>
              <s:text name="deleteBuildAgentGroup.confirmation.message">
                <s:param><s:property value="buildAgentGroup.name"/></s:param>
              </s:text>
            </strong>
          </p>
        </div>
        <div class="functnbar3">
          <s:form action="deleteBuildAgentGroup.action" method="post">
            <s:token/>
            <input type="hidden" name="buildAgentGroup.name" value="${buildAgentGroup.name}" />
            <s:hidden name="confirmed" value="true"/>
            <s:submit value="%{getText('delete')}" theme="simple"/>
            <input type="button" name="Cancel" value="<s:text name='cancel'/>" onclick="history.back();"/>
          </s:form>
        </div>
      </div>
    </body>
  </s:i18n>
</html>
