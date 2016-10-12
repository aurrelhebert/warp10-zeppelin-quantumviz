## Interpreter QuantumViz

This plugin can be used to plot [quantumviz](https://github.com/cityzendata/warp10-quantumviz) graphs in [apache zeppelin](https://zeppelin.apache.org/). It uses the library defined in quantum url given as parameter.

To get started configure the QuantumViz interpreter, adding the path to Quantumviz if local or an url to get quantumviz component as parameter.

```
name:                 value:
warp10.url           Path/to
```

This zeppelin interpreter espect scala object that follow the modelisation choosen quantumviz. 

## Use

Just put the parameter of the scala object to print in the paragraph quantumviz. One variable per line, multiple line will print multiples graphs.

```
%quantumviz
dataviz
data2
```

To work with quantum, data have to be stored previouslys in the globale resource scope. In WarpScript, they corresponds to the following modelisation. To print the scala object dataviz, a new list containing a map including three parameters (gts,params and globalParams).

```
{
    'dataviz'
    [
        {
            'gts'
            [ 
                $gts1
                $gts2
            ]
            'params'
            [
                { 'key' 'key1' }
                { 'key' 'key2' 'color' '#ff1010' }
            ]
            'globalParams'
            { 'interpolate' 'linear' }
        }
    ]
}
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
