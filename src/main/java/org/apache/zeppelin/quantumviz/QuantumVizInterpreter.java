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
import org.apache.commons.lang3.BooleanUtils;
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
  private String JSON_MAX_HEIGHT_KEY = "default-height";
  private String JSON_MAX_WIDTH_KEY = "default-width";
  private String JSON_HEIGHT_KEY = "height";
  private String JSON_WIDTH_KEY = "width";
  private String JSON_DATA_KEY = "data";
  private String JSON_SERIES_KEY = "series";
  private String JSON_INTEPOLATE_KEY = "interpolate";
  private String JSON_TIMESTAMP_KEY = "timestamps";
  private String JSON_XLABEL_KEY = "xLabel";
  private String JSON_YLABEL_KEY = "yLabel";
  private String JSON_GLOBALPARAMS_KEY = "globalParams";
  private String JSON_GTS_KEY = "gts";
  
  private List<String> listQuantumInterpolate = Arrays.asList("linear", "cardinal", "step-before");
  
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
      
      // return a Zeppelin error    
      return new InterpreterResult(InterpreterResult.Code.ERROR, 
          "Quantumviz interpreter expects a valid JSON as input");
    }
    //String bodyLines[] = body.split("\n");
    
    //
    // Verify that the JSON String has 
    //
    
    if (!jsObject.has(this.JSON_DATA_KEY)) {
      
      // return a Zeppelin error    
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
        
        // return a Zeppelin error     
        return new InterpreterResult(InterpreterResult.Code.ERROR, 
            "Quantumviz interpreter expects custom setting type to be "
            + "one of the following one [" 
            + this.SETTING_TYPE_GRAPH + ", " + this.SETTING_TYPE_GEO + "].");
      }
    }
    
    //
    // Check if div default height is set by the user
    //    
    try {
      maxHeight = initializeWidthHeight(this.SETTING_DEFAULT_MAX_HEIGHT, 
          jsObject, this.JSON_MAX_HEIGHT_KEY);
    } catch (Exception eWidth){
      
      // return a Zeppelin error
      return new InterpreterResult(InterpreterResult.Code.ERROR, eWidth.getMessage());
    }
    
    //
    // Check if div default width is set by the user
    //
    try {
      maxWidth = initializeWidthHeight(this.SETTING_DEFAULT_MAX_WIDTH, 
          jsObject, this.JSON_MAX_WIDTH_KEY);
    } catch (Exception eWidth){
      
      // return a Zeppelin error
      return new InterpreterResult(InterpreterResult.Code.ERROR, eWidth.getMessage());
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
      
      // return a Zeppelin error
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
       
        // return a Zeppelin error
        return new InterpreterResult(InterpreterResult.Code.ERROR, 
            "Quantumviz interpreter encouters an incorrect data type: "
            + "each element must be a valid JSON object.");
      }
      JSONObject jsonElement = (JSONObject) dataObject;
      
      //
      // Check if there is a series key
      //
      
      if (!jsonElement.has(this.JSON_SERIES_KEY)) {
        
        // return a Zeppelin error
        return new InterpreterResult(InterpreterResult.Code.ERROR, 
            "Quantumviz interpreter encouters an incorrect data type: "
            + "each element needs a series key.");
      }
      
      //
      // Check if there is a series key
      //
      
      if (!(jsonElement.get(this.JSON_SERIES_KEY) instanceof String )) {
        
        // return a Zeppelin error
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
      // Check if user defined an height for current div
      //
      String height = maxHeight;
      
      try {
        height = initializeWidthHeight(maxHeight, jsonElement, this.JSON_HEIGHT_KEY);
      } catch (Exception eWidth){
        
        // return a Zeppelin error
        return new InterpreterResult(InterpreterResult.Code.ERROR, eWidth.getMessage());
      }
      
      //
      // Check if user defined a width for current div
      //
      
      String width = maxWidth;
      try {
        width = initializeWidthHeight(maxWidth, jsonElement, this.JSON_WIDTH_KEY);
      } catch (Exception eWidth){
        
        // return a Zeppelin error
        return new InterpreterResult(InterpreterResult.Code.ERROR, eWidth.getMessage());
      }
      
      //
      // Set up its style (height and width)
      //
      
      res.append("<" + display + " style=\"height:" + height + ";"
          + "max-width:" + width + ";\"");
      
      //
      // Load resources from Zeppelin if founded
      //
      
      Resource resource = resources.get(seriesKey);
      String valueString = "";
      if (resource != null) {
        Object value = resources.get(seriesKey).get();
        valueString = parseObjectToString(value);
        
        //
        // Manage globalParams key with user value
        //
        
        try {
          valueString = manageGlobalParameter(parseObjectToString(value), jsonElement);  
        } catch (Exception eValue){
          
          // return a Zeppelin error
          return new InterpreterResult(InterpreterResult.Code.ERROR, eValue.getMessage());
        }
      } else {
        
        // return a Zeppelin error
        return new InterpreterResult(InterpreterResult.Code.ERROR, 
            "Quantumviz interpreter encouters an incorrect series type: "
            + "series not found in Zeppelin resource pool.");
      }
      
      
      //
      // Append data string and then close web component
      // 
      
      res.append("data='" + valueString + "'");
      res.append(" </" + display + "> <p> </p>");
      res.append("</div>");
      //System.out.println(res.toString());
    }
    
    //res.append("\"\"\"");

    return new InterpreterResult(InterpreterResult.Code.SUCCESS, res.toString());

  }

  private String manageGlobalParameter(String resource, 
      JSONObject jsonElement) throws JSONException, Exception {
    
    String result = resource;
    
    //
    // Check if JsonElement given as parameter contains one of the global param key
    //
    
    if (!(jsonElement.has(this.JSON_INTEPOLATE_KEY) || jsonElement.has(this.JSON_TIMESTAMP_KEY) 
        || jsonElement.has(this.JSON_XLABEL_KEY) || jsonElement.has(this.JSON_YLABEL_KEY))) {
      return result;
    }
    
    //
    // Check if it corresponds to an array
    //
    
    if (resource.startsWith("[")) {
      JSONArray resourceAsArray = new JSONArray(resource);   
      JSONArray secondaryArray = new JSONArray();
      
      //
      // Check if first elem of resourceArray contains a global param
      //
      if (resourceAsArray.getJSONObject(0).has(JSON_GLOBALPARAMS_KEY)) {
      
        for (Object object : resourceAsArray) {
          if (object instanceof JSONObject) {
            JSONObject resourceAsJson = (JSONObject) object;  
            
            //
            // Get global params if it exists then set it
            //
            
            if (resourceAsJson.has(JSON_GLOBALPARAMS_KEY)) {
              JSONObject globalParams = modifyGlobalParams(resourceAsJson.
                  getJSONObject(JSON_GLOBALPARAMS_KEY), jsonElement);
              resourceAsJson.put(JSON_GLOBALPARAMS_KEY, globalParams);
            }
            secondaryArray.put(resourceAsJson);
          } else {
            secondaryArray.put(object);
          }
        }
        result = secondaryArray.toString();
      }
      
      //
      // Check if first elem of resourceArray contains a global param
      //
      else if (resourceAsArray.getJSONObject(0).has(JSON_GTS_KEY)) {
        for (Object object : resourceAsArray) {
          JSONObject resourceAsJson = (JSONObject) object;  
          resourceAsJson.put(this.JSON_GLOBALPARAMS_KEY, getGlobalParams(jsonElement));
          secondaryArray.put(resourceAsJson);
        }
        result = secondaryArray.toString();
      } else {

        //
        // Else then it is a GTS arrayList on the stack
        //
        JSONObject element = new JSONObject();
        element.put(this.JSON_GTS_KEY, resourceAsArray);
        element.put(this.JSON_GLOBALPARAMS_KEY, getGlobalParams(jsonElement));
        secondaryArray.put(element);
        result = secondaryArray.toString();
      }  

      
      //element.append(JSON_GTS_KEY, value)
     
    //
    // Check if it's an object
    //
      
    } else if (resource.startsWith("{")) {
      JSONArray arrayResult = new JSONArray();
      JSONObject resourceAsJson = new JSONObject(resource);  
      
      //
      // If it's an object in quantum format, update Global params
      //
      
      if (resourceAsJson.has(this.JSON_GLOBALPARAMS_KEY)) {
        JSONObject globalParams = modifyGlobalParams(
            resourceAsJson.getJSONObject(JSON_GLOBALPARAMS_KEY), jsonElement);
        resourceAsJson.put(this.JSON_GLOBALPARAMS_KEY, globalParams);
        arrayResult.put(resourceAsJson);
        result = arrayResult.toString();
        
      //
      // If it's an object in quantum format, without Global Params
      //  
        
      } else if (resourceAsJson.has(this.JSON_GTS_KEY)) {
        resourceAsJson.put(this.JSON_GLOBALPARAMS_KEY, getGlobalParams(jsonElement));
        arrayResult.put(resourceAsJson);
        result = arrayResult.toString();
      } else {
        
        //
        // Else then expect a GTS on the stack
        //
        
        JSONObject element = new JSONObject();
        element.put(this.JSON_GTS_KEY, resourceAsJson);
        element.put(this.JSON_GLOBALPARAMS_KEY, getGlobalParams(jsonElement));
        arrayResult.put(element);
        result = arrayResult.toString();
      }
    }

    return result;
  }

  private Object getGlobalParams(JSONObject jsonElement) {
    JSONObject jsonObject = new JSONObject();
    
    if (jsonElement.has(this.JSON_INTEPOLATE_KEY)) {
      jsonObject.put(this.JSON_INTEPOLATE_KEY, jsonElement.get(this.JSON_INTEPOLATE_KEY));
    }
    
    if (jsonElement.has(this.JSON_TIMESTAMP_KEY)) {
      jsonObject.put(this.JSON_TIMESTAMP_KEY, jsonElement.get(this.JSON_TIMESTAMP_KEY));
    }
    
    if (jsonElement.has(this.JSON_XLABEL_KEY)) {
      jsonObject.put(this.JSON_XLABEL_KEY, jsonElement.get(this.JSON_XLABEL_KEY));
    }
    
    if (jsonElement.has(this.JSON_YLABEL_KEY)) {
      jsonObject.put(this.JSON_YLABEL_KEY, jsonElement.get(this.JSON_YLABEL_KEY));
    }
    return jsonObject;
  }

  private JSONObject modifyGlobalParams(JSONObject globalParams, 
      JSONObject jsonElement) throws Exception {
   
    //
    // Case interpolate
    //
    
    if (jsonElement.has(this.JSON_INTEPOLATE_KEY)) {
      Object interpolateObj = jsonElement.get(this.JSON_INTEPOLATE_KEY);
      
      if (interpolateObj instanceof String) {
        String interpolate = (String) interpolateObj;      
        
        //
        // Verify string entered as interpolate is valid
        //
        
        if (this.listQuantumInterpolate.contains(interpolate)) {
          globalParams.put(this.JSON_INTEPOLATE_KEY, interpolate);
        } else {
          throw new Exception ("Quantumviz interpreter expects interpolate value to be one of "
              + this.listQuantumInterpolate.toString());
        }
      } else {
        throw new Exception ("Quantumviz interpreter expects interpolate value to be a string");
      }
    }
    
    //
    // Case timestamp
    //
    
    if (jsonElement.has(this.JSON_TIMESTAMP_KEY)) {
      Object timestampObj = jsonElement.get(this.JSON_TIMESTAMP_KEY);
      
      //
      // Verify that timestamp value set is a boolean then push it 
      //
      
      if (timestampObj instanceof Boolean) {
        globalParams.put(this.JSON_TIMESTAMP_KEY, (Boolean) timestampObj);
      } else if (timestampObj instanceof String) {
        if (null != BooleanUtils.toBooleanObject(((String) timestampObj))) {
          globalParams.put(this.JSON_TIMESTAMP_KEY, 
              BooleanUtils.toBooleanObject(((String) timestampObj)));
        } else {
          throw new Exception ("Quantumviz interpreter expects timestamp value to be a boolean");
        }
      } else {
        throw new Exception ("Quantumviz interpreter expects timestamp value to be a boolean");
      }
    }

    //
    // Case xLabel
    //
    
    if (jsonElement.has(this.JSON_XLABEL_KEY)) {
      
      //
      // Verify that xLabel value entered is a string
      //
      
      Object xLabel = jsonElement.get(this.JSON_XLABEL_KEY);
      
      if (xLabel instanceof String) {
        globalParams.put(this.JSON_XLABEL_KEY, (String) xLabel);
      }
    }

    //
    // Case yLabel
    //
    
    if (jsonElement.has(JSON_YLABEL_KEY)) {
      
      //
      // Verify that yLabel value entered is a string
      //
      
      Object yLabel = jsonElement.get(this.JSON_YLABEL_KEY);
      
      if (yLabel instanceof String) {
        globalParams.put(this.JSON_YLABEL_KEY, (String) yLabel);
      }
    }
    
    return globalParams;
  }

  /**
   * Method to initialize Style variable height and width
   * 
   * @param defaultRes default returned result
   * @param jsonObject jsonObject to look for
   * @param code_key json key of current style
   * @return
   * @throws Exception to return a Zeppelin error
   */
  private String initializeWidthHeight(String defaultRes, 
      JSONObject jsonObject, String code_key) throws Exception {
    
    String result = defaultRes;
    
    if (jsonObject.has(code_key)) {
     
      boolean condition = false;
      String errorMessage = "Quantumviz interpreter encounters an error "
          + "when checking an Height or width element";
      
      //
      // In case code key corresponds to an height check, set condition using test on height
      //
      
      if (code_key.equals(this.JSON_HEIGHT_KEY) || code_key.equals(this.JSON_MAX_HEIGHT_KEY)) {
        condition = isHeightOk(jsonObject.getString(code_key));
        if (!condition) {
          errorMessage = "Quantumviz interpreter expects custom setting height to be "
            + "a number followed by the string px.";
        }
      }
      
      //
      // In case code key corresponds to a width check, set condition using test on width
      //
      
      if (code_key.equals(this.JSON_WIDTH_KEY) || code_key.equals(this.JSON_MAX_WIDTH_KEY)) {
        condition = isWidthOk(jsonObject.getString(code_key));
        if (!condition) {
          errorMessage = "Quantumviz interpreter expects custom setting width to be "
              + "a number followed by the string px or %.";
        }
      }
      
      //
      // Set result string from JSON object
      //
      
      if (condition) {
        result = jsonObject.getString(code_key);
      } else {
        
        //throw an exception
        throw new Exception(errorMessage);
      }
    }
    
    return result;
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
