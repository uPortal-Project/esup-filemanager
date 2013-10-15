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
 * A basic editor List< String > + params to List< UserMultivaluedAttributesEvaluator >.
 * @author GIP RECIA - Gribonvald Julien
 * 15 Oct. 2013
 */
public class ListUserMultivaluedAttributesEvaluatorEditor implements FactoryBean, InitializingBean {

    /** Logger.*/
    private static final Log LOG = LogFactory.getLog(ListUserMultivaluedAttributesEvaluatorEditor.class);

    /** */
    private List<String> valueList;
    /** */
    private String userAttribute;
    /** */
    private String mode;
	/** */
    private List<UserMultivaluedAttributesEvaluator> editedProperties;

    /**
     * Constructor of ListUserRoleEvaluatorEditor.java.
     */
    public ListUserMultivaluedAttributesEvaluatorEditor() {
        //block empty
    }

    /**
     * Constructor of ListUserRoleEvaluatorEditor.java.
     * @param arg0 list of values
     * @param arg1 attribute name
     * @param arg2 mode
     */
    public ListUserMultivaluedAttributesEvaluatorEditor(final List<String> arg0, final String arg1, final String arg2) {
        this.setAsText(arg0, arg1, arg2);
    }

    /**
     * @param arg0 list of values
     * @param arg1 attribute name
     * @param arg2 mode
     * @throws IllegalArgumentException
     * @see org.springframework.beans.propertyeditors.PropertiesEditor#setAsText(java.lang.String)
     */
    private void setAsText(final List<String> arg0, final String arg1, final String arg2) throws IllegalArgumentException {
        List<UserMultivaluedAttributesEvaluator> list = new LinkedList<UserMultivaluedAttributesEvaluator>();
        for (String value : arg0) {
		list.add(new UserMultivaluedAttributesEvaluator(value, arg1, arg2));
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("In : [" + arg0 + ", " + arg1 + ", " + arg2 + "] out : " + list.toString());
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
        Assert.notEmpty(getValueList(), "The property valueList in class "
                + this.getClass().getSimpleName() + " must not be null and not empty.");
        Assert.hasLength(getUserAttribute(), "The property userAttribute in class "
                + this.getClass().getSimpleName() + " must not be null and not empty.");
        Assert.hasLength(getMode(), "The property mode in class "
                + this.getClass().getSimpleName() + " must not be null and not empty.");

        this.setAsText(getValueList(), getUserAttribute(), getMode());
    }

    /**
	 * Getter of member valueList.
	 * @return <code>List<String></code> the attribute valueList
	 */
	public List<String> getValueList() {
		return valueList;
	}

	/**
	 * Setter of attribute valueList.
	 * @param valueList the attribute valueList to set
	 */
	public void setValueList(final List<String> valueList) {
		this.valueList = valueList;
	}

	/**
	 * Getter of member userAttribute.
	 * @return <code>String</code> the attribute userAttribute
	 */
	public String getUserAttribute() {
		return userAttribute;
	}

	/**
	 * Setter of attribute userAttribute.
	 * @param userAttribute the attribute userAttribute to set
	 */
	public void setUserAttribute(final String userAttribute) {
		this.userAttribute = userAttribute;
	}

	/**
	 * Getter of member mode.
	 * @return <code>String</code> the attribute mode
	 */
	public String getMode() {
		return mode;
	}

	/**
	 * Setter of attribute mode.
	 * @param mode the attribute mode to set
	 */
	public void setMode(final String mode) {
		this.mode = mode;
	}
}
