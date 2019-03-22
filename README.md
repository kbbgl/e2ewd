# e2ewd
## Hermetical test for your Sisense server

[![GitHub issues](https://img.shields.io/github/issues/kbbgl/e2ewd.svg?style=plastic)](https://github.com/kbbgl/e2ewd/issues)

[![GitHub repo size in bytes](https://img.shields.io/github/repo-size/badges/shields.svg)](https://github.com/kbbgl/e2ewd)

[![Depfu](https://img.shields.io/depfu/depfu/example-ruby.svg)](https://github.com/kbbgl/e2ewd)



### Tests 

* ElastiCube Server is queriable.
* ElastiCube Server internal connectivity.
* REST API is queriable.

Tests are performed for all ElastiCubes with are in _RUNNING_ mode.  

### Instructions
Installer available [here](https://github.com/kbbgl/e2ewd_installer). It will download assets and create the configuration file automatically.

1) Download or clone the project to the Sisense server. 
2) Navigate to `[path/to/dir]/build/libs/` (where `[path/to/dir]` is where you saved the project.)
3) Open the `config.properties` file and fill it out according to the following options:
 
 | Key                         | Description                                                                                                                                                                | Type    | Example                                                                       |
 |-----------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------|-------------------------------------------------------------------------------|
 | `token`                       | Can be retrieved using the Sisense REST API POST v1/authentication endpoint.                                                                                               | `String`  |                                                                               |
 | `host`                        | The hostname of your Sisense site.                                                                                                                                         | `String`  | `mycompany.sisense.com`                                                   |
 | `protocol`                    | The protocol used to access your Sisense site.                                                                                                                             | `String`  | `https` or `http`                                                                 |
 | `port`                        | The port which your Sisense application listens on.                                                                                                                        | `int`     | `80`, `8081`, `443`                                                                 |
 | (Optional) `restartECS`       | Whether to perform an ElastiCube Server restart if the test fails. The application must be run as Administrator if this option is set to `true`                            | `boolean` | `true` or `false`[default]                                                        |
 | (Optional) `restartIIS`       | Whether to perform an IIS reset if the test fails. The application must be run as Administrator if this option is set to `true`                                            | `boolean` | `true` or `false`[default]                                                        |
 | (Optional) `iisDump`          | Whether to perform an IIS process memory dump if the test fails. The application must be run as Administrator if this option is set to `true`                              | `boolean` | `true` or `false`[default]                                                        |
 | (Optional) `ecsDump`          | Whether to perform an ElastiCubeManagementService process memory dump if the test fails. The application must be run as Administrator if this option is set to `true`      | `boolean` | `true` or `false`[default]                                                        |
 | `requestTimeoutInSeconds`     | The number of seconds to wait until timing out of the REST API test.                                                                                                       | `int`     | `300`                                                                           |
 | (Optional) `slackWebhookURL`  | If set, the application will send a notification in the Slack channel. To retrieve, visit [Slack Incoming webhook documentation](https://api.slack.com/incoming-webhooks.) | `String`  | `https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX` |
 | (Optional) `friendlyHostName` | Server alias. Will be used to notify Slack channel.                                                                                                                        | `String`  | `QUERY-NODE-1`                                                                  |

 4.Run the `run.bat` file as an Administrator.  
    
    
### Result
Once the application finishes execution, it will create a file in:
`[path/to/dir]/build/libs/run/result.txt`
which will contain a `boolean` as the result, i.e. `true` means that **ALL** ElastiCube tests ran successfully.
    
If you've set `ecsDump`, `iisDump` to `true`, it will create a dump file of the respective process in the parent folder. 
The dump includes a snapshot of the process memory set which can be useful in troubleshooting the root cause.
    
### Troubleshooting
See log file located in:
`[path/to/dir]\build\libs\log\log.txt`
