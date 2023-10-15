package com.atd.microservices.core.edireader.domain;

import java.time.ZonedDateTime;

import com.atd.microservices.core.edireader.domain.deserializer.JsonDateTimeDeserializer;

import lombok.Data;

@Data
public class EDIReaderPayload {

	private String data;
	
	private String timestamp;

}
