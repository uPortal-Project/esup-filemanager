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
public class UserRoleEvaluator implements IDriveAccessEvaluator, InitializingBean {

	/** The portlet group value. */
	private String group;

	/**
	 * Contructor of the object UserRoleEvaluator.java.
	 */
	public UserRoleEvaluator() {
		// empty block
	}

	/**
	 * Contructor of the object UserRoleEvaluator.java.
	 * @param group The group name to evaluate.
	 */
	public UserRoleEvaluator(final String group) {
		this.group = group;
	}

	/**
	 * @see org.esupportail.portlet.filemanager.services.evaluators.IDriveAccessEvaluator#isApplicable(javax.portlet.PortletRequest)
	 */
	public boolean isApplicable(PortletRequest request) {
		if (request.isUserInRole(group))
			return true;
		return false;
	}

	/**
	 * Getter of member group.
	 * @return <code>String</code> the attribute group
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * Setter of attribute group.
	 * @param group the attribute group to set
	 */
	public void setGroup(final String group) {
		this.group = group;
	}

	/**
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.hasLength(group, "The group to compare must be set !");
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserRoleEvaluator [group=");
		builder.append(group);
		builder.append("]");
		return builder.toString();
	}
}