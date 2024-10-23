#!/bin/bash

set -e

javac CacheService.java
javac Transport.java
javac TcpTransport.java
javac SnwTransport.java
javac Server.java
javac Client.java

echo "Compilation completed successfully."
