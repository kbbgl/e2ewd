# e2ewd
#### Watchdog your Sisense server   

[![GitHub issues](https://img.shields.io/github/issues/kbbgl/e2ewd.svg?style=plastic)](https://github.com/kbbgl/e2ewd/issues)

[![GitHub repo size in bytes](https://img.shields.io/github/languages/code-size/kbbgl/e2ewd.svg?label=Project%20Size&style=flat-square)](https://github.com/kbbgl/e2ewd)

### Features

* Checks and alerts whether the ElastiCube Server query engine is up.
* Checks the ElastiCube Server internal connectivity.
* Performs direct ElastiCube query.
* Slack Webhook alerts on individual REST API query failures, unhealthy microservices and Broker queues.
* Checks that Broker (`RabbitMQ`) is healthy (number of consumers, messages stuck in queue.)
* Checks the health of the microservices ( using `/api/test` endpoint.)  

Tests are performed for all ElastiCubes with are in _RUNNING_ mode.  

### Instructions

1) Download or clone the project to the Sisense server or run the [installation script](https://github.com/kbbgl/e2ewd_deployer/tree/master/build/libs).
If you've decided to run the installation script, you may skip steps 2 and 3 as it the script will create all directories, download the necessary files and create the configuration file with the default values mentioned in the table below.  
2) Navigate to `[path/to/dir]/build/libs/` (where `[path/to/dir]` is where you saved the project.)
3) Open the `config.properties` file and fill it out according to the following options:
 
 | Key                           | Description                                                                                                                                                                | Type      | Example                                                                         |
 |-------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|---------------------------------------------------------------------------------|
 | `token`                       | Can be retrieved using the Sisense REST API POST v1/authentication endpoint.                                                                                               | `String`  |                                                                                 |
 | `host`                        | The hostname of your Sisense site.                                                                                                                                         | `String`  | `mycompany.sisense.com`                                                         |
 | `protocol`                    | The protocol used to access your Sisense site.                                                                                                                             | `String`  | `https` or `http`                                                               |  
 | `port`                        | The port which your Sisense application listens on.                                                                                                                        | `int`     | `80`, `8081`, `443`                                                             |
 | `requestTimeoutInSeconds`     | The number of seconds to wait until timing out of the REST API test.                                                                                                       | `int`     | `300`                                                                           |
 | (Optional) `restartECS`       | Whether to perform an ElastiCube Server restart if it is unresponsive.                                                                                                     | `boolean` | `true` or `false`[default]                                                      |
 | (Optional) `restartIIS`       | Whether to perform an IIS reset if the ElastiCube Server is unresponsive.                                                                                                  | `boolean` | `true` or `false`[default]                                                      |
 | (Optional) `iisDump`          | Whether to perform an IIS process memory dump if the ElastiCube Server is unresponsive.                                                                                    | `boolean` | `true` or `false`[default]                                                      |
 | (Optional) `ecsDump`          | Whether to perform an ElastiCubeManagementService process memory dump if the ElastiCube Server is unresponsive.                                                            | `boolean` | `true` or `false`[default]                                                      |
 | (Optional) `ecDump`           | Whether to perform an ElastiCube process memory dump if the direct ElastiCube query fails.                                                            | `boolean` | `true` or `false`[default]                                                      |
 | (Optional) `slackWebhookURL`  | If set, the application will send a notification in the Slack channel. To retrieve, visit [Slack Incoming webhook documentation](https://api.slack.com/incoming-webhooks.) | `String`  | `https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX` |
 | (Optional) `friendlyHostName` | Server alias. Will be used to notify Slack channel.                                                                                                                        | `String`  | `QUERY-NODE-1`                                                                  |

 4.Run the `run.bat` file as an Administrator.  
    
If you're planning to run the `bat` file as a task (such as Windows Task Scheduler), make sure to set the `Start In` option to the 
directory from which the `bat` file is located. For example:

![alt text](https://i.ibb.co/wMBbtsk/2019-04-01-10h27-08.png)

### [High Availability](https://documentation.sisense.com/latest/administration/high-availability-in-sisense/high-availability.htm) environments

In HA environments, some services might run and others not (depending on the version). This means that the application
should only run on certain nodes (never on the Build Node). Use the table below as reference.
 
| Sisense Version | Nodes       |
|-----------------|-------------|
| 7.2+            | Query       |
| 7.1-            | Query + Web |
     

### Result
Once the application finishes execution, it will create a file in:
`[path/to/dir]/build/libs/run/result.txt`
which will contain a `boolean` as the result, i.e. `true` means that **ALL** ElastiCube tests ran successfully.

If `slackWebhookURL` is set in `config.properties`, the channel will be alerted with individual datasource query failures.  
    
### Troubleshooting
See log files located in:

`[path/to/dir]/log/log.%i.log`

`[path/to/dir]/log/log-error.%i.log`

### Configuration

If `ecsDump`, `iisDump`, `ecDump` are set to `true`, it will create a dump file of the respective process in the parent folder. 
The dump includes a snapshot of the process memory set which can be useful in troubleshooting the root cause.

The application logging framework is [logback/slf4j](https://logback.qos.ch/) so any configuration options offered by the library can be used by modifying the `logback.xml` file.  
The default configuration is set to log level `INFO`, 10 files  `FixedWindowRollingPolicy` sized 10MB each. 


#### Building

* Must have `gradle` installed.
* Run `gradle build` 