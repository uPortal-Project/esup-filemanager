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
package org.esupportail.portlet.filemanager.services.opencmis;

import org.esupportail.portlet.filemanager.exceptions.EsupStockNoRequestBoundException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.portlet.context.PortletRequestAttributes;

public class ContextUtils {

    /**
     * @param name
     * @return The attribute that corresponds to a name.
     */
    public static Object getSessionAttribute(final String name) {
        return getContextAttribute(name, RequestAttributes.SCOPE_SESSION);
    }

    /**
     * Set a session attribute.
     * @param name
     * @param value
     */
    public static void setSessionAttribute(
            final String name,
            final Object value) {
        setContextAttribute(name, value, RequestAttributes.SCOPE_SESSION);
    }

    /**
     * @return The request attributes.
     * @throws EsupStockNoRequestBoundException
     */
    static RequestAttributes getContextAttributes() throws EsupStockNoRequestBoundException {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            throw new EsupStockNoRequestBoundException();
        }
        if (!(requestAttributes instanceof ServletRequestAttributes)
                && !(requestAttributes instanceof PortletRequestAttributes)) {
            throw new IllegalArgumentException(
                    "requestAttributes of unknown class [" + requestAttributes.getClass() + "]");
        }
        return requestAttributes;
    }

    /**
     * Set an attribute for a given scope.
     * @param name
     * @param value
     * @param scope
     */
    private static void setContextAttribute(
            final String name,
            final Object value,
            final int scope) {
        getContextAttributes().setAttribute(name, value, scope);
    }

    /**
     * @param name
     * @param scope
     * @return The value of the attribute for a given scope.
     */
    private static Object getContextAttribute(
            final String name,
            final int scope) {
        return getContextAttributes().getAttribute(name, scope);
    }
}
