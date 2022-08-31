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
- tag-processor
- a database
- a kafka broker

**frontend** is responsible for communication with the client.
It receives requests, queries the **database** to compute and return the answer requested by the client, it publishes the events to a **kafka topic**, to be stored and aggregated by the **tag-processor**.

**tag-processor** subscribes to the **kafka topic**, stores the events in the **database**, selects relevant aggregates to be updated and updates them.

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
  [appuser@broker ~]$ kafka-topics --bootstrap-server localhost:9092 --topic tags --create --partitions 10 --config retention.ms=300000
  ```

- run database:
  ```
  docker-compose -f database/src/main/docker/docker-compose.yml up
  ```

- run frontend:
  ```
  sbt "project frontend" "~reStart"
  ```

- run tag-processor:
  ```
  sbt "project tagProcessor" "~reStart"
  ```
### with docker
To build and use the docker images you will need to specify your docker username.
ie. run this (both locally and on remote machines if relevant):
```
export DOCKER_USERNAME=<your-docker-username>
```

- build the images
  - frontend:
    ```
    docker build . \
        -t ${DOCKER_USERNAME}/analytics-platform-frontend \
        -f frontend/src/main/docker/Dockerfile
    ```

  - tag-processor:
    ```
    docker build . \
        -t ${DOCKER_USERNAME}/analytics-platform-tag-processor \
        -f tag-processor/src/main/docker/Dockerfile
    ```

- run the app locally
  ```
  docker-compose up
  ```

- run the app remotely

  - To run the app remotely you will 2 machines, where you will host your database nodes, 
    1 machine where you will run the kafka broker, and one or more machines to run frontend and tag-processor.
    
    You will adittionally need to specify the host of one of the database nodes and the address of the kafka broker.
    
    Add the following lines to `.env` (remember, you also need `DOCKER_USERNAME`)
    ```
    DOCKER_USERNAME=<your-docker-username>
    AEROSPIKE_HOSTNAME=<host-of-the-database-node>
    KAFKA_BOOTSTRAP_SERVERS=<address-of-kafka-broker>
    ```

    choose a `PATH` (can be different for all machines) on your remote machines and copy the `.env` file there
    ```
    scp .env <USER>@<KAFKA-HOST>:<PATH>/.env
    scp .env <USER>@<FRONTEND-HOST>:<PATH>/.env
    scp .env <USER>@<TAG-PROC-HOST>:<PATH>/.env
    ```
  
  - push the images (run the commands in separate terminals to speed things up):
    ```
    docker push ${DOCKER_USERNAME}/analytics-platform-frontend
    docker push ${DOCKER_USERNAME}/analytics-platform-tag-processor
    ```
  
  - Copy the aerospike.conf files to your remote machines:
    ```
    scp database/src/main/resources/aerospike.conf <USER>@<DATABASE-NODE-1-HOST>:<PATH>/aerospike.conf
    scp database/src/main/resources/aerospike.conf <USER>@<DATABASE-NODE-2-HOST>:<PATH>/aerospike.conf
    ```

  - Copy the docker-compose files to your remote machines:
    ```
    scp kafka/src/main/docker/docker-compose.yml          <USER>@<KAFKA-HOST>:<PATH>/docker-compose.yml
    scp frontend/src/main/docker/docker-compose.yml       <USER>@<FRONTEND-HOST>:<PATH>/docker-compose.yml
    scp tag-processor/src/main/docker/docker-compose.yml  <USER>@<TAG-PROC-HOST>:<PATH>/docker-compose.yml
    ```

  - On your remote database machines:
    
    

    - Install aerospike:

      Download the Aerospike Community Version installation package:

      ```
      wget -O aerospike.tgz https://download.aerospike.com/download/server/5.7.0.16/artifact/ubuntu20
      ```

      Install the server:

      ```
      tar xzvf aerospike.tgz
      cd aerospike-server-community-5.7.0.16-ubuntu20.04/
      sudo ./asinstall
      ```

      Create the logging directory:

      ```
      sudo mkdir /var/log/aerospike
      ```

    - Run aerospke:
    
      Open aerospike.conf and replace `<IP_ADDRESS_OF_THE_CURRENT_SERVER>` and 
      `<IP_ADDRESS_OF_THE_OTHER_SERVER>` with the ip's of respective machines
      ```
      cd <PATH>
      nano aerospike.conf
      <...>
      ```

      Copy the Aerospike server configuration file:
      ```
      sudo cp aerospike.conf /etc/aerospike/
      ```

      Run the server on both nodes and verify the status:

      ```
      sudo systemctl start aerospike
      sudo systemctl status aerospike
      ```


  - On the remote kafka machines:
    
    run kafka:
    ```
    cd <PATH>
    sudo docker-compose up
    ```

    create a topic:
    ```
    docker exec -it broker bash
    [appuser@broker ~]$ kafka-topics --bootstrap-server ${KAFKA_BOOTSTRAP_SERVERS} --topic tags --create --partitions 10 --config retention.ms=300000
    
    ```

  - On your remote frontend machine:

    pull and run the image
    ```
    cd <PATH>
    docker pull <YOUR-DOCKER-USERNAME>/analytics-platform-frontend:latest
    sudo docker-compose up
    ```

  - On your remote tag-processor machine:

    pull and run the image
    ```
    cd <PATH>
    docker pull <YOUR-DOCKER-USERNAME>/analytics-platform-tag-processor:latest
    sudo docker-compose up
    ```

- run the app remotely with multiple replicas
  
  - To replicate tag-processor, just run more of them
    You might want to give each tag-processor a unique consumer id.
    To do that modify the field `KAFKA_CONSUMER_ID` in `docker-compose.yaml`.
  
  - To replicate frontend, you will need a loadbalancer.
    To build a loadbalancer, you need to manually add server addresses at which your frontend replicas are availible to  `loadbalancer/src/main/resources/haproxy.cfg`, you can find instructions on how to do that there.

    Next, build and push a docker image of the loadbalancer:
    ```
    docker build . \
      -t ${DOCKER_USERNAME}/analytics-platform-loadbalancer \
      -f loadbalancer/src/main/docker/Dockerfile
    
    docker push ${DOCKER_USERNAME}/analytics-platform-loadbalancer
    ```

    Next on your remote loadbalancer machine pull the image and run it:
    ```
    docker pull <YOUR-DOCKER-USERNAME>/analytics-platform-loadbalancer:latest
    docker run --network=host --privileged <YOUR-DOCKER-USERNAME>/analytics-platform-loadbalancer

    ```

    Now you can run frontends on the addresses you specified in `loadbalancer/src/main/resources/haproxy.cfg`,
    and direct requests to `<LOADBALANCER-HOST>:8080`

### Environment

The app uses the following environment variables:

| Variable                          | Format          | Default Value     | Description                                                         |
| --------------------------------- | --------------- |------------------ | ------------------------------------------------------------------- |
| `AEROSPIKE_HOSTNAME`              | string          | "localhost"       | Name of the host of the database                                    |
| `AEROSPIKE_PORT`                  | integer         | 3000              | Port at which the database is availible                             |
| `AEROSPIKE_PROFILES_NAMESPACE`    | string          | "profiles"        | Aerospike namespace in which the profiles are stored                |
| `AEROSPIKE_AGGREGATES_NAMESPACE`  | string          | "aggregates"      | Aerospike namespace in which the aggregates are stored              |
| `AEROSPIKE_PROFILES_BIN`          | string          | "profile"         | Name of the bin of a record where profiles are stored               |
| `AEROSPIKE_COMMIT_LEVEL`          | MASTER / ALL    | ALL               | the queries to the database will be successfull whan committed on ALL nodes / MASTER node |
| `AEROSPIKE_GENERATION_POLICY`     | EQ / GT / NONE  | EQ                | the queries to the database will be succesfull if the expected record generation is equal to (EQ) / greater then (GT) the actual generation / the queries will be always successfull (NONE) |
| `AEROSPIKE_BUCKETS_PER_KEY`       | integer         | 60                | when set to `N`, `N` aggregated buckets will be stored under one key (each bucket in a separate bin). The bigger the `N`, the less memory, should be used by the database. More speciffically, the key will be determined by dividing the "minutes of hour" field of the bucket by `N`, this means any number above 60 will give the same result as 60, and any number between 30 and 59 should result in the same memory usage as 30. |
| `KAFKA_TOPIC`                     | string          | "tags"            | Name of the topic where the events are published, to be aggregated  |
| `KAFKA_BOOTSTRAP_SERVERS`         | string:integer  | "localhost:9092"  | Address of the kafka broker                                         |
|                                   |                 |                   |                                                                     |
| Only used by **tag-processor**:   |                 |                   |                                                                     |
| `NUM_TAGS_TO_KEEP`        | integer         | 200               | Maximum number of events to be stored per user                          |
| `KAFKA_GROUP`             | string          | "tag-processors"  | Id of the consumer group to to which the tag processor belongs          |
| `KAFKA_CONSUMER_ID`       | string          | "consumer"        | Id of the consumer                                                      |
| `KAFKA_POLL_TIMEOUT`      | integer         | 1000              | number of milliseconds passed to the `KafkaConsumer.poll` method        |
| `KAFKA_MAX_POLL_RECORDS`  | integer         | 500               | maximum number ofrecords that can be sent to the tag-processor at once  |
|                           |                 |                   |                                                                         |
| `MAX_PARALLEL_WRITES`     | integer         | 6                 | number of parrallel requests to the database                            |
| Only used by **frontend**:  |               |           |                                                                                                     |
| `DEFAULT_LIMIT`             | integer       | 200       | Default number of events to be returned in a `/user_profiles` request                               |
| `FRONTEND_HOSTNAME`         | string        | "0.0.0.0" | Name of the host of the frontend app                                                                |
| `FRONTEND_PORT`             | integer       | 8080      | Port at which frontend will be availible                                                            |
| `USE_LOGGER`                | TRUE / FALSE  | TURE      | Disables the logger middleware when set to FALSE                                                    |
| `LOG_HEADERS`               | TRUE / FALSE  | FALSE     | If set to TRUE, the app will log headers of requests and responses (ignored when USE_LOGGER=False)  |
| `LOG_BODY`                  | TRUE / FALSE  | FALSE     | If set to TRUE, the app will log bodies of requests and responses (ignored when USE_LOGGER=False)   |







