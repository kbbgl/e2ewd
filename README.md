# e2ewd

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
2) Navigate to `[path/to/dir]/build/libs/` (where `[path/to/dir]` is where you saved the project.)
3) Open the `config.properties` file and enter the token, host and port of your Sisense instance and save the file.
4) Run the command:  


    `java -jar e2ewd.jar`
    
**NOTE**: If you don't have `java` installed globally, you may use the JRE located in:
`"C:\Program Files\Sisense\Infra\jre\bin\java.exe"`

So the command to run the application becomes:

    "C:\Program Files\Sisense\Infra\jre\bin\java.exe" -jar e2ewd.jar
    
### Troubleshooting
See log file located in:
`[path/to/dir]/build/libs/log/log.txt`