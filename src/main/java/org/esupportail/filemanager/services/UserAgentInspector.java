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
package org.esupportail.filemanager.services;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class UserAgentInspector implements InitializingBean {

    private List<String> userAgentMobile;

    private final List<Pattern> patterns = new ArrayList<Pattern>();

    public void setUserAgentMobile(List<String> userAgentMobile) {
		this.userAgentMobile = userAgentMobile;
	}

	public void afterPropertiesSet() {
        // Compile our patterns
        for (String userAgent : userAgentMobile) {
            patterns.add(Pattern.compile(userAgent));
        }
    }

    public boolean isMobile(HttpServletRequest req) {

    	boolean isMobile = false;

        // Assertions.
        if (req == null) {
            String msg = "Argument 'req' cannot be null";
            throw new IllegalArgumentException(msg);
        }

        String userAgent = req.getHeader("User-Agent");
        if (userAgent != null && patterns.size() != 0) {
            for (Pattern pattern : patterns) {
                if (pattern.matcher(userAgent).matches()) {
                	isMobile = true;
                    break;
                }
            }
        }
        return isMobile;
    }
}
