/bin/kafka-topics --bootstrap-server localhost:9092 --list

/bin/kafka-console-consumer --bootstrap-server localhost:9092 --topic fibonacci.runner.topic --from-beginning

/bin/kafka-console-producer --bootstrap-server localhost:9092 --topic fibonacci.runner.topic

/bin/kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group fibonacci.consumers

/bin/kafka-topics --describe --zookeeper localhost:2181 --topic fibonacci.runner.topic


something abiout tyoesafe config / oureconfig contaminates the system if you build against Java 17 it can only be reset by building against 12 after which it may also be built against 14