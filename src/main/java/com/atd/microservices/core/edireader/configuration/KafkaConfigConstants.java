package com.atd.microservices.core.edireader.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
public class KafkaConfigConstants {
	@Value("${kafka.security.protocol}")
	public String SECURITY_PROTOCOL;

	@Value("${ssl.truststore.password}")
	public String SSL_TRUSTSTORE_PASSWORD;

	@Value("${ssl.truststore.location}")
	public String SSL_TRUSTSTORE_LOCATION;

	@Value("${kafka.bootstrap.server.url}")
	public String BOOTSTRAP_SERVER_URL;
	
	@Value("${edireader.kafka.topic.inbound}")
	public String KAFKA_TOPIC_INBOUND;
	
	@Value("${edireader.kafka.topic.outbound}")
	public String KAFKA_TOPIC_OUTBOUND;
}
