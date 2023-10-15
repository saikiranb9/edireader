package com.atd.microservices.core.edireader.configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer2;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.atd.microservices.core.edireader.domain.EDIMapperPayload;
import com.atd.microservices.core.edireader.domain.EDIReaderPayload;

@EnableKafka
@Configuration
public class KafkaConfiguration {
	
	@Value("${spring.application.name:creditcardtransactions}")
	private String applicationName;

	@Autowired
	private KafkaConfigConstants kafkaConfigConstants;

	public ConsumerFactory<String, EDIReaderPayload> consumerFactory() {
		Map<String, Object> config = new HashMap<>();
		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfigConstants.BOOTSTRAP_SERVER_URL);
		config.put(ConsumerConfig.GROUP_ID_CONFIG, "group_edireader");
		config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");		
		config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer2.class);
		config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer2.class);
		config.put(ErrorHandlingDeserializer2.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
		config.put(ErrorHandlingDeserializer2.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
		config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.atd.microservices.core.edireader.domain.EDIReaderPayload");
		config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
		config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
		
		config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, kafkaConfigConstants.SECURITY_PROTOCOL);
		if (kafkaConfigConstants.SSL_TRUSTSTORE_LOCATION != null
				&& !StringUtils.isEmpty(kafkaConfigConstants.SSL_TRUSTSTORE_LOCATION)) {
			config.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, kafkaConfigConstants.SSL_TRUSTSTORE_LOCATION);
		}
		config.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, kafkaConfigConstants.SSL_TRUSTSTORE_PASSWORD);
		config.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");

		return new DefaultKafkaConsumerFactory<>(config);		
	}	

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, EDIReaderPayload> kafkaListenerContainerFactory() {
		ConcurrentKafkaListenerContainerFactory<String, EDIReaderPayload> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory());
		return factory;
	}
	
	@Bean
    public ProducerFactory<String, EDIMapperPayload> producerFactory() {
		Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfigConstants.BOOTSTRAP_SERVER_URL);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.CLIENT_ID_CONFIG, applicationName);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put("security.protocol", kafkaConfigConstants.SECURITY_PROTOCOL);
        if (kafkaConfigConstants.SSL_TRUSTSTORE_LOCATION != null && !StringUtils.isBlank(kafkaConfigConstants.SSL_TRUSTSTORE_LOCATION)) {
            config.put("ssl.truststore.location", kafkaConfigConstants.SSL_TRUSTSTORE_LOCATION);
        }
        config.put("ssl.truststore.password", kafkaConfigConstants.SSL_TRUSTSTORE_PASSWORD);
        config.put("ssl.endpoint.identification.algorithm", null);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean("ediMapperKafkaTemplate")
    public KafkaTemplate<String, EDIMapperPayload> ediMapperKafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

}
