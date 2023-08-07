package io.swagger.api;

import io.swagger.model.Email;
import io.swagger.model.ProviderType;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import org.epos.core.EmailSenderHandler;
import org.epos.router_framework.domain.Response;
import org.epos.router_framework.types.ServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import javax.validation.Valid;
import javax.validation.constraints.*;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2023-08-04T13:31:01.781679391Z[GMT]")
@RestController
public class SenderApiController extends ApiController implements SenderApi{

	@org.springframework.beans.factory.annotation.Autowired
	public SenderApiController(ObjectMapper objectMapper, HttpServletRequest request) {
		super(request);
		this.objectMapper = objectMapper;
		this.request = request;
	}

	private static final Logger log = LoggerFactory.getLogger(SenderApiController.class);

	private final ObjectMapper objectMapper;

	private final HttpServletRequest request;


	public ResponseEntity<Email> sendEmailPost(
			@NotNull @Parameter(in = ParameterIn.QUERY, description = "Id of the resource" , required=true,schema=@Schema()) @Valid @RequestParam(value = "id", required = true) String id,
			@NotNull @Parameter(in = ParameterIn.QUERY, description = "Contact point type" , required=true,schema=@Schema()) @Valid @RequestParam(value = "contactType", required = true) ProviderType contactType, 
			@Parameter(in = ParameterIn.DEFAULT, description = "", schema=@Schema()) @Valid @RequestBody Email body) {
		String accept = request.getHeader("Accept");
		String userEmail = request.getParameter("userEmail");
		if (accept != null && accept.contains("application/json")) {
			try {
				final Map<String, Object> requestParameters = new HashMap<String, Object>();
				if(StringUtils.isBlank(id) && StringUtils.isBlank(userEmail)) {
					return new ResponseEntity<Email>(HttpStatus.BAD_REQUEST);
				}

				requestParameters.put("id", id);
				requestParameters.put("type", contactType);
				requestParameters.put("email", userEmail);
				log.info(requestParameters.toString());

				redirectRequest(ServiceType.SENDER, requestParameters, body);

				return new ResponseEntity<Email>(objectMapper.readValue("{\n  \"bodyText\" : \"bodyText\",\n  \"subject\" : \"subject\"\n}", Email.class), HttpStatus.ACCEPTED);
			} catch (IOException e) {
				log.error("Couldn't serialize response for content type application/json", e);
				return new ResponseEntity<Email>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<Email>(HttpStatus.NOT_IMPLEMENTED);
	}

	private ResponseEntity<Email> redirectRequest(ServiceType service, Map<String, Object> requestParams,Email sendEmail) {

		Response response = doRequest(service, requestParams);

		System.out.println(response);
		System.out.println(response.getPayloadType());
		System.out.println(response.getPayloadAsPlainText().get());
		Map<String, Object> handlerResponse;
		try {
			handlerResponse = EmailSenderHandler.handle(response.getPayloadAsPlainText().get(),sendEmail, requestParams);
		} catch (UnsupportedEncodingException | MessagingException e) {
			return new ResponseEntity<Email>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<Email>(HttpStatus.ACCEPTED);
	}

}
