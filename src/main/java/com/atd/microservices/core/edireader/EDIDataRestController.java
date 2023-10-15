package com.atd.microservices.core.edireader;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebExchangeBindException;

import com.atd.microservices.core.edireader.domain.ErrorDetails;
import com.atd.microservices.core.edireader.exception.EDIReaderException;
import com.atd.microservices.core.edireader.service.EDIProcessor;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(path="/")
public class EDIDataRestController {
	
	@Autowired
	private EDIProcessor ediProcessor;
	
	@ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(
            value = "Create Json representaion of a give EDI document",
            notes = "JSON Supported", response = String.class
    )
    @ApiResponses({
			@ApiResponse(code = 400, message = "Fields are with validation errors", response = ErrorDetails.class),
			@ApiResponse(code = 404, message = "Data not found for given vendor products", response = ErrorDetails.class),
			@ApiResponse(code = 406, message = "Request not acceptable for EDI Data", response = ErrorDetails.class),
			@ApiResponse(code = 424, message = "Request not processed due to failed dependecy EDI Data", response = ErrorDetails.class)
    })
	@PostMapping(value = "/editojson", produces = { "application/json" })
	public String createJsonFromEDI(
			@ApiParam(value = "EDI Json", required = true) @RequestBody String ediString) {
		return ediProcessor.convertEDIToJson(ediString);
	}
	
	@ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(
            value = "Create EDI document from the the given json",
            notes = "JSON Supported", response = String.class
    )
    @ApiResponses({
			@ApiResponse(code = 400, message = "Fields are with validation errors", response = ErrorDetails.class),
			@ApiResponse(code = 404, message = "Data not found for given vendor products", response = ErrorDetails.class),
			@ApiResponse(code = 406, message = "Request not acceptable for EDI Data", response = ErrorDetails.class),
			@ApiResponse(code = 424, message = "Request not processed due to failed dependecy EDI Data", response = ErrorDetails.class)
    })
	@PostMapping(value = "/jsontoedi", produces = { "application/text" })
	public String createRawEDIFromJson(
			@ApiParam(value = "EDI Json", required = true) @RequestBody String jsonString) {
		return ediProcessor.convertJsonToEDI(jsonString);
	}
	
	@ExceptionHandler({Exception.class,RuntimeException.class,Throwable.class})
	public final ResponseEntity<ErrorDetails> handleAllExceptions(Exception ex) {
		ErrorDetails errorDetails = new ErrorDetails(new Date(), ex.getMessage(), "");
		HttpStatus responseCode = HttpStatus.INTERNAL_SERVER_ERROR;
		
		if (ex instanceof EDIReaderException) {
			responseCode = HttpStatus.INTERNAL_SERVER_ERROR;
		} else if (ex instanceof WebExchangeBindException) {
			responseCode = HttpStatus.BAD_REQUEST;
		}
		log.error("Response Code: {}, Message: {}, {}", responseCode.value(), ex.getMessage(), "");
		return new ResponseEntity<>(errorDetails, responseCode);
	}
}
