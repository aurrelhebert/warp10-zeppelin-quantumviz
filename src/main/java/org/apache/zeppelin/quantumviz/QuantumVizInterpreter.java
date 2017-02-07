//
//   Copyright 2016  Cityzen Data
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//

package org.apache.zeppelin.quantumviz;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterPropertyBuilder;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.thrift.InterpreterCompletion;
import org.apache.zeppelin.resource.Resource;
import org.apache.zeppelin.resource.ResourcePool;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Interpreter QuantumViz for Zeppelin
 * 
 */
public class QuantumVizInterpreter extends Interpreter
{

  private String SETTING_STRING = "custom";
  private String SETTING_TYPE_GEO = "geo";
  private String SETTING_TYPE_GRAPH = "graph";
  private String SETTING_DEFAULT_MAX_HEIGHT = "600px";
  private String SETTING_DEFAULT_MAX_WIDTH = "800%";
  
  private String JSON_TYPE_KEY = "type";
  private String JSON_MAX_HEIGHT_KEY = "max-height";
  private String JSON_MAX_WIDTH_KEY = "max-width";
  private String JSON_DATA_KEY = "data";
  private String JSON_SERIES_KEY = "series";
  
  //
  // Private Pair class
  //

  private class Pair<F, S> {
    private F first;
    private S second;

    public Pair(F first, S second) {
      super();
      this.first = first;
      this.second = second;
    }

  }

  static final String URL_KEY = "warp10.url";
  String current_Url;

  private final HashMap<String, Properties> propertiesMap;

  public QuantumVizInterpreter(Properties property) {
    super(property);
    //
    propertiesMap = new HashMap<>();
    //property.
  }

  public HashMap<String, Properties> getPropertiesMap() {
    return propertiesMap;
  }

  public void cancel(InterpreterContext arg0) {
    //

  }

  public void close() {

  }

  public List<InterpreterCompletion> completion(String arg0, int arg1) {
    //
    return null;
  }

  public FormType getFormType() {
    //
    return FormType.SIMPLE;
  }

  public int getProgress(InterpreterContext arg0) {
    //
    return 0;
  }

  //
  // When the result of the WarpScript contains a Map on top of the stack
  // then all the tuples key,value are saved in Zeppelin resource pool.
  // To ensure a Map on top of the stack the WarpScript function EXPORT can be used
  // When using Angular to save variable NaN and Infinity are transformed in String !
  //
  public InterpreterResult interpret(String body, InterpreterContext context) {

    //
    // Store the resource pool already defined in context
    //

    ResourcePool resources = context.getResourcePool();
    
    JSONObject jsObject = null;
    try {
      jsObject = new JSONObject(body);
    } catch (JSONException e) {
      
      //throw a Zeppelin exception     
      return new InterpreterResult(InterpreterResult.Code.ERROR, 
          "Quantumviz interpreter expects a valid JSON as input");
    }
    //String bodyLines[] = body.split("\n");
    
    //
    // Verify that the JSON String has 
    //
    
    if (!jsObject.has(this.JSON_DATA_KEY)) {
      
      //throw a Zeppelin exception     
      return new InterpreterResult(InterpreterResult.Code.ERROR, 
          "Quantumviz interpreter expects a data key containing GeoTimeSeries. "
          + "Data can be a single JSON object or a list thereof.");
    }
    
    //
    // Set default attributes for each elements to visualize
    //
    
    String type = this.SETTING_TYPE_GRAPH;
    String maxHeight = this.SETTING_DEFAULT_MAX_HEIGHT;
    String maxWidth = this.SETTING_DEFAULT_MAX_WIDTH;
    
    //
    // Check if type element (graph or geo) is specified by the user.
    //
    
    if (jsObject.has(this.JSON_TYPE_KEY)) {
      
      //
      // Verify type input string before setting it up
      //
      
      if (jsObject.getString(this.JSON_TYPE_KEY).equals(this.SETTING_TYPE_GRAPH) 
          || jsObject.getString(this.JSON_TYPE_KEY).equals(this.SETTING_TYPE_GEO)) {
        type = jsObject.getString(this.JSON_TYPE_KEY);
      } else {
        
        //throw a Zeppelin exception     
        return new InterpreterResult(InterpreterResult.Code.ERROR, 
            "Quantumviz interpreter expects custom setting type to be "
            + "one of the following one [" 
            + this.SETTING_TYPE_GRAPH + ", " + this.SETTING_TYPE_GEO + "].");
      }
    }
    
    //
    // Check if div max height is set by the user
    //
    if (jsObject.has(this.JSON_MAX_HEIGHT_KEY)) {
      
      //
      // Verify height string before setting it up
      //
      
      if (isHeightOk(jsObject.getString(this.JSON_MAX_HEIGHT_KEY))) {
        maxHeight = jsObject.getString(this.JSON_MAX_HEIGHT_KEY);
      } else {
        
        //throw a Zeppelin exception
        return new InterpreterResult(InterpreterResult.Code.ERROR, 
            "Quantumviz interpreter expects custom setting height to be "
            + "a number followed by the string px.");
      }
    }
    
    //
    // Check if div max width is set by the user
    //
    if (jsObject.has(this.JSON_MAX_WIDTH_KEY)) {
      
      //
      // Verify width string before setting it up
      //
      
      if (isWidthOk(jsObject.getString(this.JSON_MAX_WIDTH_KEY))) {
        maxWidth = jsObject.getString(this.JSON_MAX_WIDTH_KEY);
      } else {
        
        //throw a Zeppelin exception
        return new InterpreterResult(InterpreterResult.Code.ERROR, 
            "Quantumviz interpreter expects custom setting height to be "
            + "a number followed by the string px.");
      }
    }
    
    //
    // Build Zeppelin output by importing polymer component from local url
    //
    
    StringBuilder res = new StringBuilder();
    res.append("\"\"\"%html ");
    res.append("<script> "
        + "if (!jsCode) {"
        + "var jsCode = document.createElement('script');"
        + "jsCode.setAttribute('src', '" + current_Url 
        + "/webcomponentsjs/webcomponents-lite.js'); "
        + "document.body.appendChild(jsCode); "
        + "} "
        + "</script> ");
    res.append("<link   rel=\"import\" href=\"" + current_Url 
      + "/polymer/polymer.html\"> ");
    
    //
    // In case it's a graph or a geo map, use appropriate component
    //

    String display = "";
    if (type.equals(this.SETTING_TYPE_GRAPH)) {
      res.append("<link   rel=\"import\" href=\"" + current_Url 
        + "/warp10-quantumviz/warp10-display-alt-chart.html\"> ");
      display = "warp10-display-chart";
    } else if (type.equals(this.SETTING_TYPE_GEO)) {
      res.append("<link   rel=\"import\" href=\"" + current_Url 
          + "/warp10-quantumviz/warp10-display-map.html\">");
      display = "warp10-display-map";
    }
    
    //
    // Check if data types match a single object or a list thereof
    //
    
    JSONArray bodyElements = new JSONArray();
    if (jsObject.get(this.JSON_DATA_KEY) instanceof JSONArray) {
      bodyElements = jsObject.getJSONArray(this.JSON_DATA_KEY);
    } else if (jsObject.get(this.JSON_DATA_KEY) instanceof JSONObject) {
      bodyElements.put(jsObject.get(this.JSON_DATA_KEY));
    } else {
      
    //throw a Zeppelin exception
      return new InterpreterResult(InterpreterResult.Code.ERROR, 
          "Quantumviz interpreter encouters an incorrect data type: "
          + "expects a single JSON object or list thereof.");
    }

    //
    // Append a result string for each different graph/geo map the user add
    //
    
    for (Object dataObject : bodyElements) {
      
      //
      // Verify it the current element is valid
      //
      
      if (!(dataObject instanceof JSONObject)) {
      //throw a Zeppelin exception
        return new InterpreterResult(InterpreterResult.Code.ERROR, 
            "Quantumviz interpreter encouters an incorrect data type: "
            + "each element must be a valid JSON object.");
      }
      JSONObject jsonElement = (JSONObject) dataObject;
      
      //
      // Check if there is a series key
      //
      
      if (!jsonElement.has(this.JSON_SERIES_KEY)) {
        
        //throw a Zeppelin exception
        return new InterpreterResult(InterpreterResult.Code.ERROR, 
            "Quantumviz interpreter encouters an incorrect data type: "
            + "each element needs a series key.");
      }
      
      //
      // Check if there is a series key
      //
      
      if (!(jsonElement.get(this.JSON_SERIES_KEY) instanceof String )) {
        
        //throw a Zeppelin exception
        return new InterpreterResult(InterpreterResult.Code.ERROR, 
            "Quantumviz interpreter encouters an incorrect series type: "
            + "series corresponds to a key string to load an element from "
            + "Zeppelin resource pool.");
      }
      
      String seriesKey = jsonElement.getString(this.JSON_SERIES_KEY);
      
      //
      // Create visualization div
      //
      
      res.append("<div>");
      
      //
      // Set up its style (height and width)
      //
      
      res.append("<" + display + " style=\"height:" + maxHeight + ";"
          + "max-width:" + maxWidth + ";\"");
      
      //
      // Load resources from Zeppelin if founded
      //
      StringBuilder currentGraphs = new StringBuilder();
      Resource resource = resources.get(seriesKey);
      if (resource != null) {
        Object value = resources.get(seriesKey).get();
        currentGraphs.append(parseObjectToString(value));
      } else {
        
        //throw a Zeppelin exception
        return new InterpreterResult(InterpreterResult.Code.ERROR, 
            "Quantumviz interpreter encouters an incorrect series type: "
            + "series not found in Zeppelin resource pool.");
      }
      
      //
      // Append data string and then close web component
      // 
      
      res.append("data='" + currentGraphs.toString() + "'");
      res.append(" </" + display + "> <p> </p>");
      res.append("</div>");
      //System.out.println(res.toString());
    }
    
    //res.append("\"\"\"");

    return new InterpreterResult(InterpreterResult.Code.SUCCESS, res.toString());

  }

  /**
   * Verify that a string is adapted for the polymer width setting
   * @param width string to test
   * @return true iif valid (numbers + (px || %)
   */
  private boolean isWidthOk(String width) {
    
    String numbers = "error";
    
    //
    // Separate case where string end with % or px
    //
    
    if (width.endsWith("%")) {
      numbers = width.substring(0, width.length() - 2);
    } else if (width.endsWith("px")) {
      numbers = width.substring(0, width.length() - 3);
    }
    
    //
    // If string still equals error or isn't numbers return false
    //
    
    if (numbers.equals("error")) {
      return false;
    } else {
      if (!NumberUtils.isNumber(numbers)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Verify that a string is adapted for the polymer height setting
   * @param height string to test
   * @return true iif valid (numbers + px)
   */
  private boolean isHeightOk(String height) {
    
    //
    // Check if end of the string is px
    //
    
    if (!height.endsWith("px")) {
      return false;
    }
    
    //
    // Then verify that the first part of the string is numbers
    //
    
    String numbers = height.substring(0, height.length() - 3);
    if (!NumberUtils.isNumber(numbers)) {
      return false;
    }
    
    return true;
  }

  private String parseObjectToString(Object object) {

    if ( object instanceof Number ) {
      return object.toString();
    } else if ( object instanceof String ) {
      return object.toString(); 
    } else if (object instanceof List) {
      JSONArray array = new JSONArray();

      for (Object element : (List) object) {
        array.put(element);
      }
      return array.toString();
    } else if (object instanceof Map) {
      JSONObject map = new JSONObject();
      Map mapObj = (Map) object;
      for (Object key : mapObj.keySet()) {
        map.put(key.toString(), mapObj.get(key));
      }
      return map.toString();
    }
    return "";
  }

  private Object parseObject(Object object) {
    if (isListJSONValid(object.toString())) {
      ArrayList<Object> thisList = new ArrayList<>();
      JSONArray listObjects = new JSONArray(object.toString());
      for (Object currentElem : listObjects) {
        thisList.add(parseObject(currentElem));
      }
      //parseType.put(object.toString(), "List");
      return thisList;
    } else if (isMapJSONValid(object.toString())) {
      Map<Object, Object> map = new HashMap<>();
      JSONObject mapObjects = new JSONObject(object.toString());
      for (String element : mapObjects.keySet()) {
        map.put(element, parseObject(mapObjects.get(element)));
      }
      //parseType.put(object.toString(), "Map");
      return map;
    } else {
      //parseType.put(object.toString(), object.getClass().toString());
      return object;
    }
  }

  /**
   * Function to test if a String is a Valid JSON Map
   * @param test String to test
   * @return
   */
  public boolean isMapJSONValid(String test) {
    try {
      new JSONObject(test);
    } catch (JSONException ex) {
      return false;
    }
    return true;
  }

  /**
   * Function to test if a String is a Valid JSON List
   * @param test String to test
   * @return
   */
  public boolean isListJSONValid(String test) {
    try {
      new JSONArray(test);
    } catch (JSONException ex) {
      return false;
    }
    return true;
  }

  //@Override
  public void open() {

    //
    // Load the property URL KEY, if defined reach this URL otherwise default URL
    //

    final String keyValue = getProperty(URL_KEY);
    this.current_Url = keyValue;
    
    //Map<>
  }

  public Pair<InterpreterResult.Code, String> execRequest(String body) throws Exception {

    //
    // Execute the request on current url defined
    //

    String url = this.current_Url;
    url += "/exec";
    URL obj = new URL(url);
    HttpURLConnection con = null;

    //
    // If HTTPS execute an HTTPS connection
    //

    if (url.startsWith("https")) {
      con = (HttpsURLConnection) obj.openConnection();
    } else {
      con = (HttpURLConnection) obj.openConnection();
    }

    //add request header
    con.setDoOutput(true);
    con.setDoInput(true);
    con.setRequestMethod("POST");
    con.setChunkedStreamingMode(16384);
    con.connect();

    //
    // Write the body in the request
    //

    OutputStream os = con.getOutputStream();
    //GZIPOutputStream out = new GZIPOutputStream(os);
    PrintWriter pw = new PrintWriter(os);  
    pw.println(body);
    pw.close();

    StringBuffer response = new StringBuffer();
    Pair<InterpreterResult.Code, String> resultPair = null;

    //
    // If answer equals 200 parse result stream, otherwise error Stream
    //

    if (200 == con.getResponseCode()) {
      BufferedReader in = new BufferedReader(
          new InputStreamReader(con.getInputStream()));
      String inputLine;

      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
      resultPair = new Pair<InterpreterResult.Code, String>(InterpreterResult.Code.SUCCESS, 
          response.toString());
      in.close();
      con.disconnect();
    } else {
      String errorLine = "\"Error-Line\":" + con.getHeaderField("X-Warp10-Error-Line");
      String errorMsg = "\"Error-Message\":\"" 
          + con.getHeaderField("X-Warp10-Error-Message") + "\"";
      response.append("[{");
      response.append(errorLine + ",");
      response.append(errorMsg);
      boolean getBody = (null == con.getContentType());
      if (!getBody && !con.getContentType().startsWith("text/html")) {
        getBody = true;
      }
      if (getBody) {
        response.append(",\"Body\":\"");
        BufferedReader in = new BufferedReader(
            new InputStreamReader(con.getErrorStream()));
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }
        in.close();
        response.append("\"");
      }
      response.append("}]");
      resultPair = new Pair<InterpreterResult.Code, String>(InterpreterResult.Code.ERROR, 
          response.toString());
      con.disconnect();
    }

    //
    // Return the body message with its associated code (SUCESS or ERROR)
    //

    return resultPair;
  }
}
