package com.atd.microservices.core.edireader.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EDIMapperPayload {

	private String type;
	private String standard;
	private String version;
	private String sendercode;
	private String receivercode;
	private String data;
}
	