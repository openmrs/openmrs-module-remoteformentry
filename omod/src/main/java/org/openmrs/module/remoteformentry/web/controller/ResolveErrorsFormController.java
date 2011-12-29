package org.openmrs.module.remoteformentry.web.controller;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.formentry.FormEntryError;
import org.openmrs.module.formentry.FormEntryService;
import org.openmrs.module.remoteformentry.RemoteFormEntryException;
import org.openmrs.module.remoteformentry.RemoteFormEntryService;
import org.openmrs.web.WebConstants;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;
import org.w3c.dom.Document;

/**
 * Controller to let a user look at the remote form entry errors and relate
 * those to patients
 */
public class ResolveErrorsFormController extends SimpleFormController {

	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	private DocumentBuilderFactory documentBuilderFactory;
	private XPathFactory xPathFactory;
	
	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	protected Object formBackingObject(HttpServletRequest request)
	        throws ServletException {

		// get list of formentry error queue items that have a name error

		FormEntryService fes = (FormEntryService) Context.getService(FormEntryService.class);
		// get all form entry errors
		Collection<FormEntryError> errors = fes.getFormEntryErrors();

		// list to return to the view
		List<RemoteFormEntryErrorModel> newPatientErrors = new Vector<RemoteFormEntryErrorModel>();

		String prefix = RemoteFormEntryException.class.getName();

		for (FormEntryError error : errors) {
			if (error.getError().startsWith(prefix))
				newPatientErrors.add(new RemoteFormEntryErrorModel(error));
		}

		return newPatientErrors;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request,
	        HttpServletResponse response, Object object,
	        BindException bindException) throws Exception {

		// user must be authenticated (avoids auth errors)
		if (Context.isAuthenticated()) {
			FormEntryService formEntryService = getFormEntryService();
			RemoteFormEntryService remoteFormEntryService = getRemoteFormEntryService();
			PatientService patientService = Context.getPatientService();
			XPath xp = getXPathFactory().newXPath();
			
			// there should be equal numbers of each of these parameters
			int[] errorIds = ServletRequestUtils.getIntParameters(request, "formEntryErrorId");
			
			// loop over all parameters and do the actions the user requested
			for (int x = 0; x< errorIds.length; x++) {
				Integer errorId = errorIds[x];
				String actionItem = request.getParameter("errorItemAction-" + errorId);
				
				if ("currentPatient".equals(actionItem)) {
					// must do the request items like this so that the javascript picker works
					// correctly...
					String patientIdString = request.getParameter("currentPatientId-" + errorId);
					Integer patientId = Integer.valueOf(patientIdString);
					
					// fetch the selected patient from the database
					Patient patient = patientService.getPatient(patientId);
					
					// fetch the error queue item from the database
					FormEntryError errorItem = getFormEntryService().getFormEntryError(errorId);
					Document doc = getDocumentForErrorQueueItem(errorItem.getFormData());
					
					// update the selected patient with metadata from the form
					remoteFormEntryService.updatePatientInDatabase(patient, doc, xp);
					
					// create the formentry queue item so it can be processed normally
					remoteFormEntryService.createFormEntryQueueForPatient(errorItem.getFormData(), patient);
					
					// delete the formentry error queue item
					formEntryService.deleteFormEntryError(errorItem);
				}
				else if ("newPatient".equals(actionItem)) {
					// fetch the FormEntryError item from the database
					FormEntryError errorItem = getFormEntryService().getFormEntryError(errorId);
					Document doc = getDocumentForErrorQueueItem(errorItem.getFormData());
					
					// create the patient from the form data
					Patient newPatient = remoteFormEntryService.createPatientInDatabase(doc, xp);
					
					// create the formentry queue item so it can be processed normally
					remoteFormEntryService.createFormEntryQueueForPatient(errorItem.getFormData(), newPatient);
					
					// delete the formentry error queue item
					formEntryService.deleteFormEntryError(errorItem);
				}
				else if ("deleteError".equals(actionItem)) {
					// fetch the FormEntryError item from the database
					FormEntryError errorItem = getFormEntryService().getFormEntryError(errorId);
					
					// delete the formentry error queue item
					formEntryService.deleteFormEntryError(errorItem);
					
				}
				else if ("noChange".equals(actionItem)) {
					// do nothing here
				}
				else
					throw new APIException("Invalid action selected for: " + errorId);
			}
			
			HttpSession httpSession = request.getSession();
			httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "remoteformentry.resolveErrors.success");
			
			return new ModelAndView(new RedirectView(getSuccessView()));
			
		}

		return showForm(request, response, bindException);
	}

	/**
     * Get the form entry service
     * 
     * @return FormEntryService
     */
    private FormEntryService getFormEntryService() {
    	return (FormEntryService)Context.getService(FormEntryService.class);
    }

	/**
     * Get the remote form entry service
     * 
     * @return RemoteFormEntryService
     */
    private RemoteFormEntryService getRemoteFormEntryService() {
    	return (RemoteFormEntryService)Context.getService(RemoteFormEntryService.class);
    }
	
	private Document getDocumentForErrorQueueItem(String formData) throws Exception {
		DocumentBuilderFactory dbf = getDocumentBuilderFactory();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(IOUtils.toInputStream(formData));
		return doc;
	}
	
	private DocumentBuilderFactory getDocumentBuilderFactory() {
		if (documentBuilderFactory == null)
			documentBuilderFactory = DocumentBuilderFactory.newInstance();
		return documentBuilderFactory;
	}
	
	/**
	 * @return XPathFactory to be used for obtaining data from the parsed XML
	 */
	private XPathFactory getXPathFactory() {
		if (xPathFactory == null)
			xPathFactory = XPathFactory.newInstance();
		return xPathFactory;
	}
}
