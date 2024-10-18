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
		EntityManager em = new DBService().getEntityManager();

		String id = requestParams.get("id").toString();
		ProviderType type = ProviderType.valueOf(requestParams.get("type").toString());

		JsonArray listEmails = new JsonArray();
		
		EDMDistribution distributionSelected = getFromDB(em, EDMDistribution.class,
				"distribution.findAllByMetaId",
				"METAID", id).get(0);

		EDMDataproduct dataProduct = null;
		if (distributionSelected.getIsDistributionsByInstanceId() != null &&
				!distributionSelected.getIsDistributionsByInstanceId().isEmpty()) {
			dataProduct = distributionSelected.getIsDistributionsByInstanceId().stream()
					.map(EDMIsDistribution::getDataproductByInstanceDataproductId)
					.filter(edmDataproduct -> edmDataproduct.getState().equals(State.PUBLISHED.toString()))
					.findFirst().orElse(null);
		}
		
		EDMWebservice webService = distributionSelected.getWebserviceByAccessService() != null && distributionSelected.getWebserviceByAccessService().getState().equals(State.PUBLISHED.toString()) ?
				distributionSelected.getWebserviceByAccessService() : null;
		switch(type) {
		case SERVICEPROVIDERS:
			if(webService!=null)
				for(EDMContactpointWebservice contactPointWebService : webService.getContactpointWebservicesByInstanceId()) {
					EDMContactpoint contactPoint = contactPointWebService.getContactpointByInstanceContactpointId();
					for(EDMContactpointEmail emails : contactPoint.getContactpointEmailsByInstanceId()) {
						listEmails.add(emails.getEmail());
					}
				}
			break;
		case DATAPROVIDERS:
			if(dataProduct!=null)
				for(EDMContactpointDataproduct contactPointDataProduct : dataProduct.getContactpointDataproductsByInstanceId()) {
					EDMContactpoint contactPoint = contactPointDataProduct.getContactpointByInstanceContactpointId();
					for(EDMContactpointEmail emails : contactPoint.getContactpointEmailsByInstanceId()) {
						listEmails.add(emails.getEmail());
					}
				}
			break;
		case ALL:
			if(webService!=null)
				for(EDMContactpointWebservice contactPointWebService : webService.getContactpointWebservicesByInstanceId()) {
					EDMContactpoint contactPoint = contactPointWebService.getContactpointByInstanceContactpointId();
					for(EDMContactpointEmail emails : contactPoint.getContactpointEmailsByInstanceId()) {
						listEmails.add(emails.getEmail());
					}
				}
			if(dataProduct!=null)
				for(EDMContactpointDataproduct contactPointDataProduct : dataProduct.getContactpointDataproductsByInstanceId()) {
					EDMContactpoint contactPoint = contactPointDataProduct.getContactpointByInstanceContactpointId();
					for(EDMContactpointEmail emails : contactPoint.getContactpointEmailsByInstanceId()) {
						listEmails.add(emails.getEmail());
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
