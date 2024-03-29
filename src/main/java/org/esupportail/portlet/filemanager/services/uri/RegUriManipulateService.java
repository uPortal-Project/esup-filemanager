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
package org.esupportail.portlet.filemanager.services.uri;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegUriManipulateService implements UriManipulateService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RegUriManipulateService.class);

    protected String regexp = "";

    protected String replacement = "";

    public void setRegexp(String regexp) {
        this.regexp = regexp;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    public  String manipulate(String uri) {
        String outUri = "";
        outUri = uri;
        // we check if the path is a regular expression
        if (regexp!=null && replacement !=null) {
            Pattern p = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(outUri);
            outUri = m.replaceAll(replacement);
        }
        if(log.isDebugEnabled())
            log.debug("RegUriManipulateService:: input uri: '{}' -- output uri: '{}'", uri, outUri);

        return outUri;
    }
}
