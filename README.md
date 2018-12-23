# e2ewd
## Hermetical test for your Sisense server

[![GitHub issues](https://img.shields.io/github/issues/kbbgl/e2ewd.svg?style=plastic)](https://github.com/kbbgl/e2ewd/issues)

[![GitHub repo size in bytes](https://img.shields.io/github/repo-size/badges/shields.svg)](https://github.com/kbbgl/e2ewd)

[![Depfu](https://img.shields.io/depfu/depfu/example-ruby.svg)](https://github.com/kbbgl/e2ewd)


### Tests 

* ElastiCube Server is queriable.
* ElastiCube Server internal connectivity.
* REST API is queriable.

### Result
Once the application finishes execution, it will create a file in:
`[path/to/dir]/build/libs/run/result.txt`
which will contain a `boolean` as the result, i.e. `true` means that the tests ran successfully.

### Instructions
1) Download or clone the project to the Sisense server. 
2) Navigate to `[path/to/dir]/build/libs/` (where `[path/to/dir]` is where you saved the project.)
3) Open the `config.properties` file and enter the:
 
 - Token (`String`) - Can be retrieved using the REST API `GET v1/authentication`
 - Protocol (`String`) - i.e. `http` or `https`
 - Host (`String`) -  i.e. `localhost` or your Sisense application gateway
 - Port (`integer`) - i.e. `80, 8081, 443`
 - *Restart ECS (`boolean`) -  i.e. `true, false` - default or empty is `false`
 - REST API Connection Timeout in seconds (`int`) - i.e. `30`
 
 **If you set `restartECS=true` you will have to run the below command in elevated mode (run as Administrator)

4) Launch a Command Prompt from the current folder and run the following command:  

    `java -jar e2ewd.jar`
    
* If you don't have `java` installed globally, you may use the JRE located in:
`"C:\Program Files\Sisense\Infra\jre\bin\java.exe"`

  The command to run the application becomes:

 `"C:\Program Files\Sisense\Infra\jre\bin\java.exe" -jar e2ewd.jar`
    
### Troubleshooting
See log file located in:
`[path/to/dir]/build/libs/log/log.txt`