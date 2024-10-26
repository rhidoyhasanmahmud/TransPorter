
# File Transfer System with Caching and Protocol Abstraction


## Overview

This project implements a robust and scalable **File Transfer System** in Java, incorporating a **Cache Service**, a **Server**, and a **Client**. The system supports multiple communication protocols through a flexible **Transport** abstraction, currently including **TCP** and **SNW** (Simple Network Wrapper).

Key design principles adhered to in this project include the **SOLID** principles and **clean code** standards, ensuring maintainability, scalability, and ease of extension.

## Features

- **Client-Server Architecture**: Facilitates file upload (`put`) and download (`get`) operations between clients and the server.
- **Caching Mechanism**: Enhances performance by storing frequently accessed files in a cache service.
- **Protocol Abstraction**: Supports multiple communication protocols (TCP and SNW) through a flexible Transport interface.
- **Concurrency Handling**: Utilizes thread pools to handle multiple client connections simultaneously.
- **Robust Error Handling**: Provides meaningful error messages and ensures proper resource management.
- **Modular Design**: Separation of concerns across different classes and interfaces for better maintainability.


## Architecture

![Architecture Diagram](architecture.png)

*Figure: High-level architecture of the File Transfer System.*

### Components

1. **Client**
   - Handles user interactions and commands.
   - Communicates with the server using the selected transport protocol.

2. **Server**
   - Manages client connections.
   - Handles file storage and retrieval.
   - Interacts with the Cache Service to optimize file access.

3. **Cache Service**
   - Stores and retrieves cached files to speed up file access.
   - Acts as an intermediary between the server and the storage backend.

4. **Transport Interface**
   - Defines the contract for communication protocols.
   - Implemented by `tcp_transport` and `snw_transport` for TCP and SNW protocols respectively.

5. **CacheManager**
   - Manages cache operations such as storing and retrieving files.

6. **ClientHandler**
   - Processes individual client requests on the server side.

## Prerequisites

- **Java Development Kit (JDK) 8 or higher**
- **Git** (for cloning the repository)

## Installation

1. **Clone the Repository**

   ```bash
   git clone https://github.com/rhidoyhasanmahmud/TransPorter.git
   cd TransPorter
   ```

2. **Compile the Source Code**

   Navigate to the project directory and compile the Java source files:

   ```bash
   ./compile.sh
   ```

## Command-line Inputs

The system can be configured using command-line arguments when starting the Server, Cache, and Client. Below are the configurable parameters for each component.

When starting your client, server, and cache, you need to specify several command-line inputs as follows:

- **Server**: Takes as inputs (1) a port number on which to run and (2) a transport protocol. Example commands:

```bash
java server 10000 tcp 
java server 10000 snw
```

- **Cache**: Takes as inputs (1) a port number on which to run, (2) server IP, (3) server port, and (4) transport protocol. Example commands:

```bash
java cache 20000 localhost 10000 tcp 
java cache 20000 localhost 10000 snw
```

- **Client**: Takes as inputs (1) the server IP, (2) the server port, (3) the cache IP, (4) the cache port, and (5) the transport protocol. 

```bash
java client localhost 10000 localhost 20000 tcp
java client localhost 10000 localhost 20000 snw
```