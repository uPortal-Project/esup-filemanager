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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.chemistry.opencmis.client.bindings.spi.AbstractAuthenticationProvider;

public class TrustedHttpheadersCmisAuthenticationProvider extends AbstractAuthenticationProvider  {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TrustedHttpheadersCmisAuthenticationProvider.class);

    public static final String ESUP_HEADER_SHIB_HTTP_HEADERS = "ESUP_HEADER_SHIB_HTTP_HEADERS";

    @Override
    public Map<String, List<String>> getHTTPHeaders(String url) {
        Map<String, List<String>> httpHeaders = null;
        Object httpHeadersObject = ContextUtils.getSessionAttribute(ESUP_HEADER_SHIB_HTTP_HEADERS);
        if(httpHeadersObject != null) {
            httpHeaders = (Map<String, List<String>>) httpHeadersObject;
            log.debug("httpHeaders: {}", httpHeaders.entrySet()
                    .stream()
                    .map(e -> e.getKey() + "=\"" + e.getValue() + "\"")
                    .collect(Collectors.joining(", ")));
        } else {
            log.warn("httpHeaders will be null : we don't retrieve any userinfos attributes !");
        }
        return httpHeaders;
    }
}
