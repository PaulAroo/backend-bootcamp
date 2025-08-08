# Scala Backend Bootcamp Ecommerce API

![Scala Version](https://img.shields.io/badge/Scala-2.13.16-red)
![SBT Version](https://img.shields.io/badge/SBT-1.10.7-blueviolet)
![Scala CI](https://github.com/icemc/backend-bootcamp/actions/workflows/scala.yml/badge.svg)

## 📖 Overview
The Scala Backend Bootcamp E-Commerce API is a full-featured backend application
built as part of a hands-on bootcamp program. It’s designed to give students
real-world experience developing production-grade backend applications using the Scala
programming language and the ZIO ecosystem.

This application simulates a modular e-commerce platform with the following
key features:

- Cart Module – Manage shopping carts, add/remove items, and handle quantities.
- Products Module – Manage product catalog, including listing, filtering, and details.
- Checkout Module – Process orders, calculate totals, and handle payment simulation.
- Auth Module – Secure authentication and authorization with JWT.
- Gallery Module – Manage product images and media.

## 🚀 Features & Tech Stack

This application is built using modern Scala backend practices:

- ZIO 2: Functional, type-safe, and asynchronous programming
  - zio-http (HTTP server)
  - zio-config (configuration management)
  - zio-test (testing)
- Tapir – Type-safe API endpoints
- Circe – JSON parsing and encoding
- JWT-Circe & STTP-OAuth2 – Authentication and OAuth 2.0 support
- MongoDB Connector – Persistent data storage
- Redis Connector / ZIO Cache – Caching layer
- Prometheus Connector – Application metrics
- SBT-Docker – Build and containerize with Docker
- Logback with zio-logging – Structured logging
- Scala formatting – scalafix, scalafmt

## 📂 Project Structure

```
modules/
│
├── application      # Main ZIO application entry point
├── cart             # Cart service logic
├── products         # Product catalog service
├── checkout         # Checkout & order processing
├── auth             # Authentication & authorization
├── gallery          # Product image/media management
├── it               # Integration tests
│
docker-compose.yml   # External dependencies for local development
grafana-dashboard.json # Example Grafana dashboard for metrics
```
This structure follows a Domain-Driven Design (DDD) modular architecture, enabling clean separation of concerns and making it easy to extend the system with new features.

## 💻 Getting Started with IntelliJ IDEA
To run this project locally using IntelliJ IDEA (Community Edition is enough),
follow these steps.

1. Install Java (JDK)
  - Scala runs on the JVM, so you need a JDK installed.
  - Recommended: Java 17 (LTS)
  - Download from: https://adoptium.net
  - Verify installation:
    java -version

2. Install SBT
  - SBT (Scala Build Tool) is required to build and run Scala projects.
  - Mac (Homebrew):
    brew install sbt
  - Ubuntu/Debian:
    ```sh
    sudo apt-get update
    sudo apt-get install sbt
    ```
    If those fail then visit [sbt's reference manual](https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Linux.html#Ubuntu+and+other+Debian-based+distributions) or the linux tab on [sbt's download page](https://www.scala-sbt.org/download). There you will find the necessary commands to manually add the official SBT repository to your system's package manager (`apt`).
  - Windows:
    Download from https://www.scala-sbt.org/download.html
  - Verify installation:
    sbt sbt-version

3. Install IntelliJ IDEA
  - Download: https://www.jetbrains.com/idea/download
  - Install the Scala Plugin:
    1. Open IntelliJ
    2. Go to File > Settings > Plugins
    3. Search for "Scala" and install it
    4. Restart IntelliJ

4. Open the Project in IntelliJ
  - Clone the repository:
    git clone https://github.com/icemc/backend-bootcamp.git
    cd backend-bootcamp
  - Open IntelliJ → File > Open → Select the project folder.
  - IntelliJ will detect build.sbt and automatically import dependencies.

5. Run the Application in IntelliJ
  - Make sure all external services (MongoDB, Redis, etc.) are running (See [docker-compose.yml](./docker-compose.yml) file):
    `docker-compose up`
  - In IntelliJ, open the "application" module, right-click the main class,
    and choose Run.
  - Or run from terminal:
    sbt application/run

## 🛠 Running the Application from Terminal

- With Hot-reload: To run the application with hot-reload enabled simply run `sbt start`. This will run the application
  on the port specified in application.conf appServer.port with hot-reload enabled
- Without Hot-reload: To run the application without hot-reload enabled simply run `sbt application/run`

## 📦 Building for Production (SBT assembly & Docker)

- To build the fat jar using sbt assembly run the command `sbt build` with your desired configurations set up
  in [`application.conf`](./modules/application/src/main/resources/application.conf)
- To build the docker image run `sbt build-docker` with your desired configurations set up
  in [`application.conf`](./modules/application/src/main/resources/application.conf). Make sure you change the docker
  user in [`build.sbt`](./build.sbt)

## 🤝 Contributing

This project is open to contributions be it through issues or pull request. Have a look at
our [`contribution guide`](./CONTRIBUTING.md) before you get started.

## 📜 License

[MIT](./LICENSE)