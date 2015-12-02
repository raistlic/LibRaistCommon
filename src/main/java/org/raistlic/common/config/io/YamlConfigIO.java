package org.raistlic.common.config.io;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;
import org.raistlic.common.config.core.Config;
import org.raistlic.common.config.core.ConfigBuilder;
import org.raistlic.common.config.core.ConfigFactory;
import org.raistlic.common.config.exception.ConfigIOException;
import org.raistlic.common.config.source.ConfigSource;
import org.raistlic.common.precondition.InvalidParameterException;
import org.raistlic.common.precondition.Precondition;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

/**
 * @author Lei Chen (2015-10-12)
 */
enum YamlConfigIO implements ConfigIO {

  INSTANCE;

  @Override
  public void writeConfig(ConfigSource config, OutputStream outputStream)
      throws InvalidParameterException, ConfigIOException {

    Precondition.param(config, "config").notNull();
    Precondition.param(outputStream, "outputStream").notNull();

    try {
      Map<String, Object> map = NestedMapHelper.configToMap(config);
      OutputStreamWriter writer = new OutputStreamWriter(outputStream);
      YamlWriter yamlWriter = new YamlWriter(writer);
      yamlWriter.write(map);
      yamlWriter.close();
    }
    catch (Exception ex) {
      throw new ConfigIOException(ex);
    }
  }

  @Override
  public Config readConfig(InputStream inputStream) throws ConfigIOException {

    Precondition.param(inputStream, "inputStream").notNull();

    ConfigBuilder configBuilder = ConfigFactory.newMutableConfig();
    readConfig(configBuilder, inputStream);
    return configBuilder.build();
  }

  @Override
  public void readConfig(ConfigBuilder configBuilder, InputStream inputStream) throws ConfigIOException {

    Precondition.param(configBuilder, "configBuilder").notNull();
    Precondition.param(inputStream, "inputStream").notNull();

    try {
      InputStreamReader reader = new InputStreamReader(inputStream);
      YamlReader yamlReader = new YamlReader(reader);
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) yamlReader.read();
      NestedMapHelper.mapToConfig(map, "", configBuilder);
    }
    catch (Exception ex) {
      throw new ConfigIOException(ex);
    }
  }
}
