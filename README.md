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

This method allow the user to define the parameter he wants to use: for example choosing the key of the serie to print, its color. The user can also chose some globals parameters as the interpolation, if wants to print the series with date or timestamps values, or choose the titles of its differents axis.

An other to print a serie is to have a scala object as the variable data2 which is a simple list of time-serie. Then quantumviz will do its best to print them automatically. 

```
{
    'data2'
    [ $gts1 ]
}
```

This [page](https://github.com/aurrelhebert/WarpScript-Sample/blob/master/zeppelin/warpscript-quantumviz.json) contains a valid JSON with stored series in WarpScript and the method to print them with Quantumviz.

## Real example

In this [repository](https://github.com/aurrelhebert/WarpScript-Sample/tree/master/zeppelin) you will find how the Fuel data-set was presented using Zeppelin.

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
