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

import javax.portlet.PortletRequest;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * @author GIP RECIA - Julien Gribonvald
 * 14 oct. 2013
 */
public class PreferenceEvaluator implements IDriveAccessEvaluator, InitializingBean {

	/** The portlet preference attribute. */
	private String attribute;
	/** The portlet preference value. */
	private String value;

	/**
	 * Contructor of the object PreferenceEvaluator.java.
	 */
	public PreferenceEvaluator() {
		super();
	}

	/**
	 * Contructor of the object PreferenceEvaluator.java.
	 * @param attribute The portlet Attribute to get for evaluation.
	 * @param value The value to compare.
	 */
	public PreferenceEvaluator(final String attribute, final String value) {
		this.attribute = attribute;
		this.value = value;
	}

	/**
	 * @see org.esupportail.portlet.filemanager.services.evaluators.IDriveAccessEvaluator#isApplicable(javax.portlet.PortletRequest)
	 */
	public boolean isApplicable(PortletRequest request) {
		final String pref = request.getPreferences().getValue(attribute, null);
		if (pref != null && pref.equals(value) )
			return true;
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
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.hasLength(attribute, "The portlet Attribute to evaluate must be set !");
		Assert.hasLength(value, "The value to compare must be set !");
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PreferenceEvaluator [attribute=");
		builder.append(attribute);
		builder.append(", value=");
		builder.append(value);
		builder.append("]");
		return builder.toString();
	}
}