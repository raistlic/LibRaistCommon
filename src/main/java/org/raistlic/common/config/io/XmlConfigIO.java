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

package org.raistlic.common.config.io;

import org.raistlic.common.config.core.Config;
import org.raistlic.common.config.core.ConfigBuilder;
import org.raistlic.common.config.core.ConfigFactory;
import org.raistlic.common.config.exception.ConfigIOException;
import org.raistlic.common.config.source.ConfigSource;
import org.raistlic.common.precondition.InvalidParameterException;
import org.raistlic.common.precondition.Precondition;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Lei Chen (2015-10-12)
 */
enum XmlConfigIO implements ConfigIO {

  INSTANCE;

  @Override
  public void writeConfig(ConfigSource config, OutputStream outputStream)
    throws InvalidParameterException, ConfigIOException {

    Precondition.param(config).isNotNull();
    Precondition.param(outputStream).isNotNull();

    Configuration configuration = new Configuration();
    for (String key : config.getKeys()) {
      String value = config.getString(key);
      Entry entry = new Entry();
      entry.setKey(key);
      entry.setValue(value);
      configuration.getEntries().add(entry);
    }
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(Configuration.class);
      Marshaller marshaller = jaxbContext.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      // use PrintStream instead of Marshaller, because Marshaller seems to handle
      // new line characters in the wrong way (not cross OS proof), which causes
      // tests to fail on Windows.
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      marshaller.marshal(configuration, buffer);
      PrintStream ps = new PrintStream(outputStream);
      for (String line : new String(buffer.toByteArray()).split("\n")) {
        ps.println(line);
      }
    } catch (Exception ex) {
      throw new ConfigIOException(ex);
    }
  }

  @Override
  public Config readConfig(InputStream inputStream) throws ConfigIOException {

    Precondition.param(inputStream).isNotNull();

    ConfigBuilder configBuilder = ConfigFactory.newMutableConfig();
    readConfig(configBuilder, inputStream);
    return configBuilder.get();
  }

  @Override
  public void readConfig(ConfigBuilder configBuilder, InputStream inputStream) throws ConfigIOException {

    Precondition.param(configBuilder).isNotNull();
    Precondition.param(inputStream).isNotNull();

    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(Configuration.class);
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      @SuppressWarnings("unchecked")
      Configuration configuration = (Configuration) unmarshaller.unmarshal(inputStream);
      for (Entry entry : configuration.getEntries()) {
        configBuilder.setString(entry.getKey(), entry.getValue());
      }
    } catch (Exception ex) {
      throw new ConfigIOException(ex);
    }
  }

  @XmlRootElement(name = "configuration")
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Configuration {

    @XmlElement(name = "entry")
    @XmlElementWrapper(name = "entries")
    private List<Entry> entries = new ArrayList<Entry>();

    public List<Entry> getEntries() {

      return entries;
    }

    public void setEntries(List<Entry> entries) {

      this.entries = entries;
    }
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Entry {

    @XmlElement
    private String key;

    @XmlElement
    private String value;

    public String getKey() {

      return key;
    }

    public void setKey(String key) {

      this.key = key;
    }

    public String getValue() {

      return value;
    }

    public void setValue(String value) {

      this.value = value;
    }
  }
}
