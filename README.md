# analytics-platform

## About
A platform for an online retailer, like Amazon, Alibaba etc.
The platform has 3 use cases:
- It stores events (views and buys of products, with data about the product and the user)
- It provides the client with user profiles (lists of events performed by the user)
- It aggregates the data in 1-minute buckets, and provides the aggregates to the client.
  (eg. the client can request the total price or the total number of all products in category X, by producer Y, that were bought in a given time range.)

This is a final project at a university course, and the exact requirements for this project are availible here
(this includes enpoints, and data formats): https://github.com/RTBHOUSE/mimuw-lab/tree/main/project

## How it works
The project is intended to be a distributed application and it has the followind components:
- frontend
- aggregate-processor
- a database
- a kafka broker

**frontend** is responsible for communication with the client.
It receives requests, queries the **database** to compute and return the answer requested by the client, it stores the events in the **database**, and publishes them to a **kafka topic**, to be aggregated by the **aggregate-processor**.

**aggregate-processor** subscribes to the **kafka topic**, selects relevant aggregates to be updated and updates them.

## Run the app
### Note:
  The app uses environment variables, all of which have a default value, 
  which assumes your entire app is running on localhost. 
  Therefore If you are running the app locally, there is no need to specify the variables.
  The variables are described [here](#environment)
### locally with sbt
- run kafka:
  ```
  export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
  docker-compose -f kafka/src/main/docker/docker-compose.yml up
  ```

  create a topic:
  ```
  docker exec -it broker bash
  [appuser@broker ~]$ kafka-topics --bootstrap-server localhost:9092 --topic tags-to-aggregate --create --partitions 10
  ```

- run database:
  ```
  docker-compose -f database/src/main/docker/docker-compose.yml up
  ```

- run frontend:
  ```
  sbt "project frontend" "~reStart"
  ```

- run aggregate-processor:
  ```
  sbt "project aggregateProcessor" "~reStart"
  ```
### with docker
To build and use the docker images you will need to specify your docker username.
ie. run this (both locally and on remote machines if relevant):
```
exprt DOCKER_USERNAME=<your-docker-username>
```

Or create an `.env` file and with the following line
```
DOCKER_USERNAME=<your-docker-username>
```

- build the images
  - frontend:
    ```
    docker build . \
        -t ${DOCKER_USERNAME}/analytics-platform-frontend \
        -f frontend/src/main/docker/Dockerfile
    ```

  - aggregate-processor:
    ```
    docker build . \
        -t ${DOCKER_USERNAME}/analytics-platform-aggregate-processor \
        -f aggregate-processor/src/main/docker/Dockerfile
    ```

- run the app locally
  ```
  docker-compose up
  ```

  create a topic:
  ```
  docker exec -it local-broker bash
  [appuser@broker ~]$ kafka-topics --bootstrap-server localhost:9092 --topic tags-to-aggregate --create --partitions 10
  ```

- run the app remotely

  - To run the app remotely you will adittionally need to specify the host and
    port of where your database is availible and the address of the kafka broker.
    
    Add the following lines to `.env` (remember, you also need `DOCKER_USERNAME`)
    ```
    DOCKER_USERNAME=<your-docker-username>
    AEROSPIKE_HOSTNAME=<host-of-the-database>
    AEROSPIKE_PORT=<port-of-the-database>
    KAFKA_BOOTSTRAP_SERVERS=<address-of-kafka-broker>
    ```

    choose a `PATH` (can be different for all machines) on your remote machines and copy the `.env` file there
    ```
    scp .env <USER>@<KAFKA-MACHINE>:<PATH>/.env
    scp .env <USER>@<FRONTEND-MACHINE>:<PATH>/.env
    scp .env <USER>@<AGG-PROC-MACHINE>:<PATH>/.env
    ```
  
  - push the images (run the commands in separate terminals to speed things up):
    ```
    docker push ${DOCKER_USERNAME}/analytics-platform-frontend
    docker push ${DOCKER_USERNAME}/analytics-platform-aggregate-processor
    ```
  
  - Copy the docker-compose files to your remote machines:
    ```
    scp kafka/src/main/docker/docker-compose.yml                <USER>@<KAFKA-MACHINE>:<PATH>/docker-compose.yml
    scp database/src/main/docker/docker-compose.yml             <USER>@<DATABASE-MACHINE>:<PATH>/docker-compose.yml
    scp frontend/src/main/docker/docker-compose.yml             <USER>@<FRONTEND-MACHINE>:<PATH>/docker-compose.yml
    scp aggregate-processor/src/main/docker/docker-compose.yml  <USER>@<AGG-PROC-MACHINE>:<PATH>/docker-compose.yml
    ```

  - On your remote database machine:
    ```
    cd <PATH>
    docker-compose up
    ```

  - On remote kafka machines:
    
    run kafka:
    ```
    cd <PATH>
    docker-compose up
    ```

    create a topic:
    ```
    docker exec -it broker bash
    [appuser@broker ~]$ kafka-topics --bootstrap-server ${KAFKA_BOOTSTRAP_SERVERS} --topic tags-to-aggregate --create --partitions 10
    ```

  - On your remote frontend machine:

    pull and run the image
    ```
    cd <PATH>
    docker pull ${DOCKER_USERNAME}/analytics-platform-aggregate-processor
    docker-compose up
    ```

  - On your remote aggregate-processor machine:

    pull and run the image
    ```
    cd <PATH>
    docker pull ${DOCKER_USERNAME}/analytics-platform-aggregate-processor
    docker-compose up
    ```

### Environment

The app uses the following environment variables:

| Variable                    | Format          | Default Value           | Description                                                           |
| --------------------------- | --------------- |------------------------ | --------------------------------------------------------------------- |
| `AEROSPIKE_HOSTNAME`        | string          | "localhost"             | Name of the host of the database                                      |
| `AEROSPIKE_PORT`            | integer         | 3000                    | Port at which the database is availible                               |
| `AEROSPIKE_NAMESPACE`       | string          | "analyticsplatform"     | Aerospike namespace in which your data is stored                      |
| `AEROSPIKE_PROFILES_SET`    | string          | "profiles"              | Name of the set where user profiles are stored                        |
| `AEROSPIKE_AGGREGATES_SET`  | string          | "aggregates"            | Name of the set where aggregates are stored                           |
| `AEROSPIKE_PROFILES_BIN`    | string          | "profile"               | Name of the bin of a record where profiles are stored                 |
| `AEROSPIKE_AGGREGATES_BIN`  | string          | "aggregate"             | Name of the bin of a record where aggregates are stored               |
| `KAFKA_TOPIC`               | string          | "tags-to-aggregate"     | Name of the topic where the events are published, to be aggregated    |
| `KAFKA_BOOTSTRAP_SERVERS`   | string:integer  | "localhost:9092"        | Address of the kafka broker                                           |
|                             |                 |                         |                                                                       |
| Only used by **aggregate-processor**: |       |                         |                                                                       |
| `KAFKA_GROUP`               | string          | "aggregate-processors"  | Id of the consumer group to to which the aggregate processor belongs  |
| `KAFKA_CONSUMER_ID`         | string          | "consumer"              | Id of the consumer                                                    |
| `KAFKA_POLL_TIMEOUT`        | integer         | 1000                    | number of milliseconds passed to the `KafkaConsumer.poll` method      |
|                             |                 |                         |                                                                       |
| Only used by **frontend**:  |                 |                         |                                                                       |
| `NUM_TAGS_TO_KEEP`          | integer         | 200                     | Maximum number of events to be stored per user                        |
| `DEFAULT_LIMIT`             | integer         | 200                     | Default number of events to be returned in a `/user_profiles` request |
| `FRONTEND_HOSTNAME`         | string          | "0.0.0.0"               | Name of the host of the frontend app                                  |
| `FRONTEND_PORT`             | integer         | 8080                    | Port at which frontend will be availible                              |
| `USE_LOGGER`                | TRUE or FALSE   | TURE                    | Disables the logger middleware when set to FALSE                      |
| `LOG_HEADERS`               | TRUE or FALSE   | FALSE                   | If set to TRUE, the app will log headers of requests and responses (ignored when USE_LOGGER=False) |
| `LOG_BODY`                  | TRUE or FALSE   | FALSE                   | If set to TRUE, the app will log bodies of requests and responses (ignored when USE_LOGGER=False) |







