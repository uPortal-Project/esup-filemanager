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
package org.esupportail.filemanager.crudlog;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.esupportail.filemanager.beans.CasUser;
import org.esupportail.filemanager.exceptions.EsupStockLostSessionException;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
@Aspect
@Component
public class CrudLogService {

    private static String AFTER_THROWING = "{0} - {1} - {2} - params [ {3}] - exception {4}";

    private static String AFTER_RETURNING = "{0} - {1} - {2} - params [ {3}] - returning {4}";

    private static String AFTER_RETURNING_VOID = "{0} - {1} - {2} - params [ {3}]";

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CrudLogService.class);


	/*
	@Before(value = "@annotation(loggable)", argNames = "joinPoint, loggable")
	public void before(JoinPoint joinPoint, CrudLoggable loggable) {
	}
	*/


    @AfterThrowing(value = "@annotation(loggable)",
            throwing = "throwable", argNames = "joinPoint, loggable, throwable")
    public void afterThrowing(JoinPoint joinPoint, CrudLoggable loggable, Throwable throwable) {

        Class<? extends Object> clazz = joinPoint.getTarget().getClass();
        String name = joinPoint.getSignature().getName();

        Map<String, String> userInfos = this.getUserInfos(joinPoint);
        String username = userInfos.get("username");
        String clientIpAddress = userInfos.get("clientIpAddress");

        // EsupStockLostSessionException signals a normal "not yet authenticated" workflow:
        // log at WARN without stack trace to avoid polluting ERROR logs.
        if (throwable instanceof EsupStockLostSessionException) {
            log.warn(MessageFormat.format(AFTER_THROWING, name,
                    constructArgumentsString(name, clientIpAddress, username, joinPoint.getArgs()),
                    throwable.getMessage(), "", ""));
            return;
        }

        this.logError(throwable, AFTER_THROWING, name,
                constructArgumentsString(name, clientIpAddress, username, joinPoint.getArgs()), throwable.getMessage());
    }

    @AfterReturning(value = "@annotation(loggable)", returning = "returnValue",
            argNames = "joinPoint, loggable, returnValue")
    public void afterReturning(JoinPoint joinPoint, CrudLoggable loggable,
                               Object returnValue) {

        CrudLogLevel logLevel = loggable.value();

        if( CrudLogLevel.DEBUG.equals(logLevel) && log.isDebugEnabled() ||
                CrudLogLevel.INFO.equals(logLevel) && log.isInfoEnabled() ) {

            String name = joinPoint.getSignature().getName();

            Map<String, String> userInfos = this.getUserInfos(joinPoint);
            String username = userInfos.get("username");
            String clientIpAddress = userInfos.get("clientIpAddress");

            if (joinPoint.getSignature() instanceof MethodSignature) {
                MethodSignature signature = (MethodSignature) joinPoint
                        .getSignature();
                Class<?> returnType = signature.getReturnType();
                if (returnType.getName().compareTo("void") == 0) {
                    this.log(logLevel, AFTER_RETURNING_VOID,
                            name, clientIpAddress, username, constructArgumentsString(joinPoint.getArgs()), constructArgumentsString(returnValue));

                    return;
                }
            }

            this.log(logLevel, AFTER_RETURNING, name, clientIpAddress, username, constructArgumentsString(joinPoint.getArgs()),
                    constructArgumentsString(returnValue));
        }
    }


    private String constructArgumentsString(Object... arguments) {

        StringBuffer buffer = new StringBuffer();
        for (Object object : arguments) {
            buffer.append(object).append(" ");
        }

        return buffer.toString();
    }

    private void log(CrudLogLevel logLevel,
                     String pattern,
                     Object... arguments) {
        String message = MessageFormat.format(pattern, arguments);
        if(CrudLogLevel.DEBUG.equals(logLevel))
            log.debug(message);
        if(CrudLogLevel.INFO.equals(logLevel))
            log.info(message);
    }

    private void logError(Throwable throwable, String pattern,
                          Object... arguments) {
        String message = MessageFormat.format(pattern, arguments);
        log.error(message, throwable);
    }

    private Map<String, String> getUserInfos(JoinPoint joinPoint) {

        String username = "undefined";
        String clientIpAddress = "undefined";


        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if(authentication instanceof CasAuthenticationToken casAuthenticationToken) {
            CasUser casUser = (CasUser) casAuthenticationToken.getUserDetails();
            username = casUser.getUsername();
            Object clientIpAddressAttr = casUser.getAttributes().get("clientIpAddress");
            if (clientIpAddressAttr instanceof String) {
                clientIpAddress = (String)clientIpAddressAttr;
            }
        }

        Map<String, String> userInfos = new HashMap<String, String>();
        userInfos.put("username", username);
        userInfos.put("clientIpAddress", clientIpAddress);

        return userInfos;
    }
}

