[![CI](https://github.com/dajudge/kafkaproxy/actions/workflows/build.yaml/badge.svg)](https://github.com/dajudge/kafkaproxy/actions/workflows/build.yaml)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/2842f83708864fd68a65bf0a82f32bb0)](https://www.codacy.com/gh/dajudge/kafkaproxy/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=dajudge/kafkaproxy&amp;utm_campaign=Badge_Grade)

# kafkaproxy
kafkaproxy is a reverse proxy for the wire protocol of Apache Kafka. 

Since the Kafka wire protocol publishes the set of available broker endpoints from the brokers to the clients during
client bootstrapping, the brokers must be configured to publish endpoints that are reachable from the client side. This
can be a cumbersome restriction in several different situations, such as:
*  Network topologies preventing direct access to the broker nodes
*  Multiple networks from which broker nodes should be reachable
*  DNS resolution restrictions when accessing TLS secured broker nodes
*  Using the sidecar pattern for TLS termination in Kubernetes 

This is where kafkaproxy comes into play and allows for transparent relaying of the Kafka wire protocol by rewriting
the relevant parts of the communication where the brokers publish the endpoint names - with user-configurable endpoints
where the the proxy instances can be reached.

# Run kafkaproxy in Docker
kafkaproxy is built to be run in a container. The released versions are available at [Docker Hub](https://hub.docker.com/r/dajudge/kafkaproxy).
## Command line
The following configuration parameters are mandatory:
* `KAFKAPROXY_HOSTNAME`: the hostname at which the proxy can be reached from the clients.
* `KAFKAPROXY_BASE_PORT`: the first port to be used by kafkaproxy.
* `KAFKAPROXY_BOOTSTRAP_SERVERS`: the comma separated list of initially mapped endpoints. This is usually the list of bootstrap brokers or a load balancer in front of the kafka brokers.

For example:
```
docker run \
    --net host \
    -e KAFKAPROXY_HOSTNAME=localhost \
    -e KAFKAPROXY_BASE_PORT=4000 \
    -e KAFKAPROXY_BOOTSTRAP_SERVERS=kafka:9092 \
    -d dajudge/kafkaproxy:0.0.15
``` 
*Note:* You will have to make the proxy ports (starting with `KAFKAPROXY_BASE_PORT` and incrementing from there) available from outside the container with `-p PORT:PORT` if you're not using `--net host`.

## Demonstration setup with `docker-compose`
If you have `docker-compose` installed you can try out kafkaproxy by using the demonstration setup provided in the
`example` directory. So clone the [kafkaproxy repo](https://github.com/dajudge/kafkaproxy) and run the following commands:

**Step 1:** Start kafka, zookeeper and kafkaproxy.
```
docker-compose -f example/docker-compose.yml up -d
```
Kafka will take a couple of seconds to fully start and become available.

**Step 2:** Create `my-test-topic`.
```
docker run --rm --net host -i confluentinc/cp-zookeeper:5.2.1 kafka-topics --create --topic my-test-topic --bootstrap-server localhost:4000 --partitions 1 --replication-factor 1
```

**Step 3:** Publish a message to `my-test-topic`.
```
echo "Hello, kafkaproxy" | docker run --rm --net host -i confluentinc/cp-zookeeper:5.2.1 kafka-console-producer --broker-list localhost:4000 --topic my-test-topic
```

**Step 4:** Consume to produced message from `my-test-topic`.
```
docker run --rm --net host -it confluentinc/cp-zookeeper:5.2.1 kafka-console-consumer --bootstrap-server localhost:4000 --topic my-test-topic --from-beginning --max-messages 1
```

**Cleanup:** Stop and remove the demonstration containers.
```
docker-compose -f example/docker-compose.yml rm -sf
```

**Explanation:** The `docker-compose.yml` file starts up a kafka broker (along with it's required zookeeper) that is
only available from within the docker network as `kafka1:9092`. The kafkaproxy is configured to
proxy this kafka instance as `localhost:4000` which is also mapped from outside the docker network.

# Configuration
kafkaproxy is configured using mostly environment variables and a broker map file in YAML format. The following
section describe the configuration options in detail.

## General configuration
kafkaproxy requires some general information to start. 

| Name                               | Default value | Destription
| ---------------------------------- | ------------- | -----------
| `KAFKAPROXY_HOSTNAME`              |               | The hostname of the proxy as seen by the clients.
| `KAFKAPROXY_BASE_PORT`             |               | The base of the ports to be used by the proxy. Each new required port is created by incrementing on top of the base port.
| `KAFKAPROXY_BIND_ADDRESS`          | `0.0.0.0`     | The address server sockets will bind to (both proxy ports and HTTP).
| `KAFKAPROXY_BOOTSTRAP_SERVERS`     |               | The comma separated list of initially mapped endpoints. This is usually the list of bootstrap brokers or a load balancer in front of the kafka brokers.
| `KAFKAPROXY_LOG_LEVEL`             | `INFO`        | The log level of the root logger. This must be a valid log level for [logback](http://logback.qos.ch/manual/configuration.html).
| `KAFKAPROXY_ENABLE_JSON_LOGGING`   | `false`       | Enable/disable json logging feature.
| `KAFKAPROXY_HTTP_PORT`             | `8080`        | The HTTP port the metrics REST endpoint will be exposed on. 
 
## Client SSL configuration
The client SSL configuration determines how the Kafka clients have to connect to kafkaproxy.
Configuration can be provided using the following environment variables:

| Name                                                 | Default value | Destription
| ---------------------------------------------------- | ------------- | -----------
| `KAFKAPROXY_CLIENT_SSL_ENABLED`                      | `false`       | Enables SSL encrypted communication between clients and kafkaproxy. 
| `KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_LOCATION`          |               | The filesystem location of the trust store to use. If no value is provided the JRE's default trust store will be used.
| `KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_REFRESH_SECS`      | 300           | The minimum amount if time between checks for updates of the trust store in seconds.
| `KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_PASSWORD`          |               | The password to access the trust store. Provide no value if the trust store is not password protected. Ignored when `KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_PASSWORD_LOCATION` is set.
| `KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_PASSWORD_LOCATION` |               | The filesystem location of the password to access the trust store. Overrides `KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_PASSWORD`.
| `KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_TYPE`              | `jks`         | The type of the trust store.
| `KAFKAPROXY_CLIENT_SSL_KEYSTORE_LOCATION`            |               | The filesystem location of the proxy's server key store. If no value is provided the JRE's default key store will be used.
| `KAFKAPROXY_CLIENT_SSL_KEYSTORE_REFRESH_SECS`        | 300           | The minimum amount if time between checks for updates of the proxy's server key store in seconds.
| `KAFKAPROXY_CLIENT_SSL_KEYSTORE_PASSWORD`            |               | The password to access the proxy's server key store. Provide no value if the key store is not password protected. Ignored when `KAFKAPROXY_CLIENT_SSL_KEYSTORE_PASSWORD_LOCATION` is set.
| `KAFKAPROXY_CLIENT_SSL_KEYSTORE_PASSWORD_LOCATION`   |               | The filesystem location of the password to access the proxy's server key store. Overrides `KAFKAPROXY_CLIENT_SSL_KEYSTORE_PASSWORD`.
| `KAFKAPROXY_CLIENT_SSL_KEY_PASSWORD`                 |               | The password to access the proxy's server key. Provide no value if the key is not password protected.
| `KAFKAPROXY_CLIENT_SSL_KEY_TYPE`                     | `jks`         | The type of the key store.
| `KAFKAPROXY_CLIENT_SSL_AUTH_REQUIRED`                | `false`       | Require a valid client certificate from clients connecting to the proxy.

## Kafka SSL configuration
The Kafka SSL configuration determines how kafkaproxy connects to the Kafka broker instances.
Configuration can be provided using the following environment variables:

| Name                                                | Default value | Destription
| --------------------------------------------------- | ------------- | -----------
| `KAFKAPROXY_KAFKA_SSL_ENABLED`                      | `false`       | Enables SSL encrypted communication kafkaproxy and the Kafka brokers.
| `KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_LOCATION`          |               | The filesystem location of the trust store to use. If no value is provided the JRE's default trust store will be used.
| `KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_REFRESH_SECS`      | 300           | The minimum amount if time between checks for updates of the trust store store in seconds.
| `KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_PASSWORD`          |               | The password to access the trust store. Provide no value if the trust store is not password protected. Ignored when `KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_PASSWORD_LOCATION` is set.
| `KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_PASSWORD_LOCATION` |               | The filesystem location of the password to access the trust store. Overrides `KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_PASSWORD`. 
| `KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_TYPE`              | `jks`         | The type of the trust store.
| `KAFKAPROXY_KAFKA_SSL_VERIFY_HOSTNAME`              | `true`        | Indicates if the hostnames of the Kafka brokers are validated against the SSL certificates they provide when connecting.
| `KAFKAPROXY_KAFKA_SSL_KEYSTORE_LOCATION`            |               | The filesystem location of the proxy's client key store. Required only when `KAFKAPROXY_KAFKA_SSL_CLIENT_CERT_STRATEGY` is set to `KEYSTORE`.
| `KAFKAPROXY_KAFKA_SSL_KEYSTORE_REFRESH_SECS`        | 300           | The minimum amount if time between checks for updates of the proxy client's key store in seconds.
| `KAFKAPROXY_KAFKA_SSL_KEYSTORE_PASSWORD`            |               | The password to access the proxy's client key store. Provide no value if the key store is not password protected. Ignored when `KAFKAPROXY_KAFKA_SSL_KEYSTORE_PASSWORD_LOCATION` is set.
| `KAFKAPROXY_KAFKA_SSL_KEYSTORE_PASSWORD_LOCATION`   |               | The filesystem location of the password to access the proxy's client key store. Overrides `KAFKAPROXY_KAFKA_SSL_KEYSTORE_PASSWORD`.
| `KAFKAPROXY_KAFKA_SSL_KEYSTORE_TYPE`                | `jks`         | The type of the key store.
| `KAFKAPROXY_KAFKA_SSL_KEY_PASSWORD`                 |               | The password to access the proxy's client key. Provide no value if the key is not password protected. Ignored when `KAFKAPROXY_KAFKA_SSL_KEY_PASSWORD_LOCATION` is set.
| `KAFKAPROXY_KAFKA_SSL_KEY_PASSWORD_LOCATION`        |               | The filesytem location of the password to access the proxy's client key. Overrides `KAFKAPROXY_KAFKA_SSL_KEY_PASSWORD`.

# Further Reading
*  [A Guide To The Kafka Protocol](https://cwiki.apache.org/confluence/display/KAFKA/A+Guide+To+The+Kafka+Protocol)
*  [Kafka protocol guide](http://kafka.apache.org/protocol.html)
