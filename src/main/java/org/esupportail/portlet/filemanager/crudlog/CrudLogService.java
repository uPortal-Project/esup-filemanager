package org.esupportail.portlet.filemanager.crudlog;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CrudLogService {

	private static String AFTER_THROWING = "{0} - params [ {1}] - exception {2}";

	private static String AFTER_RETURNING = "{0} - params [ {1}] - returning {2}";

	private static String AFTER_RETURNING_VOID = "{0} - params [ {1}]";

	protected static final Log log = LogFactory.getLog(CrudLogService.class);
	
	
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
		this.logError(clazz, throwable, AFTER_THROWING, name,
				constructArgumentsString(clazz,joinPoint.getArgs()), throwable.getMessage());
	}

	@AfterReturning(value = "@annotation(loggable)", returning = "returnValue",
			argNames = "joinPoint, loggable, returnValue")
	public void afterReturning(JoinPoint joinPoint, CrudLoggable loggable,
			Object returnValue) {

		CrudLogLevel logLevel = loggable.value();

		if( CrudLogLevel.DEBUG.equals(logLevel) && log.isDebugEnabled() || 
				CrudLogLevel.INFO.equals(logLevel) && log.isInfoEnabled() ) {

			Class<? extends Object> clazz = joinPoint.getTarget().getClass();
			String name = joinPoint.getSignature().getName();

			if (joinPoint.getSignature() instanceof MethodSignature) {
				MethodSignature signature = (MethodSignature) joinPoint
						.getSignature();
				Class<?> returnType = signature.getReturnType();
				if (returnType.getName().compareTo("void") == 0) {
					this.log(logLevel, clazz, null, AFTER_RETURNING_VOID,
							name, constructArgumentsString(clazz, joinPoint.getArgs()), constructArgumentsString(clazz, returnValue));

					return;
				}
			}

			this.log(logLevel, clazz, null, AFTER_RETURNING, name, constructArgumentsString(clazz, joinPoint.getArgs()),
					constructArgumentsString(clazz, returnValue));
		}
	}
	

	private String constructArgumentsString(Class<?> clazz, Object... arguments) {

		StringBuffer buffer = new StringBuffer();
		for (Object object : arguments) {
			buffer.append(object).append(" ");
		}
		
		return buffer.toString();
	}
	
	private void log(CrudLogLevel logLevel, Class<?> clazz,
			 Throwable throwable,  String pattern,
			 Object... arguments) {	
		String message = MessageFormat.format(pattern, arguments);		
		if(CrudLogLevel.DEBUG.equals(logLevel))
			log.debug(message, throwable);	
		if(CrudLogLevel.INFO.equals(logLevel))
			log.info(message, throwable);	
	}
	
	private void logError(Class<?> clazz,
			 Throwable throwable,  String pattern,
			 Object... arguments) {	
		String message = MessageFormat.format(pattern, arguments);		
		log.error(message, throwable);	
	}
	
}

