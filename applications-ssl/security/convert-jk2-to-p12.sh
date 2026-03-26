#!/bin/bash


keytool -importkeystore -srckeystore client-publisher.keystore.jks -srcstoretype JKS -srcstorepass broker_secret -destkeystore client-publisher.keystore.p12 -deststoretype PKCS12 -deststorepass broker_secret

keytool -importkeystore -srckeystore client-stream.keystore.jks -srcstoretype JKS -srcstorepass broker_secret -destkeystore client-stream.keystore.p12 -deststoretype PKCS12 -deststorepass broker_secret

keytool -importkeystore -srckeystore kafka.server.truststore.jks -srcstoretype JKS -srcstorepass broker_secret -destkeystore kafka.server.truststore.p12 -deststoretype PKCS12 -deststorepass broker_secret


