/**
 * 
 */
package org.ajdeveloppement.webserver.viewbinder.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author aurelien
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(Collections.class)
public @interface Collection {
	Class<?> value();
	String name();
}
