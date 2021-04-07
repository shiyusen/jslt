
package com.schibsted.spt.data.json;

import java.util.List;
import java.util.Iterator;
import java.io.File;
import java.io.Reader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schibsted.spt.data.jslt.JsltException;

public class JsonIO {
  private static ObjectMapper mapper = new ObjectMapper();

  public static String toString(JsonValue value) {
    return toStringWithSortedKeys(value);
  }

  public static String toStringWithSortedKeys(JsonValue value) {
    // FIXME: no actual sorting of keys
    StringWriter buf = new StringWriter();
    value.traverse(new SerializingJsonHandler(buf));
    return buf.toString();
  }

  public static String prettyPrint(JsonValue value) {
    return toString(value);
  }

  // in UTF-8
  public static byte[] toBytes(JsonValue value) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
      value.traverse(new SerializingJsonHandler(osw));
      osw.flush();
      return baos.toByteArray();
    } catch (IOException e) {
      throw new JsltException("impossible error", e);
    }
  }

  // makes a JSON array of the list
  public static byte[] toBytes(List<JsonValue> values) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
      osw.write('[');

      for (int ix = 0; ix < values.size(); ix++) {
        values.get(ix).traverse(new SerializingJsonHandler(osw));
        if (ix+1 < values.size())
          osw.write(',');
      }

      osw.write(']');
      osw.flush();
      return baos.toByteArray();
    } catch (IOException e) {
      throw new JsltException("impossible error", e);
    }
  }

  public static JsonValue parseString(String json) {
    try {
      JsonBuilderHandler builder = new JsonBuilderHandler();
      JsonParser parser = new JsonParser();
      parser.parse(json, builder);
      return builder.get();
    } catch (IOException e) {
      throw new JsltException("impossible", e);
    }
  }

  public static JsonValue parse(Reader in) throws IOException {
    return new JacksonJsonValue(mapper.readTree(in));
  }

  public static JsonValue parse(File file) throws IOException {
    try (InputStreamReader in = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
      return parse(in);
    }
  }

  public static JsonValue parse(byte[] data) throws IOException {
    try (InputStreamReader in = new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8)) {
      return parse(in);
    }
  }

  /**
   * Parse JSON lines (newline-delimited JSON) format.
   */
  public static Iterator<JsonValue> parseLines(Reader in) throws IOException {
    return new JsonLinesIterator(in);
  }

  private static final class JsonLinesIterator implements Iterator<JsonValue> {
    private JsonParser parser;
    private BufferedReader reader;
    private String nextLine;

    public JsonLinesIterator(Reader in) throws IOException {
      this.parser = new JsonParser();
      this.reader = new BufferedReader(in);
      this.nextLine = reader.readLine();
    }

    public boolean hasNext() {
      return nextLine != null;
    }

    public JsonValue next() {
      try {
        JsonValue value = parser.parse(nextLine);
        nextLine = reader.readLine();
        return value;
      } catch (IOException e) {
        throw new JsltException("IO error", e);
      }
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}