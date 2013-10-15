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

import javax.portlet.PortletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * @author GIP RECIA - Julien Gribonvald
 * 14 oct. 2013
 */
public class GroupEvaluator implements IDriveAccessEvaluator, InitializingBean {

	/**
	 * Type operator to eavluate the list of evaluators.
	 */
	public enum Type {
        OR,
        AND,
        NOT;
    }

	/** The logger. */
	private static Log LOG = LogFactory.getLog(GroupEvaluator.class);

	/** Operator type on evaluators list. */
	private Type type = null;

	/** List of evaluators to evaluate, must contains more than one evaluator. */
	private List<IDriveAccessEvaluator> evaluators = new LinkedList<IDriveAccessEvaluator>();

	/**
	 * Contructor of the object GroupEvaluator.java.
	 */
	public GroupEvaluator() {
		//TODO nothing
	}

	/**
	 * Contructor of the object GroupEvaluator.java.
	 * @param type Type operator to test evaluators. Null if only one evluator
	 * @param evaluators List of Evaluation test.
	 */
	public GroupEvaluator(final String type, final List<IDriveAccessEvaluator> evaluators) {
		this.type = Type.valueOf(type);
		this.evaluators.addAll(evaluators);
	}

	/**
	 * Do evaluation in function of type all evaluators
	 * @param request Current PortletRequest
	 * @return boolean value depending on evaluation.
	 * @see org.esupportail.portlet.filemanager.services.evaluators.IDriveAccessEvaluator#isApplicable(PortletRequest)
	 */
	public boolean isApplicable(PortletRequest request) {
		boolean rslt = false;
        if (LOG.isDebugEnabled())
            LOG.debug(" >>>> calling GroupEvaluator[" + this + ", op=" + type +
                    "].isApplicable()");

        if (type != null) {

	        switch (this.type) {
	        case OR: {
			rslt = false;   // presume false in this case...
			for(IDriveAccessEvaluator v : this.evaluators) {
				if ( v.isApplicable( request ) ) {
					rslt = true;
					break;
				}
			}
	        } break;

	        case AND: {
			rslt = true;   // presume true in this case...
			for(IDriveAccessEvaluator v : this.evaluators) {
				if ( v.isApplicable( request ) == false ) {
					rslt = false;
					break;
				}
			}
	        } break;

	        case NOT: {
			rslt = false;   // presume false in this case... until later...
			for(IDriveAccessEvaluator v : this.evaluators) {
				if ( v.isApplicable( request ) ) {
					rslt = true;
					break;
				}
			}
			rslt = !rslt;
	        } break;
	        }
        }

        if (LOG.isDebugEnabled())
		LOG.debug(" ---- GroupEvaluator[" + this + ", op=" + type
				+ "].isApplicable()=" + rslt);

		return rslt;
	}

	/**
	 * Getter of member type.
	 * @return <code>Type</code> the attribute type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Setter of attribute type.
	 * @param type the attribute type to set
	 */
	public void setType(final Type type) {
		this.type = type;
	}

	/**
	 * Setter of attribute type.
	 * @param type the attribute type to set
	 */
	public void setType(final String type) {
		this.type = Type.valueOf(type);
	}

	/**
	 * Getter of member evaluators.
	 * @return <code>List<IDriveAccessEvaluator></code> the attribute evaluators
	 */
	public List<IDriveAccessEvaluator> getEvaluators() {
		return evaluators;
	}

	/**
	 * Setter of attribute evaluators.
	 * @param evaluators the attribute evaluators to set
	 */
	public void setEvaluators(final List<IDriveAccessEvaluator> evaluators) {
		this.evaluators.addAll(evaluators);
	}

	/**
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(type, "The type of evaluator should not be null, values are [OR, AND, NOT]!");
		Assert.notEmpty(evaluators, "The evaluator list can't be null or empty !");
		Assert.isTrue((evaluators.size() > 1 && !Type.NOT.equals(type)) || (Type.NOT.equals(type) && evaluators.size() == 1),
				"You need to set more than one evaluator when operator type != NOT or You need to set only one evaluator when operator type = NOT !");
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GroupEvaluator [type=");
		builder.append(type);
		builder.append(", evaluators=");
		builder.append(evaluators);
		builder.append("]");
		return builder.toString();
	}

}
