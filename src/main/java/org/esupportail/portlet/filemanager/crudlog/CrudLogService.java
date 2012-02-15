package org.esupportail.portlet.filemanager.crudlog;

import java.text.MessageFormat;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.esupportail.portlet.filemanager.beans.SharedUserPortletParameters;
import org.esupportail.portlet.filemanager.beans.UserPassword;
import org.esupportail.portlet.filemanager.services.FsAccess;
import org.esupportail.portlet.filemanager.services.ServersAccessService;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CrudLogService {

	private static String AFTER_THROWING = "{0} - {1} - params [ {2}] - exception {3}";

	private static String AFTER_RETURNING = "{0} - {1} - params [ {2}] - returning {3}";

	private static String AFTER_RETURNING_VOID = "{0} - {1} - params [ {2}]";

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
		String username = this.getUsername(joinPoint);
		this.logError(clazz, throwable, AFTER_THROWING, name,
				constructArgumentsString(clazz, name, username, joinPoint.getArgs()), throwable.getMessage());
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
			
			String username = this.getUsername(joinPoint);

			if (joinPoint.getSignature() instanceof MethodSignature) {
				MethodSignature signature = (MethodSignature) joinPoint
						.getSignature();
				Class<?> returnType = signature.getReturnType();
				if (returnType.getName().compareTo("void") == 0) {
					this.log(logLevel, clazz, AFTER_RETURNING_VOID,
							name, username, constructArgumentsString(clazz, joinPoint.getArgs()), constructArgumentsString(clazz, returnValue));

					return;
				}
			}

			this.log(logLevel, clazz, AFTER_RETURNING, name, username, constructArgumentsString(clazz, joinPoint.getArgs()),
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
			 String pattern,
			 Object... arguments) {	
		String message = MessageFormat.format(pattern, arguments);		
		if(CrudLogLevel.DEBUG.equals(logLevel))
			log.debug(message);	
		if(CrudLogLevel.INFO.equals(logLevel))
			log.info(message);	
	}
	
	private void logError(Class<?> clazz,
			 Throwable throwable,  String pattern,
			 Object... arguments) {	
		String message = MessageFormat.format(pattern, arguments);		
		log.error(message, throwable);	
	}
	
	private String getUsername(JoinPoint joinPoint) {
		Object[] args = joinPoint.getArgs();
		Object target = joinPoint.getTarget();
		String username = "undefined";
		if(args.length > 0) {
			String dir = null;
			if(args[0] instanceof String)
				dir = (String) args[0];
			if(args[0] instanceof List) {
				Object argList0 = ((List) args[0]).get(0);
				if(argList0 instanceof String)
					dir = (String) argList0;
			}
			if(dir != null) {
				SharedUserPortletParameters userParameters = null;
				for(Object arg: args) {
					if(arg instanceof SharedUserPortletParameters)
						userParameters = (SharedUserPortletParameters) arg;
				}
				if(userParameters != null) {
					ServersAccessService serverAccess = (ServersAccessService) target;
					String driveName = serverAccess.getDrive(dir);
					FsAccess fsAccess = serverAccess.getFsAccess(driveName, userParameters);
					UserPassword userPassword = fsAccess.getUserPassword(userParameters);
					if(userPassword != null)
						username = userPassword.getUsername();
				}
			}
		}
		return username;
	}
	
}

