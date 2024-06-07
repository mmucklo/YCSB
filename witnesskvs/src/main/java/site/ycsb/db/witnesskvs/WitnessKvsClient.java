/**
 * Copyright (c) 2017 YCSB contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */
package site.ycsb.db.witnesskvs;

import site.ycsb.ByteIterator;
import site.ycsb.DB;
import site.ycsb.DBException;
import site.ycsb.Status;
import site.ycsb.StringByteIterator;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

/**
 * YCSB Client for Google's Cloud Spanner.
 */
public class WitnessKvsClient extends DB {
  protected static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * The names of properties which can be specified in the config files and flags.
   */
  public static final class WitnessKvsProperties {
    private WitnessKvsProperties() {}

    /**
     * The WitnessKvs nodes to use when running the YCSB benchmark.
     */
    static final String NODES = "witnesskvs.nodes";
    /**
     * Number of WitnessKvs client channels to use. It's recommended to leave this to be the default value.
     */
    static final String NUM_CHANNELS = "witnesskvs.channels";
  }

  private static final Logger LOGGER = Logger.getLogger(WitnessKvsClient.class.getName());

  // Static lock for the class.
  private static final Object CLASS_LOCK = new Object();

  // Single client per process.
  private static edu.stanford.witnesskvs.client.Client client = null;

  @Override
  public void init() throws DBException {
    synchronized (CLASS_LOCK) {
      if (client != null) {
        return;
      }
      Properties properties = getProperties();
      String nodes = properties.getProperty(WitnessKvsProperties.NODES);
      String[] hostports = nodes.split(",");

      client = new edu.stanford.witnesskvs.client.Client(hostports, hostports[0]);

      LOGGER.log(Level.INFO, new StringBuilder()
          .append("\nNodes: ").append(nodes)
          .toString());
    }
  }

  protected static String toJson(Map<String, ByteIterator> values)
      throws IOException {
    ObjectNode node = MAPPER.createObjectNode();
    Map<String, String> stringMap = StringByteIterator.getStringMap(values);
    for (Map.Entry<String, String> pair : stringMap.entrySet()) {
      node.put(pair.getKey(), pair.getValue());
    }
    JsonFactory jsonFactory = new JsonFactory();
    Writer writer = new StringWriter();
    JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(writer);
    MAPPER.writeTree(jsonGenerator, node);
    return writer.toString();
  }

  protected static void fromJson(
      String value, Set<String> fields,
      Map<String, ByteIterator> result) throws IOException {
    JsonNode json = MAPPER.readTree(value);
    boolean checkFields = fields != null && !fields.isEmpty();
    for (Iterator<Map.Entry<String, JsonNode>> jsonFields = json.getFields();
         jsonFields.hasNext();
         /* increment in loop body */) {
      Map.Entry<String, JsonNode> jsonField = jsonFields.next();
      String name = jsonField.getKey();
      if (checkFields && !fields.contains(name)) {
        continue;
      }
      JsonNode jsonValue = jsonField.getValue();
      if (jsonValue != null && !jsonValue.isNull()) {
        result.put(name, new StringByteIterator(jsonValue.asText()));
      }
    }
  }

  public String getKey(String table, String key) {
    return table + "-+-+-" + key;
  }

  @Override
  public Status read(
      String table, String key, Set<String> fields, Map<String, ByteIterator> result) {
    String k = getKey(table, key);
    String r = client.get(k);
    if (r == null) {
      return Status.NOT_FOUND;
    }
    try {
      fromJson(r, fields, result);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Error encountered for key: " + key, e);
      return Status.ERROR;
    }
    return Status.OK;
  }

  @Override
  public Status scan(
      String table, String startKey, int recordCount, Set<String> fields,
      Vector<HashMap<String, ByteIterator>> result) {
    return Status.NOT_IMPLEMENTED;
  }

  @Override
  public Status update(String table, String key, Map<String, ByteIterator> values) {
    String k = getKey(table, key);

    try {
      if (client.put(k, toJson(values))) {
        return Status.OK;
      }
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Error encountered for key: " + key, e);      
    }
    return Status.ERROR;
  }

  @Override
  public Status insert(String table, String key, Map<String, ByteIterator> values) {
    String k = getKey(table, key);
    try {
      if (client.put(k, toJson(values))) {
        return Status.OK;
      }  
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Error encountered for key: " + key, e);      
    }
    return Status.ERROR;
  }

  @Override
  public void cleanup() {
  }

  @Override
  public Status delete(String table, String key) {
    String k = getKey(table, key);
    if (client.delete(k)) {
      return Status.OK;
    }
    return Status.ERROR;
  }
}
