#!/bin/bash

set -e

javac cache.java
javac CacheClientHandler.java
javac client.java
javac CacheStorageManager.java
javac server.java
javac snw_transport.java
javac tcp_transport.java
javac DataTransport.java

echo "Compilation completed successfully."
