#!/bin/bash
HOST=${1:-localhost}
PORT=${2:-6000}
echo "Connecting to $HOST:$PORT ..."
java -cp out client.ChatClient $HOST $PORT
