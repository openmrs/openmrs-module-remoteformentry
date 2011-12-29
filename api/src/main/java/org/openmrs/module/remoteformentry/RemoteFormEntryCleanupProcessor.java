package org.openmrs.module.remoteformentry;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.FormService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Processor to loop over and delete all encounters+obs that match 
 * to the xml files
 * 
 * @see org.openmrs.module.remoteformentry.ProcessPendingQueueItemsTask
 */
@Transactional
public class RemoteFormEntryCleanupProcessor{

	private static final Log log = LogFactory
	.getLog(RemoteFormEntryCleanupProcessor.class);

	private DocumentBuilderFactory documentBuilderFactory;
	private XPathFactory xPathFactory;
	private static Boolean isRunning = false; // allow only one running
	
	private static Integer runCounter = 0;
	
	private Patient newDummyPatient = null;
	
	private RemoteFormEntryService remoteFormEntryService = null;
	private PatientService patientService = null;
	private FormService formService = null;
	private EncounterService encService = null;

	private int count = 0;
	
	/**
	 * Empty constructor (requires context to be set before any other calls are
	 * made)
	 */
	public RemoteFormEntryCleanupProcessor() {
	}

	/**
	 * Process this cleanup queue item.
	 * 
	 * Delete all encounters that match to this patient
	 * 
	 * @param pendingQueue
	 *            to be cleaned/purged/deleted
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws XPathExpressionException 
	 * @throws NumberFormatException 
	 */
	public void cleanupRemoteFormEntryQueue(RemoteFormEntryPendingQueue pendingQueue) throws Exception {
		log.debug("Transforming form entry queue: " + pendingQueue.getFileSystemUrl());
		
		String formData = pendingQueue.getFormData();
		Integer formId = null;
		String errorDetails = null;
		List<PatientIdentifier> identifiers = null;
		
		// First we parse the FormEntry xml data to obtain the formId of the
		// form that was used to create the xml data
		DocumentBuilderFactory dbf = getDocumentBuilderFactory();
		DocumentBuilder db = dbf.newDocumentBuilder();
		XPathFactory xpf = getXPathFactory();
		XPath xp = xpf.newXPath();
		Document doc = db.parse(IOUtils.toInputStream(formData));
		
		try {
			formId = Integer.parseInt(xp.evaluate("/form/@id", doc));
		
			// try to get the form
			Form form = formService.getForm(formId);
			if (form == null)
				throw new RemoteFormEntryException("Error retrieving form id from data");
			
			// try to get the patient id
			String pId = xp.evaluate("/form/patient/patient.patient_id", doc);
			if (pId == null)
				throw new RemoteFormEntryException("Patient's id was not found in data in /form/patient/patient.patient_id");
			Integer patientId = Integer.valueOf(pId);
			
			Patient patientById = patientService.getPatient(patientId);
			
			// try to get the patient's identifiers
			identifiers = RemoteFormEntryUtil.getPatientIdentifiers(doc, xp, null);
			
			// try to find this patient by identifier
			Patient patientByIdentifier = null;
			int i = 0;
			while (patientByIdentifier == null && i < identifiers.size()) {
				PatientIdentifier currentIdentifier = identifiers.get(i++);
				String identifierString = currentIdentifier.getIdentifier();
				Integer identifierTypeId = currentIdentifier.getIdentifierType().getPatientIdentifierTypeId();
				
				// search the database for all patients with this identifier string
				List<Patient> patients = patientService.getPatients(null, identifierString, null);
				
				for (Patient p : patients) {
					// loop over this patient's identifiers to make sure they have this 
					// one _and_ this type.  if they do, great, its them.  if not, keep trying 
					for (PatientIdentifier identifier : p.getIdentifiers()) {
						if (identifier.getIdentifier().equals(identifierString) && 
							identifierTypeId.equals(identifier.getIdentifierType().getPatientIdentifierTypeId())) {
							patientByIdentifier = p;
							break;
						}
					}
				}
			}
			
			User enterer = RemoteFormEntryUtil.getEnterer(doc, xp);
			DateFormat hl7DateFormat = new SimpleDateFormat("yyyy-MM-dd");
		    
			String encounterDateString = xp.evaluate("/form/encounter/encounter.encounter_datetime", doc);
			if (encounterDateString == null)
				throw new APIException("Unable to parse encounterDateString for: " + pendingQueue.getFileSystemUrl());
			
			Date encounterDate = hl7DateFormat.parse(encounterDateString);
			
			String providerIdString = xp.evaluate("substring-before(/form/encounter/encounter.provider_id, '^')", doc);
			if (providerIdString.length() < 1)
				throw new APIException("Unable to parse provider for: " + pendingQueue.getFileSystemUrl());
			
			User provider = new User(Integer.valueOf(providerIdString));
	
			String locationIdString = xp.evaluate("substring-before(/form/encounter/encounter.location_id, '^')", doc);
			if (locationIdString.length() < 1)
				throw new APIException("Unable to parse location for: " + pendingQueue.getFileSystemUrl());
			
			Location location = new Location(Integer.valueOf(locationIdString));
			
			List<Form> forms = new Vector<Form>();
			forms.add(form);
			Set<Encounter> encountersToDelete = new HashSet<Encounter>();
			List<Encounter> encounters = new Vector<Encounter>();
			
			if (patientById != null)
				encounters.addAll(Context.getEncounterService().getEncounters(patientById, location, encounterDate, encounterDate, forms, null, true));
			if (patientByIdentifier != null)
				encounters.addAll(Context.getEncounterService().getEncounters(patientByIdentifier, location, encounterDate, encounterDate, forms, null, true));
			
			for (Encounter e : encounters) {
				if (e.getCreator().equals(enterer) && e.getProvider().equals(provider))
					encountersToDelete.add(e);
			}
			
			log.debug("Found " + encounters.size() + " encounters that might fit");
			// delete each of these encounters
			for (Encounter e : encountersToDelete) {
				log.error("Set to delete: " + e.getEncounterId());
				encService.voidEncounter(e, "rfe cleanup - old pid #" + e.getPatientId());
				e.setPatient(newDummyPatient);
				
				// set the new obs.person
				for (Obs o : e.getAllObs(true))
					o.setPerson(newDummyPatient);
				
				count = count + 1;
			}
			
			if (runCounter++ % 50 == 0) {
				Context.clearSession();
				System.gc();
			}
		
		}
		catch (Throwable t) {
			log.error("Error while cleaning up!", t);
		}
		finally {
			remoteFormEntryService.createRemoteFormEntryPendingQueue(pendingQueue);
			
			// remove the now useless pending queue item
			remoteFormEntryService.deleteRemoteFormEntryPendingQueue(pendingQueue);
		}
	}

	/**
	 * Transform the next pending RemoteFormEntryPendingQueue entry. If there are no pending
	 * items in the queue, this method simply returns quietly.
	 * 
	 * @return true if a queue entry was processed, false if queue was empty
	 */
	public boolean processNextRemoteFormEntryCleanupQueue() throws Exception {
		boolean transformOccurred = false;
		
		RemoteFormEntryPendingQueue rfeq;
		if ((rfeq = getNextCleanupQueueItem()) != null) {
			cleanupRemoteFormEntryQueue(rfeq);
			transformOccurred = true;
		}
		
		return transformOccurred;
	}
	
	/**
	 * @return
	 */
	private RemoteFormEntryPendingQueue getNextCleanupQueueItem() {
		AdministrationService as = Context.getAdministrationService();
		String folder = as.getGlobalProperty(
				"remoteformentry.cleanup_queue_dir",
		        "cleanup_dir");
		
		File cleanupDir = OpenmrsUtil.getDirectoryInApplicationDataDirectory(folder);

		// return the first queue item
		for (File file : cleanupDir.listFiles()) {
			RemoteFormEntryPendingQueue queueItem = new RemoteFormEntryPendingQueue();
			queueItem.setFileSystemUrl(file.getAbsolutePath());
			return queueItem;
		}
		
		return null;
	}

	/**
	 * @return DocumentBuilderFactory to be used for parsing XML
	 */
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

	/**
	 * Starts up a thread to process all existing RemoteFormEntryPendingQueue entries
	 */
	public void processRemoteFormEntryCleanupQueue() throws APIException {
		
		String patientIdString = Context.getAdministrationService().getGlobalProperty("remoteformentry.cleanup_patient_id");
		
		if (patientIdString == null) {
			log.error("The rfe module cleanup depends on there being a dummy patient to move all cleaned encounters to.  Define remoteformentry.cleanup_patient_id");
			throw new APIException("You must define a remoteformentry.cleanup_patient_id global property pointing at a dummy patient_id to catch all 'cleaned' encounters");
		}
		
		Integer patientId = Integer.valueOf(patientIdString);
		
		newDummyPatient = new Patient(patientId);
		
		remoteFormEntryService = (RemoteFormEntryService)Context.getService(RemoteFormEntryService.class);
		patientService = Context.getPatientService();
		formService = Context.getFormService();
		encService = Context.getEncounterService();
		
		
		synchronized (isRunning) {
			if (isRunning) {
				log.warn("Processor aborting (another processor already running)");
				return;
			}
			isRunning = true;
		}
		try {
			log.debug("Start processing RemoteFormEntry pending queue");
			while (processNextRemoteFormEntryCleanupQueue() && count < 50) {
				// loop until queue is empty
			}
			count = 0;
			log.debug("Done processing RemoteFormEntry pending queue");
		}
		catch (Exception e) {
			log.error("Uh oh.  Got an error", e);
		}
		finally {
			isRunning = false;
		}
	}

}
