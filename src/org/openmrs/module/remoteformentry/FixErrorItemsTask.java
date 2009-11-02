package org.openmrs.module.remoteformentry;

import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.formentry.FormEntryError;
import org.openmrs.module.formentry.FormEntryService;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.w3c.dom.Document;

/**
 * Fixes all "Unable to find Identifiers" formentry errors
 */
public class FixErrorItemsTask extends AbstractTask {

	// Logger
	private static Log log = LogFactory.getLog(FixErrorItemsTask.class);

	private DocumentBuilderFactory documentBuilderFactory;
	private XPathFactory xPathFactory;
	
	private RemoteFormEntryService remoteFormEntryService = null;
	private FormEntryService formEntryService = null;
	private PatientService patientService = null;

	private int fixCount = 0;
	
	/**
	 * Process the next form entry in the database and then remove the form
	 * entry from the database.
	 */
	public void execute() {
		Context.openSession();
		log.debug("Processing pending queue items (as central server) ... ");
		try {
			remoteFormEntryService = (RemoteFormEntryService)Context.getService(RemoteFormEntryService.class);
			formEntryService = (FormEntryService)Context.getService(FormEntryService.class);
			patientService = Context.getPatientService();
			xPathFactory = XPathFactory.newInstance();
			documentBuilderFactory = DocumentBuilderFactory.newInstance();
			
			DocumentBuilder db = documentBuilderFactory.newDocumentBuilder();
			XPath xp = xPathFactory.newXPath();
			
			for (FormEntryError errorItem : formEntryService.getFormEntryErrors()) {
				String error = errorItem.getError();
				if (error.startsWith("org.openmrs.module.remoteformentry")) {
					String formData = errorItem.getFormData();
					Document doc; 
					try {
						doc = db.parse(IOUtils.toInputStream(formData));
						
						String mrn = xp.evaluate("/form/patient/patient.medical_record_number", doc);
						if (mrn.length() > 0) {
							
							List<Patient> patients = patientService.getPatientsByIdentifier(mrn, false);
							
							if (patients.size() == 1) {
								Patient patient = patients.get(0);
								
								// update the selected patient with metadata from the form
								remoteFormEntryService.updatePatientInDatabase(patient, doc, xp);
								
								// create the formentry queue item so it can be processed normally
								remoteFormEntryService.createFormEntryQueueForPatient(errorItem.getFormData(), patient);
								
								// delete the formentry error queue item
								formEntryService.deleteFormEntryError(errorItem);
								fixCount = fixCount + 1;
								
							}
							
						}
						else {
							log.error("Can't do anything with: " + errorItem.getFormEntryErrorId());
						}
							
					} 
					catch (Exception e) {
						log.error("unable to fix error with id: " + errorItem.getFormEntryErrorId(), e);
					}
						
				}
				
			}
			
			
		} catch (ParserConfigurationException e) {
			log.error("Unable to parse config", e);
		}
		finally {
			Context.closeSession();
		}
		
		log.error("Fixed " + fixCount + " items");
	}
	
	/**
	 * Clean up any resources here
	 *
	 */
	public void shutdown() {
		
	}
	
}
