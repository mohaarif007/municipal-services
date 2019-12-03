package org.egov.bpa.service;

import org.egov.bpa.repository.BPARepository;
import org.egov.bpa.web.models.BPA;
import org.egov.bpa.web.models.BPARequest;
import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.BPARepository;
//import org.egov.tl.service.CalculationService;
//import org.egov.tl.service.DiffService;
//import org.egov.tl.service.EnrichmentService;
//import org.egov.tl.service.UserService;
//import org.egov.tl.service.notification.EditNotificationService;
import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.validator.BPAValidator;
import org.egov.bpa.workflow.ActionValidator;
import org.egov.bpa.workflow.BPAWorkflowService;
import org.egov.bpa.workflow.WorkflowIntegrator;
import org.egov.bpa.workflow.WorkflowService;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BPAService {
	
	@Autowired
	private BPARepository bpaRequestInfoDao;

	@Autowired
	private WorkflowIntegrator wfIntegrator;

	@Autowired
    private EnrichmentService enrichmentService;
	
	@Autowired
    private EDCRService edcrService;

	@Autowired
    private UserService userService;

	@Autowired
    private BPARepository repository;

    @Autowired
    private ActionValidator actionValidator;

    @Autowired
    private BPAValidator bpaValidator;

    private BPAWorkflowService TLWorkflowService;

    @Autowired
    private BPAUtil util;

//    private DiffService diffService;

    @Autowired
    private BPAConfiguration config;

    private WorkflowService workflowService;

//    private EditNotificationService  editNotificationService;
	public BPA create(BPARequest bpaRequest) {

		   Object mdmsData = util.mDMSCall(bpaRequest);
		   if( !edcrService.validateEdcrPlan(bpaRequest)) {
			   throw new CustomException("INVALID EDCR NUMBER",
						"The Scrutiny is not accepted for the EDCR Number "
								+ bpaRequest.getBPA().getEdcrNumber() );
		   }
		    bpaValidator.validateCreate(bpaRequest,mdmsData);
	        actionValidator.validateCreateRequest(bpaRequest);
	        enrichmentService.enrichBPACreateRequest(bpaRequest,mdmsData);
	       
	        userService.createUser(bpaRequest);
//	        calculationService.addCalculation(tradeLicenseRequest);
			
	        /*
			 * call workflow service if it's enable else uses internal workflow process
			 */
			if (config.getIsExternalWorkFlowEnabled())
				wfIntegrator.callWorkFlow(bpaRequest);
			repository.save(bpaRequest);
			return bpaRequest.getBPA();
	}
}