package org.epos.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.swing.event.ListSelectionEvent;
import javax.xml.bind.DatatypeConverter;

import org.epos.api.utility.EmailUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.swagger.model.Email;
import okhttp3.Credentials;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class EmailSenderHandler {


	private static final Logger LOGGER = LoggerFactory.getLogger(EmailSenderHandler.class);

	private static final String PARAMS = "params";

	private static Gson gson = new Gson();

	private static String subjectForwardedMessage = " | Receipt Confirmation";

	private static String forwardedMessage = "Thank you for your message. \n"
			+ "We will get in touch with you shortly.\n"
			+ "Copy of the message.\n\n-----------------------------------------------------\n\n";



	public static Map<String, Object> handle(String payload,Email sendEmail, Map<String, Object> requestParams) throws MessagingException, UnsupportedEncodingException {

		LOGGER.debug(payload);
		JsonObject payObj = gson.fromJson(payload, JsonObject.class);
		JsonArray mails = payObj.get("emails").getAsJsonArray();
		String from = requestParams.get("email").toString();
		String firstName = requestParams.get("firstName").toString();
		String lastName = requestParams.get("lastName").toString();

		if(System.getenv("ENVIRONMENT_TYPE").equals("production")) {
			String[] emails = new String[mails.size()];
			for (int i = 0; i < mails.size(); i++) emails[i] = mails.getAsJsonArray().get(i).getAsString();

			if(System.getenv("MAIL_TYPE").equals("SMTP")){
				sendViaSMTP(emails, from, sendEmail.getSubject(), sendEmail.getBodyText(), firstName, lastName);
			}
			if(System.getenv("MAIL_TYPE").equals("API")){
				try {
					sendViaAPI(emails, from, sendEmail.getSubject(), sendEmail.getBodyText(), firstName, lastName);
				} catch (IOException | InterruptedException e) {
					LOGGER.error(e.getLocalizedMessage());
				}
			}
		}else {
			String[] devMails =System.getenv("DEV_EMAILS").split(";");
			if(System.getenv("MAIL_TYPE").equals("SMTP")){
				sendViaSMTP(devMails, from, sendEmail.getSubject(), sendEmail.getBodyText(), firstName, lastName);
			}
			if(System.getenv("MAIL_TYPE").equals("API")){
				try {
					sendViaAPI(devMails, from, sendEmail.getSubject(), sendEmail.getBodyText(), firstName, lastName);
				} catch (IOException | InterruptedException e) {
					LOGGER.error(e.getLocalizedMessage());
				}
			}
		}

		return new HashMap<String, Object>();
	}

	public static void sendViaSMTP(String[] emails, String from, String subject, String bodyText, String firstName, String lastName) {
		Properties props = new Properties();
		{
			props.setProperty("mail.smtp.auth", "true");
			props.setProperty("mail.smtp.host", System.getenv("MAIL_HOST"));
			props.setProperty("mail.smtp.port", "587");
		}

		Authenticator auth = new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication( System.getenv("MAIL_USER"), System.getenv("MAIL_PASSWORD"));
			}
		};

		for(String email : emails) {
			LOGGER.info("Preparing email to: "+email.toString());
			LOGGER.info("Using properties: "+props.toString());
			LOGGER.info("Using Auth: "+auth.toString());
			LOGGER.info("Creating a new session");
			Session session = Session.getDefaultInstance(props, auth);
			LOGGER.info("New session created, sending email");
			EmailUtil.sendEmail(session, email,subject, "From: "+from+"\n" + bodyText);
			LOGGER.info("End session");
		}
	}

	public static void sendViaAPI(String[] emails, String from, String subject, String bodyText, String firstName, String lastName) throws IOException, InterruptedException {	
		for(String email : emails) {
			//String cmd = "curl -s --user '"+System.getenv("MAIL_API_KEY")+"' "+System.getenv("MAIL_API_URL")+" -F from='"+System.getenv("SENDER_NAME")+" <"+System.getenv("SENDER")+"@"+System.getenv("SENDER_DOMAIN")+">' -F to="+email+" -F subject='"+subject+"' -F text='From: "+from+"' -F text='" + bodyText+"'";
			OkHttpClient client = new OkHttpClient();

			String[] apiKey = System.getenv("MAIL_API_KEY").split(":");

			String credential = Credentials.basic(apiKey[0], apiKey[1]);

			RequestBody requestBody = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("from", System.getenv("SENDER_NAME")+" <"+System.getenv("SENDER")+"@"+System.getenv("SENDER_DOMAIN")+">")
					.addFormDataPart("to", email)
					.addFormDataPart("subject", subject)
					.addFormDataPart("text", "From: "+from)
					.addFormDataPart("text", bodyText)
					.build();

			Request request = new Request.Builder()
					.url(System.getenv("MAIL_API_URL"))
					.post(requestBody)
					.header("Authorization", credential)
					.build();

			try (Response response = client.newCall(request).execute()) {
				if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
				response.body().string();
			}
		}
		sendForwardViaAPI(emails, from, subject, forwardedMessage+bodyText, firstName, lastName);
	}

	public static void sendForwardViaAPI(String[] emails, String from, String subject, String bodyText, String firstName, String lastName) throws IOException, InterruptedException {	
		String dear = "Dear ";
		if(firstName!=null) dear+=firstName+" ";
		if(lastName!=null) dear+=lastName;
		if(firstName==null && lastName==null) dear+="User";
		dear+=",\n";
		
		subject += subjectForwardedMessage;
		
		OkHttpClient client = new OkHttpClient();

		String[] apiKey = System.getenv("MAIL_API_KEY").split(":");

		String credential = Credentials.basic(apiKey[0], apiKey[1]);

		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("from", System.getenv("SENDER_NAME")+" <"+System.getenv("SENDER")+"@"+System.getenv("SENDER_DOMAIN")+">")
				.addFormDataPart("to", from)
				.addFormDataPart("subject", subject)
				.addFormDataPart("text", dear)
				.addFormDataPart("text", bodyText)
				.build();

		Request request = new Request.Builder()
				.url(System.getenv("MAIL_API_URL"))
				.post(requestBody)
				.header("Authorization", credential)
				.build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
			response.body().string();
		}

	}

}
