package org.epos.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.swagger.model.ProviderType;

import org.epos.eposdatamodel.*;
import org.epos.handler.dbapi.DBAPIClient;
import org.epos.handler.dbapi.model.*;
import org.epos.handler.dbapi.service.DBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.*;
import static org.epos.handler.dbapi.util.DBUtil.getFromDB;

public class ContactPointGet {

	private static final Logger LOGGER = LoggerFactory.getLogger(ContactPointGet.class);

	private static final DBAPIClient dbapi = new DBAPIClient();

	public static JsonObject generate(JsonObject response, Map<String, Object> requestParams) {

		LOGGER.info("Requests start - JPA method");

		String id = requestParams.get("id").toString();
		ProviderType type = ProviderType.valueOf(requestParams.get("type").toString());

		JsonArray listEmails = new JsonArray();

		switch(type) {
		case SERVICEPROVIDERS:
			for(WebService ws : dbapi.retrieve(WebService.class, new DBAPIClient.GetQuery().instanceId(id))) {
				for(LinkedEntity le : ws.getContactPoint()) {
					dbapi.retrieve(ContactPoint.class, new DBAPIClient.GetQuery().instanceId(le.getInstanceId()))
					.forEach(contact-> contact.getEmail().forEach(mail->listEmails.add(mail)));
				}	
			}
			break;
		case DATAPROVIDERS:
			for(DataProduct dp : dbapi.retrieve(DataProduct.class, new DBAPIClient.GetQuery().instanceId(id))) {
				for(LinkedEntity le : dp.getContactPoint()) {
					dbapi.retrieve(ContactPoint.class, new DBAPIClient.GetQuery().instanceId(le.getInstanceId()))
					.forEach(contact-> contact.getEmail().forEach(mail->listEmails.add(mail)));
				}	
			}
			break;
		case ALL:

			//EntityManager em = new DBService().getEntityManager();

			dbapi.retrieve(Distribution.class, new DBAPIClient.GetQuery().instanceId(id));

			for(Distribution dist : dbapi.retrieve(Distribution.class, new DBAPIClient.GetQuery().instanceId(id))) {

				EDMDataproduct tempDP = null;
				if (dist.getDataProduct() != null && !dist.getDataProduct().isEmpty()) {
					System.out.println(dist.getDataProduct().size());
					for(DataProduct dp : dbapi.retrieve(DataProduct.class, new DBAPIClient.GetQuery().instanceId(dist.getDataProduct().get(0).getInstanceId()))) {
						for(LinkedEntity le : dp.getContactPoint()) {
							dbapi.retrieve(ContactPoint.class, new DBAPIClient.GetQuery().instanceId(le.getInstanceId()))
									.forEach(contact-> contact.getEmail().forEach(mail->listEmails.add(mail)));
						}
					}

					for(WebService ws : dbapi.retrieve(WebService.class, new DBAPIClient.GetQuery().instanceId(dist.getDataProduct().get(0).getInstanceId()))) {
						for(LinkedEntity le : ws.getContactPoint()) {
							dbapi.retrieve(ContactPoint.class, new DBAPIClient.GetQuery().instanceId(le.getInstanceId()))
									.forEach(contact-> contact.getEmail().forEach(mail->listEmails.add(mail)));
						}
					}
				}
			}
			break;
		default:
			return response;
		}

		response.add("emails", listEmails);

		System.out.println(response);
		return response;
	}

}
