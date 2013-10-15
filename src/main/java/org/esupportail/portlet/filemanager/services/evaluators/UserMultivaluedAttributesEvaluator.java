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

import java.util.List;
import java.util.Map;

import javax.portlet.PortletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author GIP RECIA - Julien Gribonvald
 * 14 oct. 2013
 */
public class UserMultivaluedAttributesEvaluator extends UserAttributesEvaluator implements InitializingBean {

	/** Logger. */
	private static final Log log =  LogFactory.getLog(UserMultivaluedAttributesEvaluator.class);

	/**
	 * Contructor of the object UserMultivaluedAttributesEvaluator.java.
	 */
	public UserMultivaluedAttributesEvaluator() {
		super();
	}

	/**
	 * Contructor of the object UserMultivaluedAttributesEvaluator.java.
	 * @param attribute The User Attribute to get for evaluation.
	 * @param value The value to compare.
	 * @param mode The String comparator type.
	 */
	public UserMultivaluedAttributesEvaluator(final String attribute, final String value, final String mode) {
		super(attribute, value, mode);
	}

	/**
	 * @see org.esupportail.portlet.filemanager.services.evaluators.IDriveAccessEvaluator#isApplicable(javax.portlet.PortletRequest)
	 */
	@Override
	public boolean isApplicable(PortletRequest request) {
		final Map<String, List<String>> userInfos = (Map<String, List<String>>) request.getAttribute("org.jasig.portlet.USER_INFO_MULTIVALUED");
		if (userInfos == null)
			return false;
		final List<String> attribs = userInfos.get(this.getAttribute());

		if (log.isDebugEnabled()) {
			log.debug("value=" + this.getValue() + ",mode=" + this.getMode() + ",attrib=" + attribs);
		}

		// for tests other than 'exists' the attribute must be defined
		if ( (attribs == null || attribs.isEmpty()) && !Mode.EXISTS.equals(this.getMode()) )
			return false;

		if ( Mode.EQUALS.equals(this.getMode()) )
			return attribs.contains( this.getValue() );
		if ( Mode.EXISTS.equals(this.getMode()) )
			return attribs != null && !attribs.isEmpty();
		if ( Mode.STARTS_WITH.equals(this.getMode()) ) {
			for ( String val : attribs ) {
				if (val.startsWith(this.getValue()))
					return true;
			}
		}
		if ( Mode.ENDS_WITH.equals(this.getMode()) ) {
			for ( String val : attribs ) {
				if (val.endsWith(this.getValue()))
					return true;
			}
		}
		if ( Mode.CONTAINS.equals(this.getMode()) ) {
			for ( String val : attribs ) {
				if (val.indexOf( this.getValue() ) != -1 )
					return true;
			}
		}
		if ( Mode.MATCH.equals(this.getMode()) ) {
			for ( String val : attribs ) {
				if (val.matches( this.getValue() ) )
					return true;
			}
		}

		return false;
	}

}
