/**
 * Licensed to EsupPortail under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * EsupPortail licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.esupportail.portlet.filemanager.services.evaluators;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * A basic editor List< String > to List< UserRoleEvaluator >.
 * @author GIP RECIA - Gribonvald Julien
 * 15 Oct 2013
 */
public class ListUserRoleEvaluatorEditor implements FactoryBean, InitializingBean {

    /** Logger.*/
    private static final Log LOG = LogFactory.getLog(ListUserRoleEvaluatorEditor.class);

    /** */
    private List<String> groupList;
    /** */
    private List<UserRoleEvaluator> editedProperties;

    /**
     * Constructor of ListUserRoleEvaluatorEditor.java.
     */
    public ListUserRoleEvaluatorEditor() {
        //block empty
    }

    /**
     * Constructor of ListUserRoleEvaluatorEditor.java.
     * @param arg0
     */
    public ListUserRoleEvaluatorEditor(final List<String> arg0) {
        this.setAsText(arg0);
    }

    /**
     * @param arg0
     * @throws IllegalArgumentException
     * @see org.springframework.beans.propertyeditors.PropertiesEditor#setAsText(java.lang.String)
     */
    private void setAsText(final List<String> arg0) throws IllegalArgumentException {
        List<UserRoleEvaluator> list = new LinkedList<UserRoleEvaluator>();
        for (String grp : arg0) {
		list.add(new UserRoleEvaluator(grp));
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("String in : " + arg0 + " List out : " + list.toString());
        }
        this.editedProperties = list;
    }

    /**
     * @return <code>Object</code> Here returns a List of LDAP attributes names.
     * @throws Exception
     * @see org.springframework.beans.factory.FactoryBean#getObject()
     */
    public Object getObject() throws Exception {
        // TODO Auto-generated method stub
        return editedProperties;
    }

    /**
     * @return <code>Class</code> The class name of the object returned.
     * @see org.springframework.beans.factory.FactoryBean#getObjectType()
     */
    @SuppressWarnings("rawtypes")
	public Class getObjectType() {
        // TODO Auto-generated method stub
        return List.class;
    }

    /**
     * @return <code>boolean</code>
     * @see org.springframework.beans.factory.FactoryBean#isSingleton()
     */
    public boolean isSingleton() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * @throws Exception
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        Assert.notEmpty(getGroupList(), "The property groupList in class "
                + this.getClass().getSimpleName() + " must not be null and not empty.");

        this.setAsText(getGroupList());
    }

    /**
     * Getter du membre groupList.
     * @return <code>String</code> le membre groupList.
     */
    public List<String> getGroupList() {
        return groupList;
    }

    /**
     * Setter du membre groupList.
     * @param groupList la nouvelle valeur du membre groupList.
     */
    public void setGroupList(final List<String> groupList) {
        this.groupList = groupList;
    }



}
