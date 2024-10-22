#!/bin/bash

set -e

echo "Compiling Transport.java..."
javac Transport.java

echo "Compiling TcpTransport.java..."
javac TcpTransport.java

echo "Compiling SnwTransport.java..."
javac SnwTransport.java

echo "Compiling Cache.java..."
javac Cache.java

echo "Compiling Client.java..."
javac Client.java

echo "Compiling Server.java..."
javac Server.java

echo "Compilation completed successfully."
