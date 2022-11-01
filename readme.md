/bin/kafka-topics --bootstrap-server localhost:9092 --list

/bin/kafka-console-consumer --bootstrap-server localhost:9092 --topic quotations --from-beginning

/bin/kafka-console-producer --bootstrap-server localhost:9092 --topic quotations

/bin/kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group consumer.groupId

/bin/kafka-topics --describe --zookeeper localhost:2181 --topic quotations