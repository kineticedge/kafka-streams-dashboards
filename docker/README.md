# Docker Image

* This image is based on `eclipse-temurin:17-jdk-jammy` based image.

* The gradle build pulls in jar files for the libraries and their dependencies, into `build/runtime`. This is an optimization
step to improve startup time of the applications, especially since the `rocksDB` jar file is rather large and does take time
to unzip. 

  * If `kafka-clients` or `kafka-streams` is updated, run `../gradlew build -Pforce-docker=true` to ensure this image is also updated.

  * Additional networking tools added to aid in application health checks and exploring latency delays with Linux's traffic controller.

  * `entrypoint.sh` will untar the application distribution. The tar file created by *gradle assembly` is mounted at /app.tar, and
  it will extract that and then move the runtime libraries that was built within the container into the application's lib directory.

  * Additional work done in the `entrypoint.sh` to establish group instance ID, to aid in the use of static membership within the application.
  