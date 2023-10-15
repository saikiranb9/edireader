package com.atd.microservices.core.edireader;

import java.io.IOException;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.atd.microservices.core.edireader.domain.EDIReaderPayload;
import com.atd.microservices.core.edireader.service.EDIProcessor;
import com.atd.microservices.core.edireader.service.EDIReaderConsumerService;
import com.atd.utilities.kafkalogger.operation.KafkaAnalyticsLogger;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = {
		"kafka.bootstrap.server.url=null",
		"kafka.security.protocol=null",
		"edireader.kafka.topic.inbound=TEST_TOPIC",
		"edireader.kafka.topic.outbound=TEST_TOPICS",
		"ssl.truststore.password=null",
		"ssl.truststore.location=null",
		"kafka.analytic.topic=null",
		"edireader.ediAnalyticsDataUrl=null",
		"edireader.ediConfigUrl=null"})
public class EDIProcessorTest {
	
	@MockBean
	private EDIReaderConsumerService ediReaderConsumerService;
   
	@Autowired
	private EDIProcessor ediProcessor;
	
	@MockBean	
	private KafkaTemplate<String, String> ediMapperKafkaTemplate;
	
	@MockBean
	private KafkaAnalyticsLogger serviceKafkaLogger;

    @Test
    public void testServiceMetricsManagerFulfillOrder() throws InterruptedException, IOException {
    	Resource resource = new ClassPathResource("edieader_incoming_payload.json");    	
    	JsonMapper mapper = JsonMapper.builder().configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true).build(); 
    	
    	EDIReaderPayload incomingEDIPayload = mapper.readValue(resource.getFile(),
				EDIReaderPayload.class);
    	String jsonString = ediProcessor.convertEDIToJson(incomingEDIPayload.getData());
    	Assert.assertNotNull(jsonString);
    }

}