/**
 * 
 */
package org.ajdeveloppement.webserver.viewbinder.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author aurelien
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface View {
	Class<?> defaultMapperClass() default Void.class;
}
