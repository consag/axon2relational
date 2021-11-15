cd ../../..
java -cp target/axon2relational-0.2-SNAPSHOT.jar;lib\log4j-1.2.17.jar;lib\gson-2.8.6.jar nl.jacbeekers.AxonMain "https://<HOSTNAME>:9443/api/login_check" "<USERNAME>" "<CLEAR_TEXT_PASSWORD" "https://<HOSTNAME>:9443/unison/v1/facet/_search" "attribute" 1000000 > attribute.log
