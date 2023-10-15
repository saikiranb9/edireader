package com.atd.microservices.core.edireader.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.atd.microservices.core.edireader.configuration.KafkaConfigConstants;
import com.atd.microservices.core.edireader.domain.EDIReaderPayload;
import com.atd.utilities.kafkalogger.constants.AnalyticsContants;
import com.atd.utilities.kafkalogger.operation.KafkaAnalyticsLogger;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EDIReaderConsumerService {

	@Autowired
	private EDIProcessor ediProcessor;
	
	@Autowired
	private KafkaAnalyticsLogger serviceKafkaLogger;
	
	@Autowired
	private KafkaConfigConstants kafkaConfigConstants;
	
	@Autowired
	private EDIReaderUtil ediReaderUtil;
	
	@Value("${spring.application.name}")
	private String appName;
	
	@Autowired
	private EDIReaderMetrics ediReaderMetrics;

	@KafkaListener(topics = "${edireader.kafka.topic.inbound}", groupId = "group_edireader", containerFactory = "kafkaListenerContainerFactory")
	public void analyticsMessageListener(@Payload EDIReaderPayload ediReaderPayload) {
		try {
			log.debug("Recieved message: " + ediReaderPayload);
			try {
				serviceKafkaLogger.logObject(AnalyticsContants.MessageSourceType.MESSAGE_SOURCE_TYPE_CONSUMER,
						kafkaConfigConstants.KAFKA_TOPIC_INBOUND, appName, ediReaderPayload, ediReaderUtil.getTraceId(), "2xx");
			} catch (Exception e) {
				log.error("Error logging kafka messages to Analytics topic");
			}
			ediProcessor.process(ediReaderPayload);			
		} catch (Exception e) {
			log.error("Failed in processing kakfa message", e);
		}
	}
}
