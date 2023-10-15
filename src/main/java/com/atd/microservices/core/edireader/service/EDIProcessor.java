package com.atd.microservices.core.edireader.service;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import com.atd.microservices.core.edireader.configuration.KafkaConfigConstants;
import com.atd.microservices.core.edireader.domain.EDIConfig;
import com.atd.microservices.core.edireader.domain.EDIData;
import com.atd.microservices.core.edireader.domain.EDIMapperPayload;
import com.atd.microservices.core.edireader.domain.EDIReaderPayload;
import com.atd.microservices.core.edireader.exception.EDIReaderException;
import com.atd.microservices.core.edireader.webclients.EDIAnalyticsDataClient;
import com.atd.microservices.core.edireader.webclients.EDIConfigClient;
import com.atd.utilities.kafkalogger.constants.AnalyticsContants;
import com.atd.utilities.kafkalogger.operation.KafkaAnalyticsLogger;
import com.berryworks.edireader.json.fromedi.DomEditorHL;
import com.berryworks.edireader.json.fromedi.EdiToJson;
import com.berryworks.edireader.json.fromedi.JsonAdapter;
import com.berryworks.edireader.json.fromedi.JsonAdapterH;
import com.berryworks.edireader.json.toedi.JsonToEdi;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class EDIProcessor {

	@Autowired
	private KafkaConfigConstants kafkaConfigConstants;

	@Autowired
	@Qualifier("ediMapperKafkaTemplate")
	private KafkaTemplate<String, EDIMapperPayload> ediMapperKafkaTemplate;

	@Autowired
	private KafkaAnalyticsLogger serviceKafkaLogger;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private EDIAnalyticsDataClient ediAnalyticsDataClient;

	@Autowired
	private EDIConfigClient ediConfigClient;

	@Autowired
	private EDIReaderUtil ediReaderUtil;

	@Autowired
	private EDIJsonHeaderUtil ediHeaderUtil;

	@Autowired
	private EDIReaderMetrics ediReaderMetrics;

	@Value("${spring.application.name}")
	private String appName;

	public void process(EDIReaderPayload ediReaderPayload) {
		String jsonString = null;
		EDIMapperPayload ediMapperPayload = null;

		try {
			String ediData = ediReaderPayload.getData();
			log.debug("ediData: {}", ediData);

			jsonString = convertEDIToJson(ediData);
			log.debug("EDI json string: {}", jsonString);

			// Derive headers
			Map<String, String> headers = ediHeaderUtil.extractHeaderInfoFromEDIDoc(jsonString);
			ediReaderMetrics.increaseTotalIncomingIndividualMsgTypeCount(headers.get(EDIJsonHeaderUtil.KEY_TYPE));
			ediReaderMetrics.increaseTotalIncomingIndividualMsgSenderCount(headers.get(EDIJsonHeaderUtil.KEY_SENDERCODE));
			ediReaderMetrics.increaseTotalIncomingIndividualMsgReceiverCount(headers.get(EDIJsonHeaderUtil.KEY_RECEIVERCODE));
			// Create EDIMapperPayload from converted EDI Json, EDI Json headers
			ediMapperPayload = createEDIMapperPayload(jsonString, headers);
			if (jsonString != null) {
				// Get uuid of partner using their customer code
				String uuid = ediConfigClient.getEdiConfigBySenderCodeAndReceiverCode(ediMapperPayload.getSendercode(),
						ediMapperPayload.getReceivercode()).block().getUuid();
				
				// Push to EDIMAPPER Kafka Topic
				try {
					ediMapperKafkaTemplate.send(kafkaConfigConstants.KAFKA_TOPIC_OUTBOUND, ediMapperPayload);
					createEDIAnalyticsDataSuccess(uuid, jsonString, ediReaderPayload, ediMapperPayload);
				} catch (Exception e) {
					log.error("Exception in attempting to push data to kafka: " + e);
					createEDIAnalyticsDataError(uuid, ediReaderPayload, ediMapperPayload,
							new EDIReaderException("Error attempting to push analytics data."));
				}
			} else {
				createEDIAnalyticsDataError(null, ediReaderPayload, ediMapperPayload,
						new EDIReaderException("Got Null after converting the Raw EDI data"));
			}
		} catch (Exception e) {
			try {
				createEDIAnalyticsDataError(null, ediReaderPayload, ediMapperPayload, e);
			} catch (Exception e2) {
				log.error("Error saving the Error EDI data to EDIANALYTICS DB", e);
			}
		}

		// Push to Analytic Topic (Even if there's error)
		if (ediMapperPayload != null) {
			try {
				serviceKafkaLogger.logObject(AnalyticsContants.MessageSourceType.MESSAGE_SOURCE_TYPE_PRODUCER, appName,
						kafkaConfigConstants.KAFKA_TOPIC_OUTBOUND, ediMapperPayload, ediReaderUtil.getTraceId(), "2xx");
			} catch (Exception e) {
				log.error("Error logging kafka messages to Analytics topic");
			}
		}

	}

	public String convertEDIToJson(String ediData) {
		String json = null;
		try {
			String docType = null;
			try (StringReader reader = new StringReader(ediData); StringWriter writer = new StringWriter()) {
				EdiPreview preview = new EdiPreview(reader);
				docType = preview.getDocumentType();
			}
			
			if(StringUtils.equals(docType, "856")) {
				// New code
				EdiToJson ediToJson = new EdiToJson();
				StringReader jsonReader = new StringReader(ediData);
				ediToJson.setFormatting(true);
				StringWriter jsonWriter = new StringWriter();
		        JsonAdapter jsonAdapter= new JsonAdapterH(jsonWriter);
		        ediToJson.setJsonAdapter(new DomEditorHL(jsonAdapter));
		        ediToJson.asJson(jsonReader, jsonWriter);
		        json = jsonWriter.toString();
		        
			} else {						
				EdiToJson ediToJson = new EdiToJson();
				json = ediToJson.asJson(ediData);				
			}
		} catch (Exception e) {
			log.error("Error converting EDI data to Json", e);
			throw new EDIReaderException("Error converting EDI data to Json", e);
		}
		return json;
	}

	private void createEDIAnalyticsDataSuccess(String uuid, String jsonString, EDIReaderPayload ediReaderPayload,
			EDIMapperPayload ediMapperPayload) throws Exception {
		EDIData ediAnalyticsData = new EDIData();
		JsonNode processedEdiData = objectMapper.readTree(jsonString);

		ediAnalyticsData.setTraceId(ediReaderUtil.getTraceId());
		ediAnalyticsData.setLastProcessStage(appName);
		ediAnalyticsData.setStatus("2xx");
		ediAnalyticsData.setRawData(ediReaderPayload);
		ediAnalyticsData.setProcessedData(processedEdiData);
		ediAnalyticsData.setType(ediMapperPayload.getType());
		ediAnalyticsData.setVersion(ediMapperPayload.getVersion());
		ediAnalyticsData.setSendercode(ediMapperPayload.getSendercode());
		ediAnalyticsData.setReceivercode(ediMapperPayload.getReceivercode());
		ediAnalyticsData.setStandard(ediMapperPayload.getStandard());
		ediAnalyticsData.setSourceTopic(kafkaConfigConstants.KAFKA_TOPIC_INBOUND);
		ediAnalyticsData.setCustomerPO(ediHeaderUtil.getConsumerPO(jsonString));
		ediAnalyticsData.setUuid(uuid);

		ediAnalyticsDataClient.saveEDIData(Mono.just(ediAnalyticsData)).block();
	}

	private void createEDIAnalyticsDataError(String uuid, EDIReaderPayload ediReaderPayload,
			EDIMapperPayload ediMapperPayload, Exception e) throws Exception {
		EDIData ediAnalyticsData = new EDIData();

		ediAnalyticsData.setTraceId(ediReaderUtil.getTraceId());
		ediAnalyticsData.setLastProcessStage(appName);
		ediAnalyticsData.setStatus("5xx");
		ediAnalyticsData.setRawData(ediReaderPayload);
		ediAnalyticsData.setProcessedData(null);
		ediAnalyticsData.setUuid(uuid);
		if (ediMapperPayload != null) {
			ediAnalyticsData.setType(ediMapperPayload.getType());
			ediAnalyticsData.setVersion(ediMapperPayload.getVersion());
			ediAnalyticsData.setSendercode(ediMapperPayload.getSendercode());
			ediAnalyticsData.setReceivercode(ediMapperPayload.getReceivercode());
			ediAnalyticsData.setStandard(ediMapperPayload.getStandard());
		}
		ediAnalyticsData.setSourceTopic(kafkaConfigConstants.KAFKA_TOPIC_INBOUND);
		ediAnalyticsData
				.setErrorMessage(e.getMessage() + (e.getCause() != null ? e.getCause().getLocalizedMessage() : ""));
		ediAnalyticsDataClient.saveEDIData(Mono.just(ediAnalyticsData)).block();
	}

	private EDIMapperPayload createEDIMapperPayload(String ediJsonString, Map<String, String> ediJsonHeaders) {
		EDIMapperPayload ediMapperPayload = new EDIMapperPayload();
		ediMapperPayload.setSendercode(ediJsonHeaders.get(EDIJsonHeaderUtil.KEY_SENDERCODE));
		ediMapperPayload.setReceivercode(ediJsonHeaders.get(EDIJsonHeaderUtil.KEY_RECEIVERCODE));
		ediMapperPayload.setStandard(ediJsonHeaders.get(EDIJsonHeaderUtil.KEY_STANDARD));
		ediMapperPayload.setType(ediJsonHeaders.get(EDIJsonHeaderUtil.KEY_TYPE));
		ediMapperPayload.setVersion(ediJsonHeaders.get(EDIJsonHeaderUtil.KEY_VERSION));
		ediMapperPayload.setData(ediJsonString);
		return ediMapperPayload;
	}

	public String convertJsonToEDI(String jsonString) {
		String ediString = null;
		try (StringReader reader = new StringReader(jsonString); StringWriter writer = new StringWriter();) {
			JsonToEdi jsonToEdi = new JsonToEdi();
			jsonToEdi.asEdi(reader, writer);
			ediString = writer.toString();
		} catch (Exception e) {
			log.error("Error converting EDI data to Json", e);
			throw new EDIReaderException("Error converting EDI data to Json", e);
		}
		return ediString;
	}

}
