

security.protocol=SASL_PLAINTEXT
sasl.mechanism=OAUTHBEARER

sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required clientId="app-ui" clientSecret="app-ui-secret" ;


sasl.login.callback.handler.class=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler

sasl.oauthbearer.token.endpoint.url=http://localhost:8080/realms/master/protocol/openid-connect/token



# not needed ?
sasl.oauthbearer.jwt.retriever.class=org.apache.kafka.common.security.oauthbearer.DefaultJwtRetriever




#sasl.oauthbearer.scope=openid cbc2





# Client credentials
#sasl.oauthbearer.client.credentials.client.id=app-ui
#sasl.oauthbearer.client.credentials.client.secret=app-ui-secret
#sasl.oauthbearer.scope=kafka-access


