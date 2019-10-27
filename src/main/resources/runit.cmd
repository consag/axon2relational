cd ../../..
java -cp target/axon2relational-0.2-SNAPSHOT.jar;lib\log4j-1.2.17.jar;lib\gson-2.8.6.jar nl.jacbeekers.AxonMain "https://vm400009570.nl.eu.abnamro.com:9443/api/login_check" "npa.dataqualityteam@nl.abnamro.com" "Ch@ngeM3N0w" "https://vm400009570.nl.eu.abnamro.com:9443/unison/v1/facet/_search" "attribute" 1000000 > attribute.log
