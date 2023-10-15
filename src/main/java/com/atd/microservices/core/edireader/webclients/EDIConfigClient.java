package com.atd.microservices.core.edireader.webclients;

import java.net.URI;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.atd.microservices.core.edireader.domain.EDIConfig;
import com.atd.microservices.core.edireader.exception.EDIReaderException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class EDIConfigClient {

	@Autowired
	private WebClient webClient;

	@Value("${spring.application.name}")
	private String applicationName;

	@Value("${edireader.ediConfigUrl}")
	private String ediConfigUrl;

	public Mono<EDIConfig> getEdiConfigBySenderCodeAndReceiverCode(String senderCode, String receiverCode) {
		try {
			return webClient.get().uri(URI.create(String.format(ediConfigUrl, senderCode, receiverCode)))
					.header("XATOM-CLIENTID", applicationName).retrieve()
					.onStatus(HttpStatus::isError,
							exceptionFunction -> Mono.error(
									new EDIReaderException("EDIReader returned error attempting to call EdiConfig")))
					.bodyToMono(EDIConfig.class);
		} catch (Exception e) {
			log.error("Error while querying EDI Config", e);
			return Mono.error(new EDIReaderException("Error while querying EDI Config", e));
		}
	}
}