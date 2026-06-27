#!/bin/bash
PORT=${1:-6000}
echo "Starting Chat Server on port $PORT ..."
java -cp out server.ChatServer $PORT
