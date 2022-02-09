## logsvc Overview
This project implments a basic log message management service in the form of a REST API.

## Executing without building the project
### Run docker-compose
logsvc can be executed with the docker-compose.yml file provided in the root of the project.

```
docker-compose up -d
```

The service will be exposed at `http://localhost:8080/logs`

NOTE: It takes a while before Elasticsearch is fully initialized. Exercising the logsvc endpoints too soon can result in transient errors.

### Import logsvc.postman_collection.json into Postman
As a convenience for exercising the logsvc API endpoints, a postman collection file has been provided. If you do not already have the Postman app installed, it is available [here](https://www.postman.com/downloads/).

Run Postman and import file `logsvc.postman_collection.json` from the root of the project. Four requests are provided in the collection:

* PostLogs: Stores log messages.
* PostSearch: Queries stored log messages.
* DeleteSearch: Deletes log messages matching the search criteria.
* GetAbout: Returns a message with the author's name.

Experiment with the first three endpoints to test logsvc.

## Building the project
I am a bit uncertain as to the full set of dependencies needed to build logsvc. At a minimum, you will need:

* Java JDK 11
* Gradle
* Docker

To build: 
```
./gradlew build
```

To create the logsvc docker container:
```angular2html
./gradlew dockerBuild
```
