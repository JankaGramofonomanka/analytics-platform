# About
A platform for an online retailer, like Amazon, Alibaba etc.
The platform has 3 use cases:
- It stores events (views and buys of products, with data about the product and the user)
- It provides the client with user profiles (lists of events performed by the user)
- It aggregates the data in 1-minute buckets, and provides the aggregates to the client.
  (eg. the client can request the total price or the total number of all products in category X, by producer Y, that were bought in a given time range.)

This is a final project at a university course, and the exact requirements for this project are availible here
(this includes enpoints, and data formats): https://github.com/RTBHOUSE/mimuw-lab/tree/main/project

# How it works
The project is intended to be a distributed application and it has the following components:
- frontend
- tag-processor
- a database
- a kafka broker

**frontend** is responsible for communication with the client.
It receives requests, queries the **database** to compute and return the answer requested by the client, it publishes the events to a **kafka topic**, to be stored and aggregated by the **tag-processor**.

**tag-processor** subscribes to the **kafka topic**, stores the events in the **database**, selects relevant aggregates to be updated and updates them.

::: mermaid
graph TD
    client        --> | events | frontend
    frontend      --> | profiles, aggregates | client

    database      --> | profiles, aggregates | frontend
    frontend      --> | events | kafka
    kafka         --> | events | tag-processor
    tag-processor --> | profiles, aggregates | database
    database      --> | profiles, aggregates | tag-processor
:::

# Run the app

## Note:
  The app uses environment variables, all of which have a default value, 
  which assumes your entire app is running on localhost. 
  Therefore If you are running the app locally, there is no need to specify the variables.
  The variables are described [here](#environment)

## locally
- run kafka:
  ```
  export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
  docker-compose -f kafka/src/main/docker/docker-compose.yml up
  ```

  create a topic:
  ```
  docker exec -it broker bash
  [appuser@broker ~]$ kafka-topics --bootstrap-server localhost:9092 --topic tags --create --partitions 10 --config retention.ms=900000
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

## REMOTELY
To run the app remotely you will need 10 machines, For convinience let's give them names:
- `DB-NODE-1`
- `DB-NODE-2`
- `KAFKA-HOST`
- `CLUSTER-MASTER`
- `CLUSTER-NODE-1`
- `CLUSTER-NODE-2`
- `CLUSTER-NODE-3`
- `CLUSTER-NODE-4`
- `CLUSTER-NODE-5`
- `CLUSTER-NODE-6`
  
`DB-NODE-1` and `DB-NODE-2` will need 3.25GB free memory and 8GB free disk space



### Build and push the images
To build and use the docker images you will need to specify your docker username.
ie. run this:
```
export DOCKER_USERNAME=<your-docker-username>
```

- frontend:
  ```
  docker build . \
      -t ${DOCKER_USERNAME}/analytics-platform-frontend \
      -f frontend/src/main/docker/Dockerfile
  
  docker push ${DOCKER_USERNAME}/analytics-platform-frontend
  ```

- tag-processor:
  ```
  docker build . \
      -t ${DOCKER_USERNAME}/analytics-platform-tag-processor \
      -f tag-processor/src/main/docker/Dockerfile

  docker push ${DOCKER_USERNAME}/analytics-platform-tag-processor
  ```

### Install and run aerospike
Choose a `PATH` on `DB-NODE-1` and `DB-NODE-2` where you will store the necessary files.
- on `DB-NODE-1` and `DB-NODE-2` install aerospike:
  
  Download the Aerospike Community Version installation package (You need this exact version):

  ```
  cd <PATH>
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

- on your local computer, copy the aerospike.conf files to `DB-NODE-1` and `DB-NODE-2`
  ```
  scp database/src/main/resources/aerospike.conf <USER>@<DB-NODE-1>:<PATH>/aerospike.conf
  scp database/src/main/resources/aerospike.conf <USER>@<DB-NODE-2>:<PATH>/aerospike.conf
  ```

- on `DB-NODE-1` and `DB-NODE-2` run aerospke:

  Open aerospike.conf and replace `<IP_ADDRESS_OF_THE_CURRENT_SERVER>` and 
  `<IP_ADDRESS_OF_THE_OTHER_SERVER>` with the ip's of respective machines
  ```
  cd <PATH>
  nano aerospike.conf
  ...
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

### Run kafka
Choose a `PATH` on `KAFKA-HOST` where you will store the necessary files.
- copy the `docker-compose.yml` file to `KAFKA-HOST`:
  ```
  scp kafka/src/main/docker/docker-compose.yml <USER>@<KAFKA-HOST>:<PATH>/docker-compose.yml
  ```

- on `KAFKA-HOST`:

  run kafka
  ```
  cd <PATH>
  echo KAFKA_BOOTSTRAP_SERVERS=<KAFKA-HOST>:9092 > .env
  sudo docker-compose up -d
  ```

  create the topic
  ```
  docker exec -it broker bash
  [appuser@broker ~]$ kafka-topics --bootstrap-server ${KAFKA_BOOTSTRAP_SERVERS} --topic tags --create --partitions 9 --config retention.ms=900000
  ```

### Set up a kubernetes cluster
- on `CLUSTER-MASTER`, `CLUSTER-NODE-<1-6>` install `kubeadm`, `kubelet`, `kubectl`
  ```
  sudo apt-get update
  sudo apt-get install -y apt-transport-https ca-certificates curl
  sudo curl -fsSLo /usr/share/keyrings/kubernetes-archive-keyring.gpg https://packages.cloud.google.com/apt/doc/apt-key.gpg
  echo "deb [signed-by=/usr/share/keyrings/kubernetes-archive-keyring.gpg] https://apt.kubernetes.io/ kubernetes-xenial main" | sudo tee /etc/apt/sources.list.d/kubernetes.list
  sudo apt-get update
  sudo apt-get install -y kubelet kubeadm kubectl
  sudo apt-mark hold kubelet kubeadm kubectl
  ```

- on `CLUSTER-MASTER` initialize the cluster
  ```
  sudo kubeadm init --pod-network-cidr=192.168.0.0/16
  ```

  This should output instructions that look like this:
  ```
  Your Kubernetes control-plane has initialized successfully!

  To start using your cluster, you need to run the following as a regular user:

    mkdir -p $HOME/.kube
    sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
    sudo chown $(id -u):$(id -g) $HOME/.kube/config

  You should now deploy a Pod network to the cluster.
  Run "kubectl apply -f [podnetwork].yaml" with one of the options listed at:
    /docs/concepts/cluster-administration/addons/

  You can now join any number of machines by running the following on each node
  as root:

    kubeadm join <control-plane-host>:<control-plane-port> --token <token> --discovery-token-ca-cert-hash sha256:<hash>
  ```

  Follow these instructions, that is copy or save the command:
  ```
  kubeadm join <control-plane-host>:<control-plane-port> --token <token> --discovery-token-ca-cert-hash sha256:<hash>
  ```

  Execute the following
  ```
  mkdir -p $HOME/.kube
  sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
  sudo chown $(id -u):$(id -g) $HOME/.kube/config
  ```

  Install a pod network:
  ```
  kubectl apply -f https://raw.githubusercontent.com/antrea-io/antrea/main/build/yamls/antrea.yml
  ```

- on `CLUSTER-NODE-<1-6>`:
  
  Join the cluster by executing the command you saved / copied after initializing the cluster
  ```
  kubeadm join <control-plane-host>:<control-plane-port> --token <token> --discovery-token-ca-cert-hash sha256:<hash>
  ```

### Deploy **frontend** and **tag-processor**
Choose a `PATH` on `CLUSTER-MASTER` where you will store the kubernetes-templates.
- on your local computer copy the kubernetes templates to `CLUSTER-MASTER`:
  ```
  scp frontend/src/main/kubernetes/frontend.yml           <USER>@<CLUSTER-MASTER>:<PATH>/
  scp tag-processor/src/main/kubernetes/tag-processor.yml <USER>@<CLUSTER-MASTER>:<PATH>/
  ```
  
- on `CLUSTER-MASTER`:
  
  Replace `<DOCKER-USERNAME>`, `<DB-NODE-1>` and `<KAFKA-HOST>` with the appropriate values in both templates
  ```
  cd <PATH>
  nano frontend.yml
  ...
  nano tag-processor.yml
  ...
  ```

  Deploy **frontend**:
  ```
  kubectl apply -f frontend.yml
  ```

  Deploy **tag-processor**:
  ```
  kubectl apply -f tag-processor.yml
  ```

  Execute the following:
  ```
  kubectl get services
  ```

  It should give an output similar to this:
  ```
  NAME               TYPE           CLUSTER-IP     EXTERNAL-IP   PORT(S)          AGE
  frontend-service   LoadBalancer   10.104.35.11   <pending>     8080:31673/TCP   6d23h
  kubernetes         ClusterIP      10.96.0.1      <none>        443/TCP          7d
  ```

  In the case abive, the port under which the service is availible is `31673`.
  That is, it is the `SERVICE-PORT` value in the column:
  ```
  PORT(S)
  8080:<SERVICE-PORT>/TCP
  443/TCP
  ```

  You should now be able to use the app by sending requests to `<CLUSTER-MASTER>:<SERVICE-PORT>`
  (in the case above, to `<CLUSTER-MASTER>:31673`).

  


  

  

## Environment

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







