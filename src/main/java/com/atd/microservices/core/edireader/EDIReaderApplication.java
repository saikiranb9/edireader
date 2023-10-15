package com.atd.microservices.core.edireader;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import brave.Span.Kind;
import brave.baggage.BaggagePropagation;
import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


@SpringBootApplication(scanBasePackages = {"com.atd.utilities.kafkalogger", "com.atd.microservices.core.edireader"})
@EnableSwagger2
public class EDIReaderApplication {

	@Value("${spring.application.name}")
	private String appName;

	@Value("${env.host.url:#{null}}")
	private String envHostURL;

	public static void main(String[] args) {
		SpringApplication.run(EDIReaderApplication.class, args);
	}

	/*@Bean
	public TraceableExecutorService getExecutorService(BeanFactory beanFactory) {
		return new TraceableExecutorService(beanFactory,
        		Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()), "futureroutes");
	}*/
	
	@Bean
	public WebClient defaultWebClient() {
		return WebClient.create();
	}
	
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
	
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
	
	static final Propagation.Factory B3_FACTORY = B3Propagation.newFactoryBuilder()
			.injectFormat(Kind.PRODUCER, B3Propagation.Format.MULTI).build();

	@Bean
	BaggagePropagation.FactoryBuilder baggagePropagationFactoryBuilder() {
		return BaggagePropagation.newFactoryBuilder(B3_FACTORY);
	}
}
