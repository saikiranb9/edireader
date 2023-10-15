package com.atd.microservices.core.edireader.service;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;

@Component
public class EDIReaderMetrics {

	MeterRegistry meterRegistry;

	public EDIReaderMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void increaseTotalIncomingIndividualMsgTypeCount(String type) {
		this.meterRegistry.counter(String.format(("edireader_total_incoming_individual_%s_edi_docs"), type)).increment();
	}

	public void increaseTotalIncomingIndividualMsgSenderCount(String senderCode) {
		this.meterRegistry.counter(String.format(("edireader_total_incoming_sender_%s_edi_docs"), senderCode)).increment();
	}

	public void increaseTotalIncomingIndividualMsgReceiverCount(String receiverCode) {
		this.meterRegistry.counter(String.format(("edireader_total_incoming_receiver_%s_edi_docs"), receiverCode)).increment();
	}
}

