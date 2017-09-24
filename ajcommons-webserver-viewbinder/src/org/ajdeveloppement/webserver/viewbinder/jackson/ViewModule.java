/**
 * 
 */
package org.ajdeveloppement.webserver.viewbinder.jackson;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.ajdeveloppement.webserver.viewbinder.annotations.CollectionType;
import org.ajdeveloppement.webserver.viewbinder.annotations.View;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;

/**
 * @author aurelien
 *
 */
public class ViewModule extends Module {

	@SuppressWarnings("unchecked")
	private static <T> T map(Class<T> klaus, final ObjectNode data) {
		return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{klaus}, new InvocationHandler() {
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				String property = null;
				if(method.getName().startsWith("is"))
					property = method.getName().substring(2);
				else if(method.getName().startsWith("get") || method.getName().startsWith("set"))
					property = method.getName().substring(3);
				
				property = new String(new char[]{property.charAt(0)}).toLowerCase() + property.substring(1);

				// getter
				if ((method.getName().startsWith("get") || method.getName().startsWith("is")) && method.getParameterTypes().length == 0) {
					return getProperty(data, property, method);
				}

				// setter
				if (method.getName().startsWith("set") && method.getParameterTypes().length == 1) {
					data.putPOJO(property, args[0]);
					return null;
				}

				return null;
			}
		});
	}
	
	/**
	 * @param returnType
	 * @param value
	 */
	private static Object getJsonValue(Class<?> returnType, JsonNode value, Method method) {
		if((value == null || value.isNull()) && !returnType.isPrimitive())
			return null;
		
		if (Integer.class.equals(returnType) || int.class.equals(returnType))
			return value != null ? value.asInt() : 0;
		else if (Double.class.equals(returnType) || double.class.equals(returnType))
			return value != null ? value.asDouble() : 0.0d;
		else if (Float.class.equals(returnType) || float.class.equals(returnType))
			return value != null ? (float)value.asDouble() : 0.0f;
		else if (Long.class.equals(returnType) || long.class.equals(returnType))
			return value != null ? value.asLong(): 0l;
		else if (Boolean.class.equals(returnType) || boolean.class.equals(returnType))
			return value != null ? value.asBoolean() : false;
		else if (String.class.equals(returnType))
			return value.asText();
		else if (Date.class.equals(returnType))
			return Date.from(LocalDateTime.parse(value.asText(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")).atZone(ZoneId.systemDefault()).toInstant());
		else if (UUID.class.equals(returnType)) {
			String uuidValue = value.asText();
			if(uuidValue != null && !uuidValue.isEmpty())
				return UUID.fromString(uuidValue);
			
			return null;
		} else if (returnType.isInterface() && returnType.isAnnotationPresent(View.class)) {
			if (value.isObject()) {
				return map(returnType, (ObjectNode) value);
			} else if (value.isPojo()) {
				return ((POJONode) value).getPojo();
			}
		} else if (returnType.isInterface() && returnType.isAssignableFrom(List.class) 
				&& method != null && method.isAnnotationPresent(CollectionType.class) && value.isArray()) {
			List<Object> values = new ArrayList<>();
			
			Class<?> collectionType = method.getAnnotation(CollectionType.class).value();
			for(JsonNode listNode : ((ArrayNode)value)) {
				values.add(getJsonValue(collectionType, listNode, null));
			}
			
			return values;
		} else if (returnType.isEnum()) {
			String strValue = value.asText();
			for(Object enumConstant : returnType.getEnumConstants()) {
				if(enumConstant.toString().equals(strValue))
					return enumConstant;
			}
			return null;
		}
		
		throw new RuntimeException("Unknown return type: "+returnType);
	}

	private static Object getProperty(ObjectNode data, String property, Method method) {
		Class<?> returnType = method.getReturnType();
		JsonNode value = data.get(property);

		return getJsonValue(returnType, value, method);
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.Module#getModuleName()
	 */
	@Override
	public String getModuleName() {
		return "ViewModule";
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.Module#version()
	 */
	@Override
	public Version version() {
		return new Version(1, 0, 0, null, "org.ajdeveloppement", "org.ajdeveloppement.webserver.viewbinder.jackson.ViewModule");
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.Module#setupModule(com.fasterxml.jackson.databind.Module.SetupContext)
	 */
	@Override
	public void setupModule(SetupContext context) {
		context.addDeserializers(new Deserializers.Base() {
			public @Override JsonDeserializer<?> findBeanDeserializer(final JavaType type, DeserializationConfig config, BeanDescription beanDesc) throws JsonMappingException {
				if (type.getRawClass().isAnnotationPresent(View.class)) {
					return new JsonDeserializer<Object>() {
						public @Override Object deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
							Iterator<ObjectNode> obj = jp.readValuesAs(ObjectNode.class);
							return map(type.getRawClass(), obj.next());
						}
					};
				}

				return super.findBeanDeserializer(type, config, beanDesc);
			}
		});
	}
}
