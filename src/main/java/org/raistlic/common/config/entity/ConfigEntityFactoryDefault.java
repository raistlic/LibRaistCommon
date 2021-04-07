/*
 * Copyright 2015 Lei CHEN (raistlic@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.raistlic.common.config.entity;

import org.raistlic.common.assertion.AssertionFactory;
import org.raistlic.common.assertion.Assertions;
import org.raistlic.common.codec.Deserializer;
import org.raistlic.common.codec.Deserializers;
import org.raistlic.common.codec.ValueConversionException;
import org.raistlic.common.config.exception.ConfigEntityCreationException;
import org.raistlic.common.config.exception.ConfigValueConvertException;
import org.raistlic.common.config.source.ConfigSource;
import org.raistlic.common.precondition.InvalidParameterException;
import org.raistlic.common.precondition.Precondition;
import org.raistlic.common.reflection.ClassHelper;
import org.raistlic.common.reflection.ReflectionPredicates;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * The default implementation of {@link ConfigEntityFactory} interface.
 *
 * @author Lei CHEN (2015-12-21)
 */
class ConfigEntityFactoryDefault implements ConfigEntityFactory {

  private final Map<Class<?>, Deserializer<?>> deserializers;

  ConfigEntityFactoryDefault() {

    deserializers = new ConcurrentHashMap<>();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E> E createConfigEntity(Class<E> configEntityType, ConfigSource configSource, String path) {

    Precondition.param(configSource).isNotNull();
    Precondition.param(configEntityType).isNotNull();

    path = (path == null) ? "" : path.trim();
    ClassHelper<E> classHelper = ClassHelper.of(configEntityType);
    try {
      E entity = doCreateConfigEntity(classHelper, configSource, path);
      injectConfigProperties(classHelper, configSource, entity, path);
      return entity;
    } catch (Exception ex) {
      if (ex instanceof ConfigEntityCreationException) {
        throw (ConfigEntityCreationException) ex;
      } else {
        throw new ConfigEntityCreationException(ex);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <E> E doCreateConfigEntity(ClassHelper<E> classHelper, ConfigSource configSource, String path)
    throws InstantiationException, IllegalAccessException, InvocationTargetException {

    Method factoryMethod = getConfigConstructorFactoryMethod(classHelper);
    if (factoryMethod != null) {
      return (E) createConfigEntity(configSource, factoryMethod, path);
    }

    Constructor<E> constructor = getConfigConstructor(classHelper);
    if (constructor != null) {
      return createConfigEntity(configSource, constructor, path);
    }

    factoryMethod = getUsableFactoryMethod(classHelper);
    if (factoryMethod != null) {
      return (E) createConfigEntity(configSource, factoryMethod, path);
    }

    constructor = getUsableConstructor(classHelper);
    if (constructor != null) {
      return createConfigEntity(configSource, constructor, path);
    }

    throw new ConfigEntityCreationException(
      "Cannot find usable factory method or constructor: '" + classHelper.getTargetClass().getName() + "'");
  }

  @Override
  public Set<String> getConfigKeys(Class<?> configEntityType) {

    Set<String> buffer = new HashSet<>();
    gatherConfigKeys(configEntityType, "", buffer);
    return buffer;
  }

  private void gatherConfigKeys(Class<?> configEntityType, String path, Set<String> buffer) {

    Deserializer<?> deserializer = getDeserializer(configEntityType);
    if (deserializer != null) {
      if (path.isEmpty()) {
        throw new InvalidParameterException("Not a valid config entity type: " + configEntityType.getName());
      }
      buffer.add(path);
      return;
    }

    final String prefix = (path == null) ? "" : path.trim();
    ClassHelper<?> classHelper = ClassHelper.of(configEntityType);

    classHelper.getAllFieldsAsStream()
      .filter(PREDICATE_ANNOTATED_WITH_CONFIG_PROPERTY)
      .forEach(field -> {
        ConfigProperty configProperty = field.getAnnotation(ConfigProperty.class);
        String propertyName = configProperty.value();
        if (propertyName.isEmpty()) {
          propertyName = path;
        } else {
          propertyName = (prefix.isEmpty() ? "" : prefix + ".") + propertyName;
        }
        gatherConfigKeys(field.getType(), propertyName, buffer);
      });
    classHelper.getAllMethodsAsStream()
      .staticOnes()
      .hasReturnType(configEntityType)
      .hasAllParametersMatch(PREDICATE_ANNOTATED_WITH_CONFIG_PROPERTY)
      .flatMap(method -> Arrays.asList(method.getParameters()).stream())
      .forEach(parameter -> {
        ConfigProperty configProperty = parameter.getAnnotation(ConfigProperty.class);
        String propertyName = configProperty.value();
        if (propertyName.isEmpty()) {
          propertyName = path;
        } else {
          propertyName = (prefix.isEmpty() ? "" : prefix + ".") + propertyName;
        }
        gatherConfigKeys(parameter.getType(), propertyName, buffer);
      });
    classHelper.getConstructorsAsStream()
      .hasAllParametersMatch(PREDICATE_ANNOTATED_WITH_CONFIG_PROPERTY)
      .flatMap(constructor -> Arrays.asList(constructor.getParameters()).stream())
      .forEach(parameter -> {
        ConfigProperty configProperty = parameter.getAnnotation(ConfigProperty.class);
        String propertyName = configProperty.value();
        if (propertyName.isEmpty()) {
          propertyName = path;
        } else {
          propertyName = (prefix.isEmpty() ? "" : prefix + ".") + propertyName;
        }
        gatherConfigKeys(parameter.getType(), propertyName, buffer);
      });
  }

  @Override
  public <E> void registerDeserializer(Class<E> type, Deserializer<E> deserializer) {

    Precondition.param(deserializer).isNotNull();
    Precondition.param(type).isNotNull()
      .matches(
        VALID_DESERIALIZE_CUSTOMIZABLE_TYPE_PREDICATE,
        "The de-serialize logic for type '" + type.getName() + "' cannot be customized."
      );

    deserializers.put(type, deserializer);
  }

  private Deserializer<?> getDeserializer(Class<?> type) {

    Deserializer<?> deserializer = FIXED_DESERIALIZERS.get(type);
    if (deserializer == null) {
      deserializer = deserializers.get(type);
    }
    return deserializer;
  }

  private Object createConfigEntity(
    ConfigSource configSource,
    Method factoryMethod,
    String path) throws IllegalAccessException, InvocationTargetException {

    Parameter[] parameters = factoryMethod.getParameters();
    if (parameters.length == 0) {
      factoryMethod.setAccessible(true);
      return factoryMethod.invoke(null);
    } else {
      Object[] configValues = prepareParameterValues(configSource, parameters, path);
      factoryMethod.setAccessible(true);
      return factoryMethod.invoke(null, configValues);
    }
  }

  private <E> E createConfigEntity(
    ConfigSource configSource,
    Constructor<E> constructor,
    String path) throws InstantiationException, IllegalAccessException, InvocationTargetException {

    if (constructor.getParameterCount() == 0) {
      constructor.setAccessible(true);
      return constructor.newInstance();
    } else {
      Parameter[] parameters = constructor.getParameters();
      Object[] configValues = prepareParameterValues(configSource, parameters, path);
      constructor.setAccessible(true);
      return constructor.newInstance(configValues);
    }
  }

  private Object[] prepareParameterValues(ConfigSource configSource,
                                          Parameter[] parameters,
                                          String path) {

    Object[] properties = new Object[parameters.length];
    for (int i = 0, len = parameters.length; i < len; i++) {

      Parameter parameter = parameters[i];
      ConfigProperty configProperty = parameter.getAnnotation(ConfigProperty.class);
      String configPropertyName = getConfigPropertyName(path, configProperty, "");
      Class<?> parameterType = parameter.getType();
      properties[i] = getConfigValue(configSource, configPropertyName, parameterType);
    }
    return properties;
  }

  private <E> void injectConfigProperties(ClassHelper<E> classHelper,
                                          ConfigSource configSource,
                                          E entity,
                                          String path) throws Exception {

    classHelper.getAllFieldsAsStream()
      .noneStaticOnes()
      .annotatedWith(ConfigProperty.class)
      .forEach(field -> {
        ConfigProperty configProperty = field.getAnnotation(ConfigProperty.class);
        String configPropertyName = getConfigPropertyName(path, configProperty, field.getName());
        Object value = getConfigValue(configSource, configPropertyName, field.getType());
        field.setAccessible(true);
        try {
          field.set(entity, value);
        } catch (Exception ex) {
          throw new ConfigEntityCreationException(ex);
        }
      });
  }

  private Object getConfigValue(ConfigSource configSource, String key, Class<?> type) {

    VALIDATOR.expect(key).isNotNull("ConfigProperty annotation value for property type '" + type.getName() + "' is null.")
      .isNotEmpty("ConfigProperty annotation value for property type '" + type.getName() + "' is empty.");

    Deserializer<?> deserializer = getDeserializer(type);
    if (deserializer == null) {
      return createConfigEntity(type, configSource, key);
    }

    String value = configSource.getString(key);
    try {
      return (value == null) ? null : deserializer.decode(value);
    } catch (ValueConversionException ex) {
      throw new ConfigEntityCreationException(ex);
    }
  }

  private static String getConfigPropertyName(String prefix,
                                              ConfigProperty configProperty,
                                              String fallbackName) {

    String configName = configProperty.value();
    if (configName.isEmpty()) {
      VALIDATOR.expect(fallbackName).isNotNull("Some of the config properties missing property name.");
      configName = fallbackName;
    }
    if (!prefix.isEmpty()) {
      configName = prefix + CONFIG_PROPERTY_DILIMETER + configName;
    }
    return configName;
  }

  private static Method getConfigConstructorFactoryMethod(ClassHelper<?> classHelper) {

    return classHelper.getDeclaredMethodsAsStream()
      .staticOnes()
      .hasReturnType(classHelper.getTargetClass())
      .annotatedWith(ConfigConstructor.class)
      .findFirst()
      .orElse(null);
  }

  @SuppressWarnings("unchecked")
  private static <E> Constructor<E> getConfigConstructor(ClassHelper<E> classHelper) {

    return classHelper.getConstructorsAsStream()
      .annotatedWith(ConfigConstructor.class)
      .findFirst()
      .orElse(null);
  }

  private static Method getUsableFactoryMethod(ClassHelper<?> classHelper) {

    return classHelper.getDeclaredMethodsAsStream()
      .staticOnes()
      .hasReturnType(classHelper.getTargetClass())
      .hasAllParametersMatch(ReflectionPredicates.elementAnnotatedWith(ConfigProperty.class))
      .findFirst()
      .orElse(null);
  }

  @SuppressWarnings("unchecked")
  private static <E> Constructor<E> getUsableConstructor(ClassHelper<E> classHelper) {

    return classHelper.getConstructorsAsStream()
      .hasAllParametersMatch(ReflectionPredicates.elementAnnotatedWith(ConfigProperty.class))
      .findFirst()
      .orElse(null);
  }

  private static Map<Class<?>, Deserializer<?>> initFixedDeserializers() {

    Map<Class<?>, Deserializer<?>> map = new HashMap<>();
    map.put(String.class, Deserializers.getStringDeserializer());
    map.put(Boolean.class, Deserializers.getBooleanDeserializer());
    map.put(boolean.class, Deserializers.getBooleanDeserializer());
    map.put(Byte.class, Deserializers.getByteDeserializer());
    map.put(byte.class, Deserializers.getByteDeserializer());
    map.put(Character.class, Deserializers.getCharacterDeserializer());
    map.put(char.class, Deserializers.getCharacterDeserializer());
    map.put(Short.class, Deserializers.getShortDeserializer());
    map.put(short.class, Deserializers.getShortDeserializer());
    map.put(Integer.class, Deserializers.getIntegerDeserializer());
    map.put(int.class, Deserializers.getIntegerDeserializer());
    map.put(Long.class, Deserializers.getLongDeserializer());
    map.put(long.class, Deserializers.getLongDeserializer());
    map.put(Float.class, Deserializers.getFloatDeSerializer());
    map.put(float.class, Deserializers.getFloatDeSerializer());
    map.put(Double.class, Deserializers.getDoubleDeserializer());
    map.put(double.class, Deserializers.getDoubleDeserializer());
    map.put(BigInteger.class, Deserializers.getBigIntegerDeserializer());
    map.put(BigDecimal.class, Deserializers.getBigDecimalDeserializer());
    map.put(Integer.class, Deserializers.getStringDeserializer());
    return map;
  }

  private static final String CONFIG_PROPERTY_DILIMETER = ".";

  private static final Map<Class<?>, Deserializer<?>> FIXED_DESERIALIZERS
    = Collections.unmodifiableMap(initFixedDeserializers());

  private static final AssertionFactory VALIDATOR = Assertions.createDefaultExpectedCases(ConfigValueConvertException::new);

  private static final Predicate<Class<?>> VALID_DESERIALIZE_CUSTOMIZABLE_TYPE_PREDICATE =
    aClass -> !(aClass.isPrimitive() || FIXED_DESERIALIZERS.containsKey(aClass));

  private static final Predicate<AnnotatedElement> PREDICATE_ANNOTATED_WITH_CONFIG_PROPERTY =
    ReflectionPredicates.elementAnnotatedWith(ConfigProperty.class);
}
