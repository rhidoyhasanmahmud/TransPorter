#!/bin/bash

set -e

javac cache.java
javac CacheManager.java
javac client.java
javac ClientHandler.java
javac server.java
javac snw_transport.java
javac tcp_transport.java
javac Transport.java

echo "Compilation completed successfully."
