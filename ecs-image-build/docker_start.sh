#!/bin/bash
#
# Start script for payment-processed-consumer-java

PORT=8081
exec java -jar -Dserver.port="${PORT}" "payment-processed-consumer-java.jar"
