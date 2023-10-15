package com.atd.microservices.core.edireader.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.atd.microservices.core.edireader.exception.EDIReaderException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class EDIJsonHeaderUtil {

	public static String KEY_SENDERCODE = "sendercode";
	public static String KEY_RECEIVERCODE = "receivercode";
	public static String KEY_STANDARD = "standard";
	public static String KEY_VERSION = "version";
	public static String KEY_TYPE = "type";
	private static Pattern pattern = Pattern.compile("(^\\h*)|(\\h*$)");

	@Autowired
	private ObjectMapper objectMapper;

	public Map<String, String> extractHeaderInfoFromEDIDoc(String processedEDIJson) {
		String type, version, standard, senderCode, receiverCode = null;
		try {
			JsonNode rootNode = objectMapper.readTree(processedEDIJson);
			// Type
			JsonNode typeNode = rootNode
					.at("/interchanges/0/functional_groups/0/transactions/0/ST_01_TransactionSetIdentifierCode");
			if (typeNode != null && !typeNode.isMissingNode()) {
				type = trim(typeNode.textValue());
			} else {
				throw new EDIReaderException(
						"Type information(ST_01_TransactionSetIdentifierCode) not found in EDI data");
			}

			String senderPrefix = trim(rootNode.at("/interchanges/0/ISA_05_SenderQualifier").textValue());
			JsonNode senderCodeNode = rootNode.at("/interchanges/0/ISA_06_SenderId");
			String receiverPrefix = trim(rootNode.at("/interchanges/0/ISA_07_ReceiverQualifier").textValue());
			JsonNode receiverCodeNode = rootNode.at("/interchanges/0/ISA_08_ReceiverId");

			if (senderCodeNode != null && !senderCodeNode.isMissingNode()) {
				senderCode = senderPrefix + "-" + trim(senderCodeNode.textValue());
			} else {
				throw new EDIReaderException(
						"Type information(ISA_05_SenderQualifier / ISA_06_SenderId) not found in EDI data");
			}
			
			if (receiverCodeNode != null && !receiverCodeNode.isMissingNode()) {
				receiverCode = receiverPrefix + "-" + trim(receiverCodeNode.textValue());
			} else {
				throw new EDIReaderException(
						"Type information(ISA_07_ReceiverQualifier / ISA_08_ReceiverId) not found in EDI data");
			}

			// Version
			JsonNode versionNode = rootNode.at("/interchanges/0/functional_groups/0/GS_08_Version");
			if (versionNode != null && !versionNode.isMissingNode()) {
				version = trim(versionNode.textValue());
			} else {
				throw new EDIReaderException("Version information(GS_08_Version) not found in EDI data");
			}

			// Standard
			JsonNode standardNode = rootNode.at("/interchanges/0/functional_groups/0/GS_07_ResponsibleAgencyCode");
			if (standardNode != null && !standardNode.isMissingNode()) {
				String value = standardNode.textValue();
				standard = StringUtils.equals("X", trim(value)) ? "X12" : trim(value);
			} else {
				throw new EDIReaderException("Standard information(GS_07_ResponsibleAgencyCode) not found in EDI data");
			}
		} catch (JsonProcessingException e) {
			throw new EDIReaderException("Error while extracting header values from EDI Json", e);
		}
		Map<String, String> headers = new HashMap<>();
		headers.put(KEY_SENDERCODE, senderCode);
		headers.put(KEY_RECEIVERCODE, receiverCode);
		headers.put(KEY_STANDARD, standard);
		headers.put(KEY_VERSION, version);
		headers.put(KEY_TYPE, type);
		return headers;
	}

	private static String trim(String str) {
		return pattern.matcher(str).replaceAll("");
	}

	public String getConsumerPO(String ediJsonString) {
		String customerPO = null;
		try {
			JsonNode rootNode = objectMapper.readTree(ediJsonString);
			// Type
			JsonNode segmentsNode = rootNode.at("/interchanges/0/functional_groups/0/transactions/0/segments");

			JsonNode beg03Node = segmentsNode.findValue("BEG_03");
			if (beg03Node != null && beg03Node.isValueNode()) {
				customerPO = beg03Node.textValue();
			}
		} catch (Exception e) {
			throw new EDIReaderException("Error extracting Customer PO from payload", e);
		}
		return customerPO;
	}

}
