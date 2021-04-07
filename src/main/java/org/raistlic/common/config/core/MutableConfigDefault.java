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

package org.raistlic.common.config.core;

import org.raistlic.common.codec.Encoder;
import org.raistlic.common.codec.ValueConversionException;
import org.raistlic.common.config.exception.ConfigValueConvertException;
import org.raistlic.common.config.source.ConfigSource;
import org.raistlic.common.config.source.ConfigSourceFactory;

import java.util.*;

import static org.raistlic.common.precondition.Precondition.param;

/**
 * @author Lei Chen (2015-09-11)
 */
class MutableConfigDefault extends AbstractConfig implements MutableConfig {

  private final Map<String, String> map;

  private final Set<String> keys;

  MutableConfigDefault() {

    map = new HashMap<String, String>();
    keys = Collections.unmodifiableSet(map.keySet());
  }

  private MutableConfig doImportFrom(ConfigSource configSource) {

    for (String key : configSource.getKeys()) {
      doSetString(key, configSource.getString(key));
    }
    return this;
  }

  @Override
  public MutableConfig importFrom(ConfigSource configSource) {

    param(configSource).isNotNull();
    return doImportFrom(configSource);
  }

  @Override
  public MutableConfig importFrom(Map<String, String> map) {

    param(map).isNotNull();

    ConfigSource wrappedSource = ConfigSourceFactory.wrap(map);
    return doImportFrom(wrappedSource);
  }

  @Override
  public MutableConfig importFrom(Properties properties) {

    param(properties).isNotNull();

    ConfigSource wrappedSource = ConfigSourceFactory.wrap(properties);
    return doImportFrom(wrappedSource);
  }

  private void doSetString(String key, String value) {

    map.put(key, value);
  }

  @Override
  public MutableConfig setString(String key, String value) {

    param(key).isNotNull();

    doSetString(key, value);
    return this;
  }

  @Override
  public MutableConfig setBoolean(String key, boolean value) {

    param(key).isNotNull();

    doSetString(key, String.valueOf(value));
    return this;
  }

  @Override
  public MutableConfig setByte(String key, byte value) {

    param(key).isNotNull();

    doSetString(key, String.valueOf(value));
    return this;
  }

  @Override
  public MutableConfig setChar(String key, char value) {

    param(key).isNotNull();

    doSetString(key, String.valueOf(value));
    return this;
  }

  @Override
  public MutableConfig setShort(String key, short value) {

    param(key).isNotNull();

    doSetString(key, String.valueOf(value));
    return this;
  }

  @Override
  public MutableConfig setInt(String key, int value) {

    param(key).isNotNull();

    doSetString(key, String.valueOf(value));
    return this;
  }

  @Override
  public MutableConfig setLong(String key, long value) {

    param(key).isNotNull();

    doSetString(key, String.valueOf(value));
    return this;
  }

  @Override
  public MutableConfig setFloat(String key, float value) {

    param(key).isNotNull();

    doSetString(key, String.valueOf(value));
    return this;
  }

  @Override
  public MutableConfig setDouble(String key, double value) {

    param(key).isNotNull();

    doSetString(key, String.valueOf(value));
    return this;
  }

  @Override
  public <E> MutableConfig setValue(String key, E value, Encoder<? super E, String> encoder) {

    param(key).isNotNull();
    param(encoder).isNotNull();

    try {
      String converted = encoder.encode(value);
      doSetString(key, converted);
      return this;
    } catch (ValueConversionException ex) {
      throw new ConfigValueConvertException(ex);
    }
  }

  @Override
  public Set<String> getKeys() {

    return keys;
  }

  @Override
  public boolean hasKey(String key) {

    param(key).isNotNull();
    return map.containsKey(key);
  }

  @Override
  public String getString(String key) {

    param(key).isNotNull();
    return map.get(key);
  }

  @Override
  public void applyConfig(Config configuration) {

    importFrom(configuration);
  }

  @Override
  public Config get() {

    ConfigSource configSource = ConfigSourceFactory.immutableCopyOf(this);
    return ConfigFactory.wrap(configSource);
  }
}
