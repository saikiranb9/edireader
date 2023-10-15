package com.atd.microservices.core.edireader.domain;

import java.util.UUID;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EDIData {
	private String id;
	@NotNull
	private String traceId;
	private Object rawData;
	private Object processedData;
	@NotNull
	private String lastProcessStage;
	private String errorMessage;
	private String status;
	private String sourceTopic;
	private String type;
	private String version;
	private String sendercode;
	private String receivercode;
	private String standard;
	private String customerPO;
	@NotNull
	private String uuid;
}
