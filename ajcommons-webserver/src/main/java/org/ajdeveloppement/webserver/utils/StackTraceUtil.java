/**
 * 
 */
package org.ajdeveloppement.webserver.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author a.jeoffray
 *
 */
public class StackTraceUtil {
	public static String getStackTrace(Throwable throwable) {
		StringWriter errors = new StringWriter();
		throwable.printStackTrace(new PrintWriter(errors));
		
		return errors.toString();
	}
}
