package org.egov.fsm.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.fsm.config.FSMConfiguration;
import org.egov.fsm.repository.ServiceRequestRepository;
import org.egov.fsm.web.model.AuditDetails;
import org.egov.mdms.model.MasterDetail;
import org.egov.mdms.model.MdmsCriteria;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.mdms.model.ModuleDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

import lombok.Builder;

@Component
public class FSMUtil {
	/**
	 * json path's defuault cofig to read/parse the json
	 */
	@Autowired
	private ServiceRequestRepository serviceRequestRepository;
	
	@Autowired
	private FSMConfiguration config;
	public void defaultJsonPathConfig() {
		Configuration.setDefaults(new Configuration.Defaults() {

			private final JsonProvider jsonProvider = new JacksonJsonProvider();
			private final MappingProvider mappingProvider = new JacksonMappingProvider();

			@Override
			public JsonProvider jsonProvider() {
				return jsonProvider;
			}

			@Override
			public MappingProvider mappingProvider() {
				return mappingProvider;
			}

			@Override
			public Set<Option> options() {
				return EnumSet.noneOf(Option.class);
			}
		});
	}
	public AuditDetails getAuditDetails(User user, Boolean isCreate) {
		Long time = System.currentTimeMillis();
		if (isCreate)
			return AuditDetails.builder().createdTime(time).lastModifiedTime(time)
					.build();
		else
			return AuditDetails.builder().lastModifiedTime(time).build();
	}
	/**
	 * makes mdms call with the given criteria and reutrn mdms data
	 * @param requestInfo
	 * @param tenantId
	 * @return
	 */
	public Object mDMSCall(RequestInfo requestInfo, String tenantId) {
		MdmsCriteriaReq mdmsCriteriaReq = getMDMSRequest(requestInfo, tenantId);
		Object result = serviceRequestRepository.fetchResult(getMdmsSearchUrl(), mdmsCriteriaReq);
		return result;
	}
	/**
	 * Returns the URL for MDMS search end point
	 *
	 * @return URL for MDMS search end point
	 */
	public StringBuilder getMdmsSearchUrl() {
		return new StringBuilder().append(config.getMdmsHost()).append(config.getMdmsEndPoint());
	}
	/**
	 * prepares the mdms request object
	 * @param requestInfo
	 * @param tenantId
	 * @return
	 */
	public MdmsCriteriaReq getMDMSRequest(RequestInfo requestInfo, String tenantId) {
		List<ModuleDetail> moduleRequest = getFSMModuleRequest();

		List<ModuleDetail> moduleDetails = new LinkedList<>();
		moduleDetails.addAll(moduleRequest);

		MdmsCriteria mdmsCriteria = MdmsCriteria.builder().moduleDetails(moduleDetails).tenantId(tenantId).build();

		MdmsCriteriaReq mdmsCriteriaReq = MdmsCriteriaReq.builder().mdmsCriteria(mdmsCriteria).requestInfo(requestInfo)
				.build();
		return mdmsCriteriaReq;
	}
	public List<ModuleDetail> getFSMModuleRequest() {

		// master details for FSM module
		List<MasterDetail> fsmMasterDtls = new ArrayList<>();

		// filter to only get code field from master data
		final String filterCode = "$.[?(@.active==true)].code";



		// master details for proeprty module
		List<MasterDetail> propertyMasterDetails = new ArrayList<>();
		propertyMasterDetails
				.add(MasterDetail.builder().name(FSMConstants.PROPERTY_TYPE).filter(filterCode).build());
		
		ModuleDetail propertyMasterMDtl = ModuleDetail.builder().masterDetails(propertyMasterDetails)
				.moduleName(FSMConstants.PROPERTY_MASTER_MODULE).build();
		
		// master details for NOC module


		return Arrays.asList(propertyMasterMDtl);

	}

}