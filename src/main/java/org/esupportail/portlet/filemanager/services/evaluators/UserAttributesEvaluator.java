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

import java.util.Map;

import javax.portlet.PortletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * @author GIP RECIA - Julien Gribonvald
 * 14 oct. 2013
 */
public class UserAttributesEvaluator implements IDriveAccessEvaluator, InitializingBean {

	public enum Mode {
		CONTAINS,
		EQUALS,
		STARTS_WITH,
		ENDS_WITH,
		EXISTS,
		MATCH,
	}

	/** Logger. */
	private static final Log log =  LogFactory.getLog(UserAttributesEvaluator.class);

	/** The portlet preference attribute. */
	private String attribute;
	/** The portlet preference value. */
	private String value;
	/** The mode to evaluate the attribute. */
	private Mode mode;

	/**
	 * Contructor of the object UserAttributesEvaluator.java.
	 */
	public UserAttributesEvaluator() {
		super();
	}

	/**
	 * Contructor of the object UserAttributesEvaluator.java.
	 * @param mode The String comparator type.
	 */
	public UserAttributesEvaluator(final String mode) {
		this.mode = Mode.valueOf(mode);
	}

	/**
	 * Contructor of the object UserAttributesEvaluator.java.
	 * @param attribute The User Attribute to get for evaluation.
	 * @param value The value to compare.
	 * @param mode The String comparator type.
	 */
	public UserAttributesEvaluator(final String attribute, final String value, final String mode) {
		this.attribute = attribute;
		this.value = value;
		this.mode = Mode.valueOf(mode);
	}

	/**
	 * @see org.esupportail.portlet.filemanager.services.evaluators.IDriveAccessEvaluator#isApplicable(javax.portlet.PortletRequest)
	 */
	public boolean isApplicable(PortletRequest request) {
		final Map<String, String> userInfos = (Map<String, String>) request.getAttribute(PortletRequest.USER_INFO);
		if (userInfos == null)
			return false;

		final String attrib = userInfos.get(attribute);

		if (log.isDebugEnabled()) {
			log.debug("value=" + value + ",mode=" + mode + ",attrib=" + attrib);
		}

		// for tests other than 'exists' the attribute must be defined
		if ( attrib == null && !Mode.EXISTS.equals(mode) )
			return false;

		if ( Mode.EQUALS.equals(mode) )
			return attrib.equals( value );
		if ( Mode.EXISTS.equals(mode) )
			return attrib != null;
		if ( Mode.STARTS_WITH.equals(mode) )
			return attrib.startsWith( value );
		if ( Mode.ENDS_WITH.equals(mode) )
			return attrib.endsWith( value );
		if ( Mode.CONTAINS.equals(mode) )
			return (attrib.indexOf( value ) != -1 );
		if ( Mode.MATCH.equals(mode) )
			return attrib.matches(value);
		// will never get here
		return false;
	}

	/**
	 * Getter of member attribute.
	 * @return <code>String</code> the attribute attribute
	 */
	public String getAttribute() {
		return attribute;
	}

	/**
	 * Setter of attribute attribute.
	 * @param attribute the attribute attribute to set
	 */
	public void setAttribute(final String attribute) {
		this.attribute = attribute;
	}

	/**
	 * Getter of member value.
	 * @return <code>String</code> the attribute value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Setter of attribute value.
	 * @param value the attribute value to set
	 */
	public void setValue(final String value) {
		this.value = value;
	}

	/**
	 * Getter of member mode.
	 * @return <code>Mode</code> the attribute mode
	 */
	public Mode getMode() {
		return mode;
	}

	/**
	 * Setter of attribute mode.
	 * @param mode the attribute mode to set
	 */
	public void setMode(final Mode mode) {
		this.mode = mode;
	}

	/**
	 * Setter of attribute mode.
	 * @param mode the attribute mode to set
	 */
	public void setMode(final String mode) {
		this.mode = Mode.valueOf(mode);
	}

	/**
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.hasLength(attribute, "The portlet Attribute to evaluate must be set !");
		Assert.hasLength(value, "The value to compare must be set !");
		Assert.notNull(mode, "The comparison mode must not be null !");
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserAttributesEvaluator [attribute=");
		builder.append(attribute);
		builder.append(", value=");
		builder.append(value);
		builder.append(", mode=");
		builder.append(mode);
		builder.append("]");
		return builder.toString();
	}

}
