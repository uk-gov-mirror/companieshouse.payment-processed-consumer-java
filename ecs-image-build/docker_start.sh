#!/bin/bash
#
# Start script for payment-processed-consumer-java

PORT=3057
exec java -jar -Dserver.port="${PORT}" "payment-processed-consumer-java.jar"
