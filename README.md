## Interpreter QuantumViz

** Quantumviz has migrated, this documentation corresponds now to a new QuantumViz release (1.6.1). Use 0.6.1 branches for older version.**

## Requirement

Quantumviz version 1.6.1 must be deployed somewhere (for example directly inside zeppelin-web). 
To work with QuantumViz interpreter, data to plot have to be stored previously in the global resource scope, using an interpreter.
The format can be an object in [QuantumViz](https://github.com/cityzendata/warp10-quantumviz/blob/master/examples/example-warp10-display-chart-syntax.html) format, a GTS series, or a GTS list.

## GTS format in Scala

Here you will find an example on Scala of a valid serie, than can be plot using QuantumViz

```
import com.google.gson.Gson

//
// GTS implementation in Scala
//

class GTS(var c: String, var l: java.util.Map[String, String], var a : java.util.Map[String, String], var v : java.util.List[Object]) {

  override def toString: String =
    "{" + "a=" + a + ", c=" + c + ", v=" + v + ", l=" + l + "}"
}

//
// Instanciate series labels, attributes and values
//

val labels = new Gson().fromJson("{ 'l0' : 'a' }", classOf[java.util.Map[String, String]]);
var attributes = new Gson().fromJson("{}", classOf[java.util.Map[String, String]]);
var values =  new Gson().fromJson("[[1, 0], [1000, 1], [2000, 2], [10000, 10], [20000, 20]]", classOf[java.util.List[Object]]);

//
// Instanciate the series, and create it's associates JSON object
//

val scalaGTS = new GTS("name", labels , attributes, values)
val json = new Gson().toJson(scalaGTS)

//
// Put series in Zeppelin resource pool
//

z.put("scalaGTS", json)
```

## Syntax of QuantumViz interpreter

The input string for the QuantumViz interpreter is now a ** JSON String**. 

There is four different fields : *data*, *default-width*, *default-height* and *type*.

The type key can be or *graph* to plot the series as a graph or *geo* as a geographical map. By default a graph is plotted.
The keys default-width are default-height used to set the default width and height for each graphs. Those keys are optionnals, and are set by default to 600px for the height and 95 % for the width.
The key data is use to load the specific data to visualize. This key is required.

The data object can have different fields :
	-	*series* (required) corresponds to the object to load in Zeppelin pool. It can be object directly in QuantumViz format, GTS series, or a GTS list.
    - 	*width* (optional) the width or the current graph.
    -   *interpolate* (optional) change the interpolation of the graph. By default, it is QuantumViz value : interpolate.
    -   *timestamps* (optional) the time display (timestamps or date). By default, it is QuantumViz value : false.
    -   *xLabel* (optional) used to name the x axis. By default, there is no name.    
    -   *yLabel* (optional) used to name the y axis. By default, there is no name.

Example of the syntax of the QuantumViz interpreter for Zeppelin
```
%quantumviz
{
    "data" : 
        [ 
            { "series" : "scalaGTS", "width" : "600px", "interpolate" : "step-before", xLabel : "x", yLabel : "y", timestamps : true },
            { "series" : "scalaGTS", "width" : "600px" }
        ],
    "default-width" : "80%",
    "default-height" : "300px",
    "type" : "graph"
}
```

## Configuration

This plugin can be used to plot [quantumviz](https://github.com/cityzendata/warp10-quantumviz) graphs in [apache zeppelin](https://zeppelin.apache.org/). It uses the library defined in quantum url given as parameter.

To get started configure the QuantumViz interpreter, adding the path to Quantumviz if local or an url to get quantumviz component as parameter. Use the "." value, if QuantumViz is deployed directly inside Zeppelin.

```
name:                 value:
warp10.url           Path/to
```

## Set-up 

Compile the interpreter with maven.

```
mvn clean package
```

Create a directory quantumviz in folder interpreter.

Then copy the Jar with dependencies inside.

Add in file conf/zeppelin-site.xml, for the property zeppelin.interpreters, this interpreter: 

```
<property>
<name>zeppelin.interpreters</name>
<value>org.apache.zeppelin.spark.SparkInterpreter,...,org.apache.zeppelin.quantumviz.QuantumVizInterpreter</value>
</property>
```

Stop and restart Zeppelin.

## Deploy

To deploy, use branch deploy and add this jar with scp add the follwing property file etc/config/local.properties containing: 

```
sshUrl=scp://user@my.domain:/path/to
```

Then run 

```
mvn deploy -Drat.skip=true
```
