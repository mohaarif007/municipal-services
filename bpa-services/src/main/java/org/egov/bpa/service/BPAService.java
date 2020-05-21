package org.egov.bpa.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.egov.bpa.repository.BPARepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.util.NotificationUtil;
import org.egov.bpa.validator.BPAValidator;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.workflow.BusinessService;
import org.egov.bpa.workflow.ActionValidator;
import org.egov.bpa.workflow.WorkflowIntegrator;
import org.egov.bpa.workflow.WorkflowService;
import org.egov.common.contract.request.RequestInfo;
import org.egov.land.web.models.LandInfo;
import org.egov.land.web.models.LandSearchCriteria;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;

@Service
@Slf4j
public class BPAService {

	@Autowired
	private WorkflowIntegrator wfIntegrator;

	@Autowired
	private EnrichmentService enrichmentService;

	@Autowired
	private EDCRService edcrService;

	@Autowired
	private BPARepository repository;

	@Autowired
	private ActionValidator actionValidator;

	@Autowired
	private BPAValidator bpaValidator;

	@Autowired
	private BPAUtil util;

	@Autowired
	private CalculationService calculationService;

	@Autowired
	private WorkflowService workflowService;

	@Autowired
	private NotificationUtil notificationUtil;

	@Autowired
	private BPALandService landService;
	
	public BPA create(BPARequest bpaRequest) {
		RequestInfo requestInfo = bpaRequest.getRequestInfo();
		String tenantId = bpaRequest.getBPA().getTenantId().split("\\.")[0];
		Object mdmsData = util.mDMSCall(requestInfo, tenantId);
		if (bpaRequest.getBPA().getTenantId().split("\\.").length == 1) {
			throw new CustomException(" Invalid Tenant ", " Application cannot be create at StateLevel");
		}
		
		Map<String, String> values = edcrService.validateEdcrPlan(bpaRequest, mdmsData);
		bpaValidator.validateCreate(bpaRequest, mdmsData, values);
		landService.addLandInfoToBPA(bpaRequest);
		enrichmentService.enrichBPACreateRequest(bpaRequest, mdmsData);
		
		wfIntegrator.callWorkFlow(bpaRequest);

			calculationService.addCalculation(bpaRequest, BPAConstants.APPLICATION_FEE_KEY);
		repository.save(bpaRequest);
		return bpaRequest.getBPA();
	}


	/**
	 * Searches the Bpa for the given criteria if search is on owner paramter
	 * then first user service is called followed by query to db
	 * 
	 * @param criteria
	 *            The object containing the parameters on which to search
	 * @param requestInfo
	 *            The search request's requestInfo
	 * @return List of bpa for the given criteria
	 */
	public List<BPA> search(BPASearchCriteria criteria, RequestInfo requestInfo) {
		List<BPA> bpa = new LinkedList<>();
		bpaValidator.validateSearch(requestInfo, criteria);
		LandSearchCriteria landcriteria = new LandSearchCriteria();
		landcriteria.setTenantId(criteria.getTenantId());
		if (criteria.getMobileNumber() != null) {
			landcriteria.setMobileNumber(criteria.getMobileNumber());
			ArrayList<LandInfo> landInfo = landService.searchLandInfoToBPA(requestInfo, landcriteria);
			bpa = getBPAFromLandId(criteria, requestInfo);
			if (landInfo.size() > 0) {
				for (int i = 0; i < bpa.size(); i++) {
					for (int j = 0; j < landInfo.size(); j++) {
						if (landInfo.get(j).getId().equalsIgnoreCase(bpa.get(i).getLandId())) {
							bpa.get(i).setLandInfo(landInfo.get(j));
						}
					}
				}
				bpa = bpa.stream().filter(a -> a.getLandInfo() != null).collect(Collectors.toList());
			}
		} else {
		

			bpa = getBPAFromCriteria(criteria, requestInfo);
			ArrayList<String> data = new ArrayList<String>();
			if (bpa.size() > 0) {
				for(int i=0; i<bpa.size(); i++){
					data.add(bpa.get(i).getLandId());
				}
				landcriteria.setIds(data);
				ArrayList<LandInfo> landInfo = landService.searchLandInfoToBPA(requestInfo, landcriteria);
				
				for (int i = 0; i < bpa.size(); i++) {
					for (int j = 0; j < landInfo.size(); j++) {
						if (landInfo.get(j).getId().equalsIgnoreCase(bpa.get(i).getLandId())) {
							bpa.get(i).setLandInfo(landInfo.get(j));
						}
					}
				}
			}
		}
		return bpa;
	}

	


	private List<BPA> getBPAFromLandId(BPASearchCriteria criteria, RequestInfo requestInfo) {
		// TODO Auto-generated method stub
		List<BPA> bpa = new LinkedList<>();
		bpa = repository.getBPAData(criteria);
		if (bpa.size() == 0) {
			return Collections.emptyList();
		}
//		criteria = enrichmentService.getBPACriteriaFromIds(bpa, criteria.getLimit());
		return bpa;
	}


	/**
	 * Returns the bpa with enriched owners from user service
	 * 
	 * @param criteria
	 *            The object containing the parameters on which to search
	 * @param requestInfo
	 *            The search request's requestInfo
	 * @return List of bpa for the given criteria
	 */
	public List<BPA> getBPAFromCriteria(BPASearchCriteria criteria, RequestInfo requestInfo) {
		List<BPA> bpa = repository.getBPAData(criteria);
		if (bpa.isEmpty())
			return Collections.emptyList();
		return bpa;
	}

	/**
	 * Updates the bpa
	 * 
	 * @param bpaRequest
	 *            The update Request
	 * @return Updated bpa
	 */
	/**
	 * Updates the bpa
	 * 
	 * @param bpaRequest
	 *            The update Request
	 * @return Updated bpa
	 */
	public BPA update(BPARequest bpaRequest) {
		RequestInfo requestInfo = bpaRequest.getRequestInfo();
		String tenantId = bpaRequest.getBPA().getTenantId().split("\\.")[0];
		Object mdmsData = util.mDMSCall(requestInfo, tenantId);
		BPA bpa = bpaRequest.getBPA();

		if (bpa.getId() == null) {
			throw new CustomException("UPDATE ERROR", "Application Not found in the System" + bpa);
		}

		BusinessService businessService = workflowService.getBusinessService(bpa, bpaRequest.getRequestInfo(),
				bpa.getApplicationNo());

		List<BPA> searchResult = getBPAWithBPAId(bpaRequest);
		if (CollectionUtils.isEmpty(searchResult)) {
			throw new CustomException("UPDATE ERROR", "Failed to Update the Application");
		}

		bpaRequest.getBPA().setAuditDetails(searchResult.get(0).getAuditDetails());
		enrichmentService.enrichBPAUpdateRequest(bpaRequest, businessService);

		if (bpa.getWorkflow().getAction() != null && (bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_REJECT)
				|| bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_REVOCATE))) {

			if (bpa.getWorkflow().getComments() == null || bpa.getWorkflow().getComments().isEmpty()) {
				throw new CustomException("BPA_UPDATE_ERROR_COMMENT_REQUIRED",
						"Comment is mandaotory, please provide the comments ");
			}

		} else {
			// userService.createUser(bpaRequest);
			if (!bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_SENDBACKTOCITIZEN)) {
				actionValidator.validateUpdateRequest(bpaRequest, businessService);
				landService.updateLandInfo(bpaRequest);
				bpaValidator.validateUpdate(bpaRequest, searchResult, mdmsData,
						workflowService.getCurrentState(bpa.getStatus(), businessService));
				bpaValidator.validateCheckList(mdmsData, bpaRequest,
						workflowService.getCurrentState(bpa.getStatus(), businessService));
			}
		}

		wfIntegrator.callWorkFlow(bpaRequest);

		enrichmentService.postStatusEnrichment(bpaRequest);

		log.info("Bpa status is : " + bpa.getStatus());

		if (bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_APPLY)) {
			// generate sanction fee demand as well for the low risk application
				calculationService.addCalculation(bpaRequest, BPAConstants.APPLICATION_FEE_KEY);
		}
		
		// Generate the sanction Demand
		if (bpa.getStatus().equalsIgnoreCase(BPAConstants.SANC_FEE_STATE)) {
			calculationService.addCalculation(bpaRequest, BPAConstants.SANCTION_FEE_KEY);
		}
		
		repository.update(bpaRequest, workflowService.isStateUpdatable(bpa.getStatus(), businessService));
		return bpaRequest.getBPA();

	}


	/**
	 * Returns bpa from db for the update request
	 * 
	 * @param request
	 *            The update request
	 * @return List of bpas
	 */
	public List<BPA> getBPAWithBPAId(BPARequest request) {
		BPASearchCriteria criteria = new BPASearchCriteria();
		List<String> ids = new LinkedList<>();
		ids.add(request.getBPA().getId());
		criteria.setTenantId(request.getBPA().getTenantId());
		criteria.setIds(ids);
		List<BPA> bpa = repository.getBPAData(criteria);
		return bpa;
	}

	public void getEdcrPdf(BPARequest bpaRequest) {

		byte[] ba1 = new byte[1024];
		int baLength;
		String fileName = BPAConstants.EDCR_PDF;
		PDDocument doc = null;
		BPA bpa = bpaRequest.getBPA();

		if (StringUtils.isEmpty(bpa.getApprovalNo())) {
			throw new CustomException("INVALID_REQUEST", "Permit Order No is required.");
		}

		try {
			String pdfUrl = edcrService.getEDCRPdfUrl(bpaRequest);
			URL downloadUrl = new URL(pdfUrl);
			FileOutputStream fos1 = new FileOutputStream(fileName);
			log.info("Connecting to redirect url" + downloadUrl.toString() + " ... ");
			URLConnection urlConnection = downloadUrl.openConnection();

			// Checking whether the URL contains a PDF
			if (!urlConnection.getContentType().equalsIgnoreCase("application/pdf")) {
				String downloadUrlString = urlConnection.getHeaderField("Location");
				if (!StringUtils.isEmpty(downloadUrlString)) {
					downloadUrl = new URL(downloadUrlString);
					log.info("Connecting to download url" + downloadUrl.toString() + " ... ");
					urlConnection = downloadUrl.openConnection();
					if (!urlConnection.getContentType().equalsIgnoreCase("application/pdf")) {
						log.error("Download url content type is not application/pdf.");
						throw new Exception();
					}
				} else {
					log.error("Unable to fetch the location header URL");
					throw new Exception();
				}
			}
			// Read the PDF from the URL and save to a local file
			InputStream is1 = downloadUrl.openStream();
			while ((baLength = is1.read(ba1)) != -1) {
				fos1.write(ba1, 0, baLength);
			}
			fos1.flush();
			fos1.close();
			is1.close();

			doc = PDDocument.load(new File(fileName));

			PDPageTree allPages = doc.getDocumentCatalog().getPages();

			String localizationMessages = notificationUtil.getLocalizationMessages(bpa.getTenantId(),
					bpaRequest.getRequestInfo());
			String permitNo = notificationUtil.getMessageTemplate(BPAConstants.PERMIT_ORDER_NO, localizationMessages);
			permitNo = permitNo != null ? permitNo : BPAConstants.PERMIT_ORDER_NO;
			String generatedOn = notificationUtil.getMessageTemplate(BPAConstants.GENERATEDON, localizationMessages);
			generatedOn = generatedOn != null ? generatedOn : BPAConstants.GENERATEDON;

			for (int i = 0; i < allPages.getCount(); i++) {
				PDPage page = (PDPage) allPages.get(i);
				@SuppressWarnings("deprecation")
				PDPageContentStream contentStream = new PDPageContentStream(doc, page, true, true, true);
				PDFont font = PDType1Font.TIMES_ROMAN;
				float fontSize = 12.0f;
				contentStream.beginText();
				// set font and font size
				contentStream.setFont(font, fontSize);

				PDRectangle mediabox = page.getMediaBox();
				float margin = 20;
				float startX = mediabox.getLowerLeftX() + margin;
				float startY = mediabox.getUpperRightY() - margin;
				contentStream.newLineAtOffset(startX, startY);

				contentStream.showText(permitNo + " : " + bpaRequest.getBPA().getApprovalNo());
				if (bpa.getApprovalDate() != null) {
					Date date = new Date(bpa.getApprovalDate());
					DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
					String formattedDate = format.format(date);
					contentStream.newLineAtOffset(400, 4.5f);
					contentStream.showText(generatedOn + " : " + formattedDate);
				} else {
					contentStream.newLineAtOffset(400, 4.5f);
					contentStream.showText(generatedOn + " : " + "NA");
				}

				contentStream.endText();
				contentStream.close();
			}
			doc.save(fileName);

		} catch (Exception ex) {
			log.info("Exception occured while downloading pdf", ex.getMessage());
			throw new CustomException("UNABLE_TO_DOWNLOAD", "Unable to download the file");
		} finally {
			try {
				if (doc != null) {
					doc.close();
				}
			} catch (Exception ex) {
				throw new CustomException("INVALID_FILE", "UNABLE CLOSE THE FILE");
			}
		}
	}
}
