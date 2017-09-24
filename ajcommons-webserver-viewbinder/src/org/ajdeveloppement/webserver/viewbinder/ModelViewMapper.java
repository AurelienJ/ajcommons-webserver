package org.ajdeveloppement.webserver.viewbinder;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.ajdeveloppement.webserver.viewbinder.annotations.CollectionType;
import org.ajdeveloppement.webserver.viewbinder.annotations.Implementation;
import org.ajdeveloppement.webserver.viewbinder.annotations.View;

public class ModelViewMapper {

	/**
	 * A partir d'un nom d'une méthode getter ou setter, returne le nom de la propriété correspondante (sans le get/set/is)
	 * 
	 * @param methodName
	 * @return
	 */
	@SuppressWarnings("nls")
	private static String getPropertyName(String methodName) {
		if (methodName.startsWith("get") || methodName.startsWith("set") || methodName.startsWith("is")) {
			if (methodName.startsWith("is"))
				return methodName.substring(2, 3).toLowerCase() + methodName.substring(3);
			return methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
		}

		return methodName;
	}
	
	private static <Model, ModelView> void mapCollection(Method viewModelReadMethod, Method modelWriteMethod,
			ModelView modelView, Model model)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
			NoSuchMethodException, SecurityException {
		
		Class<?> viewReturnType = viewModelReadMethod.getReturnType();
		Class<?> modelSetType = modelWriteMethod.getParameterTypes()[0];
		Class<?> viewItemReturnType = null;
		Class<?> viewItemMapperType = null;
		Method viewItemToModelMapper = null;
		
		CollectionType viewItemType = viewModelReadMethod.getAnnotation(CollectionType.class);
		if(viewItemType != null) {
			viewItemReturnType = viewItemType.value();
			View viewAnnotation = viewItemReturnType.getAnnotation(View.class);
			if(viewAnnotation != null) {
				viewItemMapperType = viewAnnotation.defaultMapperClass();
				
				Method m = viewItemMapperType.getMethod("toModel", viewItemReturnType);
				
				if(Modifier.isStatic(m.getModifiers())) {
					viewItemToModelMapper = m;
				}
			}
		}
		
		List<Object> tempModelCollection = new ArrayList<>();
		if(Iterable.class.isAssignableFrom(viewReturnType)) {
			Iterable<Object> collectionItems = (Iterable<Object>)viewModelReadMethod.invoke(modelView);
			
			for(Object viewItem : collectionItems) {
				tempModelCollection.add(viewItemToModelMapper.invoke(null, viewItem));
			}
		} else if(viewReturnType.isArray()) {
			Object[] collectionItems = (Object[])viewModelReadMethod.invoke(modelView);
			
			for(Object viewItem : collectionItems) {
				tempModelCollection.add(viewItemToModelMapper.invoke(null, viewItem));
			}
		}
		
		if(modelSetType.isAssignableFrom(tempModelCollection.getClass())) {
			modelWriteMethod.invoke(model, tempModelCollection);
		} else if(modelSetType.isArray()){
			modelWriteMethod.invoke(model, tempModelCollection.toArray());
		}
	}

	@SuppressWarnings("nls")
	public static <Model, ModelView> void mapModelToViewModel(Model model, ModelView modelView)
			throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		if (modelView != null && model != null) {
			BeanInfo beanInfoModelView = Introspector.getBeanInfo(modelView.getClass());
			BeanInfo beanInfoModel = Introspector.getBeanInfo(model.getClass());

			List<PropertyDescriptor> propertyDescriptorsModel = Arrays.asList(beanInfoModel.getPropertyDescriptors());
			for (PropertyDescriptor pd : beanInfoModelView.getPropertyDescriptors()) {
				String propName = pd.getName();
				if (!propName.equals("class") && pd.getWriteMethod() != null) {
					Method viewModelWriteMethod = pd.getWriteMethod();
					if (viewModelWriteMethod != null) {
						Implementation bindedProperty = viewModelWriteMethod.getAnnotation(Implementation.class);
						if (bindedProperty != null && !bindedProperty.methodModelToView().isEmpty()) {
							String substituteMethod = bindedProperty.methodModelToView();
							propName = getPropertyName(substituteMethod);
						}

						String modelPropertyName = propName;

						if (bindedProperty == null || bindedProperty.mapperClass() == Void.class) {
							Optional<PropertyDescriptor> modelPropertyDescriptor = propertyDescriptorsModel.stream()
									.filter(pdm -> pdm.getName().equals(modelPropertyName)).findFirst();
							if (modelPropertyDescriptor.isPresent()) {

								Method modelReadMethod = modelPropertyDescriptor.get().getReadMethod();

								viewModelWriteMethod.invoke(modelView, modelReadMethod.invoke(model));
							}
						} else {
							Method implementationMethod = bindedProperty.mapperClass().getMethod(
									bindedProperty.methodModelToView(), new Class[] { modelView.getClass(), model.getClass() });
							if (implementationMethod != null)
								implementationMethod.invoke(null, new Object[] { modelView, model });
						}
					}
				}
			}
		}
	}

	public static <Model, ModelView> void mapModelViewToModel(ModelView modelView, Model model)
			throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException, InstantiationException {
		
		if (modelView != null && model != null) {
			BeanInfo beanInfoModelView = Introspector.getBeanInfo(modelView.getClass());
			BeanInfo beanInfoModel = Introspector.getBeanInfo(model.getClass());
			
			Class<?> defaultMapperType = null;
			View viewAnnotation = modelView.getClass().getAnnotation(View.class);
			if(viewAnnotation != null && viewAnnotation.defaultMapperClass() != Void.class)
				defaultMapperType = viewAnnotation.defaultMapperClass();
			
			List<PropertyDescriptor> propertyDescriptorsModel = Arrays.asList(beanInfoModel.getPropertyDescriptors());
			//On boucle sur les propriétés de la vue
			for (PropertyDescriptor pd : beanInfoModelView.getPropertyDescriptors()) {
				
				String propName = pd.getName();
				//On exploite les getters
				if (!propName.equals("class") && pd.getReadMethod() != null) { //$NON-NLS-1$
					
					Method viewModelReadMethod = pd.getReadMethod();

					//On recherche une annotation Implementation sur le getter
					Implementation bindedProperty = viewModelReadMethod.getAnnotation(Implementation.class);
					if (bindedProperty != null && !bindedProperty.methodViewToModel().isEmpty()) {
						String substituteMethod = bindedProperty.methodViewToModel();
						propName = getPropertyName(substituteMethod);
					}

					String modelPropertyName = propName;

					//Si on n'a pas d'implementation
					if (bindedProperty == null || bindedProperty.mapperClass() == Void.class) {
						//Si c'est une collection annoté fait le mapping des elements internes
						CollectionType collectionType = viewModelReadMethod.getAnnotation(CollectionType.class);
						if(collectionType != null) {
							
							View itemTypeView = collectionType.value().getAnnotation(View.class);
							Class<?> itemMapperType = itemTypeView.defaultMapperClass();
							
							List<Object> newCollection = new ArrayList<>();
							Iterable<?> viewCollection = (Iterable<?>)viewModelReadMethod.invoke(modelView);
							
							if(itemMapperType == Void.class) {
								for(Object viewItem : viewCollection) {
									Object modelItem = collectionType.value().newInstance();
									ModelViewMapper.mapModelViewToModel(viewItem, modelItem);
									newCollection.add(modelItem);
								}
							} else {
								for(Object viewItem : viewCollection) {
									Method mapperMethod = itemMapperType.getMethod("toModel", collectionType.value());
									Object modelItem = mapperMethod.invoke(null, viewItem);
									newCollection.add(modelItem);
								}
							}
						} else {
							//on recherche sur le modèle un setter avec le même nom de propriété
							Optional<PropertyDescriptor> modelPropertyDescriptor = propertyDescriptorsModel.stream()
									.filter(pdm -> pdm.getName().equals(modelPropertyName)).findFirst();
							if (modelPropertyDescriptor.isPresent()) {
								//si on trouve, on injecte la valeur du getter de la vue dans le setter du modele
								Method modelWriteMethod = modelPropertyDescriptor.get().getWriteMethod();
	
								//on contrôle la cohérence des type
								if (modelWriteMethod.getParameters()[0].getType() == viewModelReadMethod
										.getReturnType())
									modelWriteMethod.invoke(model, viewModelReadMethod.invoke(modelView));
								else
									throw new IllegalArgumentException("Probleme de type"
											+ modelWriteMethod.getParameters()[0].getType().toString() + " <> "
											+ viewModelReadMethod.getReturnType().toString());
							}
						}
					} else {
						//Si on a une implementation spécifié, invoque celle ci
						Class<?> mapperType = defaultMapperType;
						if(bindedProperty.mapperClass() != Void.class)
							mapperType = bindedProperty.mapperClass();
						Method implementationMethod = mapperType.getMethod(
								bindedProperty.methodViewToModel(), new Class[] { modelView.getClass(), model.getClass() });
						if (implementationMethod != null)
							implementationMethod.invoke(null, new Object[] { modelView, model });
					}
				}
			}
		}
	}

}
