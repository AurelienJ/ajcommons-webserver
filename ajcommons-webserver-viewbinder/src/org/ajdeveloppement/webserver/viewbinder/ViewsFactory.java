/**
 * 
 */
package org.ajdeveloppement.webserver.viewbinder;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ajdeveloppement.webserver.viewbinder.annotations.Collection;
import org.ajdeveloppement.webserver.viewbinder.annotations.CollectionType;
import org.ajdeveloppement.webserver.viewbinder.annotations.Implementation;
import org.ajdeveloppement.webserver.viewbinder.annotations.Reference;
import org.ajdeveloppement.webserver.viewbinder.annotations.View;

import com.google.common.base.Defaults;


/**
 * @author aurelien
 *
 */
public class ViewsFactory {
	private static Map<Class<?>, Map<Object, Object>> proxyCache  = new WeakHashMap<>();
	
	@SuppressWarnings("unchecked")
	public static <T> T getView(Class<T> viewType, Object data) {
		if(data != null)
			return getView(viewType, null, (Class<Object>)data.getClass(), data, viewType.getClassLoader());
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getView(Class<T> viewType, Object data, ClassLoader classLoader) {
		if(data != null)
			return getView(viewType, null, (Class<Object>)data.getClass(), data, classLoader);
		return null;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> T getView(Class<T> viewType, Class[] othersView, Object data, ClassLoader classLoader) {
		if(data != null)
			return getView(viewType, null, (Class<Object>)data.getClass(), data, classLoader);
		return null;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T,M> T getView(Class<T> viewType, Class[] othersView, Class<M> modelType, M data, ClassLoader classLoader) {
		
		if(data == null)
			return null;
		
		if(!proxyCache.containsKey(viewType))
			proxyCache.put(viewType, new WeakHashMap<>());
		
		Map<Object, Object> typeProxyCache = proxyCache.get(viewType);
		
		if(typeProxyCache.containsKey(data)) {
			WeakReference<T> refProxy = (WeakReference<T>)typeProxyCache.get(data);
			T proxy = refProxy.get();
			if(proxy != null)
				return proxy;
		}
		
		int nbViewInterface = othersView != null ? othersView.length + 1 : 1;
		Class[] viewsClass = new Class[nbViewInterface];
		viewsClass[0] = viewType;
		if(othersView != null && othersView.length > 0)
			System.arraycopy(othersView, 0, viewsClass, 1, othersView.length);
		
		T proxyInstance = (T)Proxy.newProxyInstance(classLoader,
				viewsClass,
				(Object proxy, Method method, Object[] args) -> {
					String methodName = method.getName();
					if(methodName.equals("toString")) {
						return "Proxy("+ modelType.getTypeName() + ")";
					}
					
					Class<?> mapperType = null;
					if(method.isAnnotationPresent(Implementation.class)) {
						Implementation implementation = method.getAnnotation(Implementation.class);
						methodName = implementation.methodModelToView();
						mapperType = implementation.mapperClass();
					}
				
					if(data != null) {
						if(mapperType == Void.class || mapperType == null) {
							Method implementationMethod = null;
							try {
								implementationMethod = data.getClass().getMethod(methodName, method.getParameterTypes());
							} catch (NoSuchMethodException e) {
								//ignore if no method return null
							}
							
							if(implementationMethod != null) {
								if(method.getReturnType().isAnnotationPresent(View.class)) {
									Object subElement = implementationMethod.invoke(data, args);
									return getView(method.getReturnType(), null, (Class<Object>)implementationMethod.getReturnType(), subElement, classLoader);
								}
								
								if(method.isAnnotationPresent(CollectionType.class)) {
									if(List.class.isAssignableFrom(method.getReturnType())) {
										Iterable<Object> listItems = (Iterable<Object>)implementationMethod.invoke(data, args);
										List<Object> viewItems = new ArrayList<>();
										if(listItems != null) {
											for(Object item : listItems) {
												if(item != null) {
													viewItems.add(getView(method.getAnnotation(CollectionType.class).value(), item, classLoader));
												}
											}
										}
										return viewItems;
									}
								}
								
								return implementationMethod.invoke(data, args);
							}
						} else {
							Class[] parametersType = new Class[method.getParameterCount()+1];
							parametersType[0] = modelType;
							if(method.getParameterCount() > 0)
								System.arraycopy(method.getParameterTypes(), 0, parametersType, 1, method.getParameterCount());
							
							Object[] parametersValues = new Object[parametersType.length];
							parametersValues[0] = data;
							if(args != null && args.length > 0)
								System.arraycopy(args, 0, parametersValues, 1, args.length);
							
							Method implementationMethod = mapperType.getMethod(methodName, parametersType);
							if(implementationMethod != null)
								return implementationMethod.invoke(null, parametersValues);
						}
					}
					
					if(method.getReturnType().isPrimitive())
						return Defaults.defaultValue(method.getReturnType());
					
					return null;
				});
		typeProxyCache.put(data, new WeakReference<T>(proxyInstance));
		
		return proxyInstance;
	}

	public static Description getDescription(Class<?> viewType) {
		if(!viewType.isAnnotationPresent(View.class))
			throw new IllegalArgumentException("viewType must be annotated with @View");
		
		View view = viewType.getAnnotation(View.class);
		
		Description description = new Description();
		//description.setKey(view.restKey());
		
		Stream<Method> methods = Arrays.asList(viewType.getMethods()).stream();
		for(Method ref : methods.filter(m -> m.isAnnotationPresent(Reference.class)).collect(Collectors.toList())) {
			Reference reference = ref.getAnnotation(Reference.class);
			View viewChild = reference.value().getAnnotation(View.class);
			if(viewChild != null) {
				//description.getReferences().add(new Description.Reference(reference.name(), viewChild.restKey()));
			}
		}
		
		for(Collection collection : viewType.getAnnotationsByType(Collection.class)) {
			View viewChild = collection.value().getAnnotation(View.class);
			if(viewChild != null) {
				//description.getCollections().add(new Description.Collection(collection.name(), viewChild.restKey()));
			}
		}
		
		return description;
	}
}
