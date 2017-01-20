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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

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

  @Override
  public void cancel(InterpreterContext arg0) {
    //

  }

  @Override
  public void close() {

  }

  @Override
  public List<InterpreterCompletion> completion(String arg0, int arg1) {
    //
    return null;
  }

  @Override
  public FormType getFormType() {
    //
    return FormType.SIMPLE;
  }

  //@Override
  public int getProgress(InterpreterContext arg0) {
    //
    return 0;
  }

  //@Override
  //
  // When the result of the WarpScript contains a Map on top of the stack
  // then all the tuples key,value are saved in Angular variables.
  // To ensure a Map on top of the stack the WarpScript function EXPORT can be used
  // When using Angular to save variable NaN and Infinity are transformed in String !
  //
  public InterpreterResult interpret(String body, InterpreterContext context) {

    System.out.println(context.getConfig().toString());
    //
    // Store the resource pool already defined in context
    //

    ResourcePool resources = context.getResourcePool();

    StringBuilder res = new StringBuilder();
    res.append("\"\"\"%html ");
    res.append("<script> "
        + "if (!jsCode) {"
        + "var jsCode = document.createElement('script');"
        // 'https://warp.cityzendata.net/quantumviz/latest/webcomponentsjs/webcomponents-lite.js'
        + "jsCode.setAttribute('src', '" + current_Url 
        + "/webcomponentsjs/webcomponents-lite.js'); "
        + "document.body.appendChild(jsCode); "
        + "} "
        + "</script> ");
    res.append("<link   rel=\"import\" href=\"" + current_Url 
      + "/polymer/polymer.html\"> ");
    res.append("<link   rel=\"import\" href=\"" + current_Url 
      + "/warp10-quantumviz/warp10-display-alt-chart.html\"> ");
    String bodyLines[] = body.split("\n");
    //String areWords = "";

    //res.append(bodyLines.length);
    
    for (String line : bodyLines) {
      
      res.append("<div>");
      res.append("<warp10-display-chart style=\"height:600px\" width=\"800\"");
      
      String[] words = line.split("\\s+");
      
      StringBuilder currentGraphs = new StringBuilder();
      for (String word : words) {
        Resource resource = resources.get(word);

        //areWords += word + " ";
        if (resource != null) {
          Object value = resources.get(word).get();
          currentGraphs.append(parseObjectToString(value));
        }
      }
      
      
      res.append("data='" + currentGraphs.toString() + "'");
      
      res.append(" </warp10-quantumviz> <p> </p>");

      res.append("</div>");
      System.out.println(res.toString());
    }
    //res.append("\"\"\"");

    return new InterpreterResult(InterpreterResult.Code.SUCCESS, res.toString());

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
