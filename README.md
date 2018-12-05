# e2ewd
## Hermetical test for your Sisense server

[![GitHub issues](https://img.shields.io/github/issues/kbbgl/e2ewd.svg?style=plastic)](https://github.com/kbbgl/e2ewd/issues)

[![GitHub repo size in bytes](https://img.shields.io/github/repo-size/badges/shields.svg)](https://github.com/kbbgl/e2ewd)

[![Depfu](https://img.shields.io/depfu/depfu/example-ruby.svg)](https://github.com/kbbgl/e2ewd)


### Tests 

* Verifies web application DB is queriable.
* Verifies ECS is queriable.
* Verifies web application API is connected.

### Result
Once the application finishes execution, it will create a file in:
`[path/to/dir]/build/libs/run/result.txt`
which will contain a `boolean` as the result, i.e. `true` means that the tests ran successfully.

### Instructions
1) Download or clone the project to the Sisense server. 
2) Launch a Command Prompt and Navigate to `[path/to/dir]/build/libs/` (where `[path/to/dir]` is where you saved the project.)
3) Open the `config.properties` file and enter the token, protocol, host, port of your Sisense instance and save the file. There's also an option to restart the ElastiCube Server (`restartECS`). If you're planning to set `restartECS=true` , you will need to open the Command Prompt in elevated mode (i.e. as a Administrator) 

4) Run the command:  

    `java -jar e2ewd.jar`
    
* If you don't have `java` installed globally, you may use the JRE located in:
`"C:\Program Files\Sisense\Infra\jre\bin\java.exe"`

  The command to run the application becomes:

 `"C:\Program Files\Sisense\Infra\jre\bin\java.exe" -jar e2ewd.jar`
    
### Troubleshooting
See log file located in:
`[path/to/dir]/build/libs/log/log.txt`