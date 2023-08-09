package org.epos.core;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.swagger.model.Email;


public class EmailSenderHandler {


	private static final Logger LOGGER = LoggerFactory.getLogger(EmailSenderHandler.class);

	private static final String PARAMS = "params";

	private static Gson gson = new Gson();



	public static Map<String, Object> handle(String payload,Email sendEmail, Map<String, Object> requestParams) throws MessagingException, UnsupportedEncodingException {

		LOGGER.debug(payload);

		JsonObject payObj = gson.fromJson(payload, JsonObject.class);

		JsonArray mails = payObj.get("emails").getAsJsonArray();

		System.out.println(mails.toString());

		System.out.println("SSLEmail Start");

		// properties
		Properties props = new Properties();
		{
			props.setProperty("mail.smtp.auth", "true");
			props.setProperty("mail.smtp.host", System.getenv("MAIL_HOST"));
			props.setProperty("mail.smtp.socketFactory.port", "465");
			props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			props.setProperty("mail.smtp.port", "587"); //SMTP Port
		}

		Authenticator auth = new Authenticator() {
			//override the getPasswordAuthentication method
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication( System.getenv("MAIL_USER"), System.getenv("MAIL_PASSWORD"));
			}
		};
		
		if(System.getenv("ENVIRONMENT_TYPE").equals("production")) {
			for(JsonElement email : mails) {
				System.out.println("Creating a new session");
				Session session = Session.getDefaultInstance(props, auth);
				System.out.println("New session created, sending email");
				org.epos.api.utility.EmailUtil.sendEmail(session, email.getAsString(),sendEmail.getSubject(), "From: "+requestParams.get("email").toString()+"\n"
						+ sendEmail.getBodyText());
				System.out.println("End session");
			}
		}else {
			String[] devMails =System.getenv("DEV_EMAILS").split(";");

			for(String email : devMails) {
				System.out.println("Creating a new session");
				Session session = Session.getDefaultInstance(props, auth);
				System.out.println("New session created, sending email");
				org.epos.api.utility.EmailUtil.sendEmail(session, email,sendEmail.getSubject(), "From: "+requestParams.get("email").toString()+"\n"
						+ sendEmail.getBodyText());
				System.out.println("End session");
			}
		}

		



		return new HashMap<String, Object>();
	}

}
