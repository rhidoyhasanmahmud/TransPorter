#!/bin/bash

set -e

echo "Compiling Transport.java..."
javac Transport.java

echo "Compiling TcpTransport.java..."
javac tcp_transport.java

echo "Compiling SnwTransport.java..."
javac snw_transport.java

echo "Compiling Cache.java..."
javac cache.java

echo "Compiling Client.java..."
javac client.java

echo "Compiling Server.java..."
javac server.java

echo "Compilation completed successfully."
