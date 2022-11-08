#!/bin/sh

(cd applications; docker-compose down)
./gradlew build
(cd applications; docker-compose up -d)
(cd publisher; ../gradlew run)
