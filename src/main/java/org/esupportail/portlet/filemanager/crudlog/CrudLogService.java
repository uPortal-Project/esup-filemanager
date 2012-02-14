package org.esupportail.portlet.filemanager.crudlog;

import java.text.MessageFormat;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CrudLogService {

	private static String BEFORE_STRING = "[ entering < {0} > ]";
	
	private static String BEFORE_WITH_PARAMS_STRING = "[ entering < {0} > with params {1} ]";

	private static String AFTER_THROWING = "[ exception thrown < {0} > exception message {1} with params {2} ]";

	private static String AFTER_RETURNING = "[ leaving < {0} > returning {1} ]";

	private static String AFTER_RETURNING_VOID = "[ leaving < {0} > ]";

	protected static final Log log = LogFactory.getLog(CrudLogService.class);
	
	
	@Before(value = "@annotation(trace)", argNames = "joinPoint, trace")
	public void before(JoinPoint joinPoint, CrudLoggable loggable) {

		Class<? extends Object> clazz = joinPoint.getTarget().getClass();
		String name = joinPoint.getSignature().getName();
		
		if (ArrayUtils.isEmpty(joinPoint.getArgs())) {
			this.log(clazz, null, BEFORE_STRING, name,
				constructArgumentsString(clazz, joinPoint.getArgs()));
		} else {
			this.log(clazz, null, BEFORE_WITH_PARAMS_STRING, name,
					constructArgumentsString(clazz, joinPoint.getArgs()));			
		}
	}


	@AfterThrowing(value = "@annotation(org.esupportail.portlet.filemanager.crudlog.CrudLoggable)",
			throwing = "throwable", argNames = "joinPoint, throwable")
	public void afterThrowing(JoinPoint joinPoint, Throwable throwable) {

		Class<? extends Object> clazz = joinPoint.getTarget().getClass();
		String name = joinPoint.getSignature().getName();
		this.logError(clazz, throwable, AFTER_THROWING, name,
				throwable.getMessage(), constructArgumentsString(clazz,
						joinPoint.getArgs()));
	}

	@AfterReturning(value = "@annotation(trace)", returning = "returnValue",
			argNames = "joinPoint, trace, returnValue")
	public void afterReturning(JoinPoint joinPoint, CrudLoggable loggable,
			Object returnValue) {

		Class<? extends Object> clazz = joinPoint.getTarget().getClass();
		String name = joinPoint.getSignature().getName();

		if (joinPoint.getSignature() instanceof MethodSignature) {
			MethodSignature signature = (MethodSignature) joinPoint
					.getSignature();
			Class<?> returnType = signature.getReturnType();
			if (returnType.getName().compareTo("void") == 0) {
				this.log(clazz, null, AFTER_RETURNING_VOID,
						name, constructArgumentsString(clazz, returnValue));

				return;
			}
		}

		this.log(clazz, null, AFTER_RETURNING, name,
				constructArgumentsString(clazz, returnValue));
	}
	

	private String constructArgumentsString(Class<?> clazz, Object... arguments) {

		StringBuffer buffer = new StringBuffer();
		for (Object object : arguments) {
			buffer.append(object);
		}
		
		return buffer.toString();
	}
	
	private void log(Class<?> clazz,
			 Throwable throwable,  String pattern,
			 Object... arguments) {	
		String message = MessageFormat.format(pattern, arguments);		
		log.info(message, throwable);	
	}
	
	private void logError(Class<?> clazz,
			 Throwable throwable,  String pattern,
			 Object... arguments) {	
		String message = MessageFormat.format(pattern, arguments);		
		log.error(message, throwable);	
	}
	
}

