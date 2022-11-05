## An example App in Cats Effects and FS2

This app calculates the Fibonacci sequence using Cats Effects, FS2, Protobuf, and KafkaProducer and -Consumer as Streams. An http4s server provides routes to initiate the process `localhost:8080/reset` and to monitor the progress `localhost:8080/latest`

While being an oddly complex way to attain Fibonacci, this code is otherwise quite a real world-like example of an FS2 app, up to the point of coordinating to itself via Kafka.

---

The code would otherwise support Java 17 but something in the typesafe config / pureconfig combination requires version 12.

---

###  Useful Kafka commands during development
- /bin/kafka-topics --bootstrap-server \<localhost:port> --list

- /bin/kafka-console-consumer --bootstrap-server \<localhost:port> --topic <error-topic> --from-beginning

- /bin/kafka-consumer-groups --bootstrap-server \<localhost:port> --describe --group fibonacci.consumers

- /bin/kafka-topics --describe --zookeeper \<localhost:port> --topic fibonacci.runner.topic
