package org.openmrs.module.remoteformentry.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonName;
import org.openmrs.Relationship;
import org.openmrs.User;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.FormService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.formentry.FormEntryArchive;
import org.openmrs.module.formentry.FormEntryException;
import org.openmrs.module.formentry.FormEntryQueue;
import org.openmrs.module.formentry.FormEntryService;
import org.openmrs.module.formentry.FormEntryUtil;
import org.openmrs.module.formentry.PublishInfoPath;
import org.openmrs.module.remoteformentry.RemoteFormEntryConstants;
import org.openmrs.module.remoteformentry.RemoteFormEntryPendingQueue;
import org.openmrs.module.remoteformentry.RemoteFormEntryService;
import org.openmrs.module.remoteformentry.RemoteFormEntryUtil;
import org.openmrs.module.remoteformentry.db.RemoteFormEntryDAO;
import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.web.WebUtil;
import org.w3c.dom.Document;

/**
 * Remote data entry-related services
 * 
 */
public class RemoteFormEntryServiceImpl implements RemoteFormEntryService {

	private Log log = LogFactory.getLog(this.getClass());

	private RemoteFormEntryDAO dao;

	private Boolean isGeneratingDataFile = false;
	
	/**
	 * Get the remote form entry data access object
	 * 
	 * @return RemoteFormEntryDAO
	 */
	@SuppressWarnings("unused")
    private RemoteFormEntryDAO getRemoteFormEntryDAO() {
		return dao;
	}

	/**
	 * @see org.openmrs.module.remoteformentry.RemoteFormEntryService#setRemoteFormEntryDAO(org.openmrs.module.remoteformentry.db.RemoteFormEntryDAO)
	 */
	public void setRemoteFormEntryDAO(RemoteFormEntryDAO dao) {
		this.dao = dao;
	}

	/**
	 * @see org.openmrs.module.formentry.RemoteFormEntryService#createRemoteFormEntryPendingQueue(org.openmrs.module.formentry.RemoteFormEntryPendingQueue)
	 */
	public void createRemoteFormEntryPendingQueue(
	        RemoteFormEntryPendingQueue pendingQueue) {
		File queueDir = RemoteFormEntryUtil.getPendingQueueDir();
		
		String fileSystemUrl = pendingQueue.getFileSystemUrl();
		
		File outFile = null;
		
		if (fileSystemUrl != null) {
			// get just the file name out of the full path
			String filename = WebUtil.stripFilename(fileSystemUrl);
			// create the file with the same name as the filename in the current pending queue's url
			outFile = new File(queueDir, filename);
		}
		else 
			outFile = OpenmrsUtil.getOutFile(queueDir,
		                                     null,
		                                     pendingQueue.getCreator());

		// write the queue's data to the file
		try {
			FileWriter writer = new FileWriter(outFile);

			writer.write(pendingQueue.getFormData());

			writer.close();
		} catch (IOException io) {
			throw new FormEntryException("Unable to save formentry queue", io);
		}
	}

	/**
	 * @see org.openmrs.module.formentry.RemoteFormEntryService#getRemoteFormEntryPendingQueues()
	 */
	public List<RemoteFormEntryPendingQueue> getRemoteFormEntryPendingQueues() {
		List<RemoteFormEntryPendingQueue> queues = new Vector<RemoteFormEntryPendingQueue>();

		File queueDir = RemoteFormEntryUtil.getPendingQueueDir();

		if (queueDir.exists() == false) {
			log.warn("Unable to open queue directory: " + queueDir);
			return queues;
		}

		// loop over all files in queue dir and create lazy queue items
		for (File file : queueDir.listFiles()) {
			RemoteFormEntryPendingQueue queueItem = new RemoteFormEntryPendingQueue();
			queueItem.setFileSystemUrl(file.getAbsolutePath());
			queueItem.setDateCreated(new Date(file.lastModified()));
			queues.add(queueItem);
		}

		return queues;
	}

	/**
	 * @see org.openmrs.module.formentry.RemoteFormEntryService#deleteRemoteFormEntryPendingQueue(org.openmrs.module.formentry.RemoteFormEntryPendingQueue)
	 */
	public void deleteRemoteFormEntryPendingQueue(
	        RemoteFormEntryPendingQueue pendingQueue) {
		if (pendingQueue == null || pendingQueue.getFileSystemUrl() == null)
			throw new FormEntryException("Unable to load remoteFormEntryPendingQueue with empty file system url");

		File file = new File(pendingQueue.getFileSystemUrl());

		// if we have a relative url, prepend pendingQueue dir
		if (!file.exists() && !file.isAbsolute()) {
			file = new File(RemoteFormEntryUtil.getPendingQueueDir(),
			                pendingQueue.getFileSystemUrl());
		}

		if (file.exists()) {
			file.delete();
		}
	}

	/**
	 * @see org.openmrs.module.formentry.RemoteFormEntryService#getNextRemoteFormEntryPendingQueue()
	 */
	public RemoteFormEntryPendingQueue getNextRemoteFormEntryPendingQueue() {
		File queueDir = RemoteFormEntryUtil.getPendingQueueDir();

		// return the first queue item
		for (File file : queueDir.listFiles()) {
			RemoteFormEntryPendingQueue queueItem = new RemoteFormEntryPendingQueue();
			queueItem.setFileSystemUrl(file.getAbsolutePath());
			return queueItem;
		}

		return null;
	}

	/**
	 * @see org.openmrs.module.formentry.RemoteFormEntryService#getRemoteFormEntryPendingQueueSize()
	 */
	public Integer getRemoteFormEntryPendingQueueSize() {
		File queueDir = RemoteFormEntryUtil.getPendingQueueDir();

		return queueDir.list().length;
	}

	/**
	 * @see org.openmrs.module.remoteformentry.RemoteFormEntryService#getInitialEncounterTypes()
	 */
	public List<EncounterType> getInitialEncounterTypes() {
		AdministrationService as = Context.getAdministrationService();
		EncounterService es = Context.getEncounterService();

		String encounterTypeIdsString = as.getGlobalProperty(RemoteFormEntryConstants.GP_INITIAL_ENCOUNTER_TYPES,
		                                                     "");
		String[] encounterTypeIds = encounterTypeIdsString.split(",");

		// populate the encounter type objects
		List<EncounterType> encounterTypes = new Vector<EncounterType>();
		for (String type : encounterTypeIds) {
			type = type.trim();
			if (type.length() > 0) {
				try {
					EncounterType encType = es.getEncounterType(Integer.valueOf(type));
					encounterTypes.add(encType);
				} catch (Exception e) {
					log.warn("Unable to get encounter type with id: " + type, e);
				}
			}
		}

		return encounterTypes;
	}

	/**
	 * @see org.openmrs.module.remoteformentry.RemoteFormEntryService#setInitialEncounterTypes(java.lang.Integer)
	 */
	public void setInitialEncounterTypes(List<Integer> encounterTypeIds) {
		StringBuilder encounterTypeIdsBuilder = new StringBuilder();

		for (Integer typeId : encounterTypeIds) {
			encounterTypeIdsBuilder.append(typeId);
			encounterTypeIdsBuilder.append(",");
		}

		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(RemoteFormEntryConstants.GP_INITIAL_ENCOUNTER_TYPES);
		if (gp == null)
			gp = new GlobalProperty(RemoteFormEntryConstants.GP_INITIAL_ENCOUNTER_TYPES);
		gp.setPropertyValue(encounterTypeIdsBuilder.toString());
		as.saveGlobalProperty(gp);
	}

	/**
	 * @throws XPathExpressionException
	 * @see org.openmrs.module.remoteformentry.RemoteFormEntryService#createPatientInDatabase(org.w3c.dom.Document,
	 *      javax.xml.xpath.XPath)
	 */
	public Patient createPatientInDatabase(Document doc, XPath xp)
	        throws XPathExpressionException, Exception {

		PatientService patientService = Context.getPatientService();

		User enterer = RemoteFormEntryUtil.getEnterer(doc, xp);

		Patient patient = new Patient();

		// create the person name
		for (PersonName personName : RemoteFormEntryUtil.getPersonNames(doc, xp, enterer))
			patient.addName(personName);

		// create the person address
		for (PersonAddress address : RemoteFormEntryUtil.getPersonAddresses(doc, xp, enterer))
			patient.addAddress(address);

		// create and add the patient identifier
		patient.addIdentifiers(RemoteFormEntryUtil.getPatientIdentifiers(doc, xp, enterer));

		// create and add all person attributes
		for (PersonAttribute attr : RemoteFormEntryUtil.getPersonAttributes(doc, xp, enterer))
			patient.addAttribute(attr);

		// set the person properties (like gender, death status, birthdate, etc)
		RemoteFormEntryUtil.setPersonProperties(patient, doc, xp, enterer);

		// set the patient properties (like tribe, etc)
		RemoteFormEntryUtil.setPatientProperties(patient, doc, xp, enterer);

		// now finally create the person in the database
		Patient createdPatient = patientService.savePatient(patient);

		// TODO create the relationships
		for (Relationship rel : RemoteFormEntryUtil.getRelationships(createdPatient, doc, xp, enterer)) {
			Context.getPersonService().saveRelationship(rel);
		}

		// TODO create the program/workflow additions
		// createProgramWorkflowEnrollment(createdPatient, doc, xp, enterer);

		return createdPatient;
	}

	/**
	 * @throws XPathExpressionException
	 * @see org.openmrs.module.remoteformentry.RemoteFormEntryService#updatePatientInDatabase(org.openmrs.Patient,
	 *      org.w3c.dom.Document, javax.xml.xpath.XPath)
	 */
	public void updatePatientInDatabase(Patient patient, Document doc, XPath xp)
	        throws XPathExpressionException, Exception {
		// loop over all possible things that could have been changed and
		// update them for this patient object

		PatientService patientService = Context.getPatientService();

		User enterer = RemoteFormEntryUtil.getEnterer(doc, xp);

		// add the person name if the patient doesn't have it yet
		for (PersonName newPersonName : RemoteFormEntryUtil.getPersonNames(doc, xp, enterer)) {
			boolean found = false;
			for (PersonName currentName : patient.getNames()) {
				if (currentName.equalsContent(newPersonName)) {
					found = true;
					if (newPersonName.isVoided()) {
						currentName.setVoided(true);
						currentName.setVoidedBy(enterer);
						currentName.setDateVoided(new Date());
					}
					currentName.setPreferred(newPersonName.getPreferred());
					break;
				}
			}
			
			if (!found)
				patient.addName(newPersonName);
			
		}

		// add the person address if patient doesn't have it yet
		for (PersonAddress newPersonAddress : RemoteFormEntryUtil.getPersonAddresses(doc, xp, enterer)) {
			boolean found = false;
			for (PersonAddress currentAddress : patient.getAddresses()) {
				if (currentAddress.equalsContent(newPersonAddress)) {
					found = true;
					if (newPersonAddress.isVoided()) {
						currentAddress.setVoided(true);
						currentAddress.setVoidedBy(enterer);
						currentAddress.setDateVoided(new Date());
					}
					currentAddress.setPreferred(newPersonAddress.getPreferred());
					break;
				}
			}
			
			if (!found)
				patient.addAddress(newPersonAddress);
			
		}

		// add the patient identifier if patient doesn't have it yet
		for (PatientIdentifier newPersonIdentifier : RemoteFormEntryUtil.getPatientIdentifiers(doc, xp, enterer)) {
			boolean found = false;
			for (PatientIdentifier currentIdentifier : patient.getIdentifiers()) {
				if (currentIdentifier.equalsContent(newPersonIdentifier)) {
					found = true;
					if (newPersonIdentifier.isVoided()) {
						currentIdentifier.setVoided(true);
						currentIdentifier.setVoidedBy(enterer);
						currentIdentifier.setDateVoided(new Date());
					}
					currentIdentifier.setPreferred(newPersonIdentifier.getPreferred());
					break;
				}
			}
			
			if (!found)
				patient.addIdentifier(newPersonIdentifier);
			
		}
		
		// add all person attributes if patient doesn't have them yet
		for (PersonAttribute newPersonAttribute : RemoteFormEntryUtil.getPersonAttributes(doc, xp, enterer)) {
			boolean found = false;
			for (PersonAttribute currentAttribute : patient.getAttributes()) {
				// we want to use .equals() here instead of .equalsContent() because
				// of the "voided" attribute needing to be included in the equalsContent
				if (currentAttribute.equals(newPersonAttribute)) {
					found = true;
					if (newPersonAttribute.isVoided()) {
						currentAttribute.setVoided(true);
						currentAttribute.setVoidedBy(enterer);
						currentAttribute.setDateVoided(new Date());
					}
					break;
				}
			}
			
			if (!found && newPersonAttribute != null)
				patient.addAttribute(newPersonAttribute);
			
		}
		
		// set the person properties (like gender, death status, birthdate, etc)
		RemoteFormEntryUtil.setPersonProperties(patient, doc, xp, enterer);

		// set the patient properties (like tribe, etc)
		RemoteFormEntryUtil.setPatientProperties(patient, doc, xp, enterer);

		// now finally save the person in the database
		patientService.savePatient(patient);

		// Doing this after saving the patient so we're sure to have primary keys for both
		// this new patient and the other person relation
		// add all relationships if patient doesn't have them yet
		for (Relationship newRelationship : RemoteFormEntryUtil.getRelationships(patient, doc, xp, enterer)) {
			boolean found = false;
			for (Relationship rel : Context.getPersonService().getRelationshipsByPerson(patient)) {
				// we want to use .equals() here instead of .equalsContent() because
				// of the "voided" attribute needing to be included in the equalsContent
				if (equalsContent(rel, newRelationship)) {
					found = true;
					if (newRelationship.isVoided() && !rel.isVoided()) {
						rel.setVoided(true);
						rel.setVoidedBy(enterer);
						rel.setDateVoided(new Date());
					}
					break;
				}
			}
			
			if (!found && newRelationship != null)
				Context.getPersonService().saveRelationship(newRelationship);
		}
		
		// TODO add the program/workflow additions
		// createProgramWorkflowEnrollment(createdPatient, doc, xp, enterer);

	}
	
	/**
	 * Compares personA's, personB's, and relationship type
	 * 
	 * @param rel first side
	 * @param newRelationship other side
	 * @return true if these are really the same relationship
	 */
	private boolean equalsContent(Relationship rel, Relationship newRelationship) {
	    return rel.getPersonA().equals(newRelationship.getPersonA()) &&
	    	rel.getPersonB().equals(newRelationship.getPersonB()) &&
	    	rel.getRelationshipType().equals(newRelationship.getRelationshipType());
    }

	/**
	 * @see org.openmrs.module.remoteformentry.RemoteFormEntryService#receiveGeneratedDataFromCentralForLocation(java.io.File)
	 */
	public void receiveGeneratedDataFromCentralForLocation(File generatedDataFile) {
		synchronized (isGeneratingDataFile) {
			dao.execGeneratedFile(generatedDataFile);
		}
	}
	
	/**
	 * @see org.openmrs.module.remoteformentry.RemoteFormEntryService#receiveGeneratedDataFromCentral(java.io.File)
	 */
	public void receiveGeneratedDataFromCentral(File generatedDataFile) {
		// parse and do stuff with the sql file
		// global properties for formentry and remoteformentry should be
		// preserved when doing this update
		synchronized (isGeneratingDataFile) {

			// save the formentry and remoteformentry global properties to
			// a local variable
			AdministrationService as = Context.getAdministrationService();
			List<GlobalProperty> properties = as.getAllGlobalProperties();
			Map<String, String> savedProperties = new HashMap<String, String>();
			String[] prefixesToSave = { "formentry", "remoteformentry" };

			for (GlobalProperty prop : properties) {
				for (String prefix : prefixesToSave) {
					if (prop.getProperty().startsWith(prefix)) {
						savedProperties.put(prop.getProperty(),
						                    prop.getPropertyValue());
					}
				}
			}
			
			// save the scheduler startup/started properties to a local variable
			// and replace them afterwards
			Map<Integer, Boolean> startOnStartupTasks = new HashMap<Integer, Boolean>();
			Map<Integer, Boolean> startedTasks = new HashMap<Integer, Boolean>();
			SchedulerService schedService = Context.getSchedulerService();
			Collection <TaskDefinition> tasks = schedService.getRegisteredTasks();
			if (tasks != null) {
				for ( TaskDefinition task : tasks ) { 
					startOnStartupTasks.put(task.getId(), task.getStartOnStartup());
					startedTasks.put(task.getId(), task.getStarted());
				}
			}
			
			// exec mysql again to run the script and overwrite the database?
			dao.execGeneratedFile(generatedDataFile);
            
			// Clear the session so no ambiguous data gets saved at the end of
			// the transaction
			Context.clearSession();
			
			// save the formentry and remoteformentry properties back in the
			// database
			for (Entry<String, String> entry : savedProperties.entrySet()) {
				as.saveGlobalProperty(new GlobalProperty(entry.getKey(), entry.getValue()));
			}
			
			// force this session to flush/commit so that these global properties are not
			// rolled back.
			dao.commitSession();
			
			// rebuild all of the xsns after the data dump because they are most
			// likely customized for this remote server
			FormService formService = Context.getFormService();
			Integer count = 0;
			for (Form formObj : formService.getAllForms(false)) {
				if (log.isDebugEnabled())
					log.debug("Rebuilding form: " + formObj);
				
				try {
					Object[] streamAndDir = FormEntryUtil.getCurrentXSN(formObj, false);
					InputStream formStream = (InputStream) streamAndDir[0];
					File tempDir = (File) streamAndDir[1];
					if (formStream != null) {
						PublishInfoPath.publishXSN(formStream);
						count = count + 1;
						try {
							OpenmrsUtil.deleteDirectory(tempDir);
						} catch (IOException ioe) {}
						
						try {
							formStream.close();
						} catch (IOException ioe) {}
					}
					else if (log.isDebugEnabled())
						log.debug("Unable to rebuild form: " + formObj + " because the xsn stream is null");
					
				} 
				catch (IOException ioe) {
					log.warn("Unable to rebuild the xsn: " + formObj.getFormId());
				}
			}
			if (log.isDebugEnabled())
				log.debug(count + " xsn(s) rebuilt");
			
			// force this session to flush/commit so that these forms are not
			// rolled back.
			dao.commitSession();
			
			// put the scheduler settings back into the database
			tasks = schedService.getRegisteredTasks();
			
			if (log.isDebugEnabled())
				log.debug("Found " + tasks + " tasks to update");
			
			if (tasks != null) {
				for ( TaskDefinition task : tasks ) { 
					if (log.isDebugEnabled())
						log.debug("Updating task: " + task);
					
					Boolean started = startedTasks.get(task.getId());
					if (started == null)
						started = false;
						
					task.setStarted(started);
					
					Boolean startOnStartup = startOnStartupTasks.get(task.getId());
					if (startOnStartup == null)
						startOnStartup = false;
					
					task.setStartOnStartup(startOnStartup);
					
					schedService.saveTask(task);
				}
			}
			
		}

	}

	/**
	 * @see org.openmrs.module.remoteformentry.RemoteFormEntryService#generateDataFile()
	 */
	public void generateDataFile() {
		synchronized (isGeneratingDataFile) {

			File outFile = RemoteFormEntryUtil.getGeneratedReturnDataFile();

			dao.generateDataFile(outFile);
			
			// loop over the user defined remote locations
			for (Location location : RemoteFormEntryUtil.getRemoteLocations().keySet()) {
				File outFolderForLocation = RemoteFormEntryUtil.getGeneratedReturnDataFolderForLocation(location);
				dao.generateDataFileForLocation(outFolderForLocation, location);
			}
		}
	}

	/**
	 * @see org.openmrs.module.remoteformentry.RemoteFormEntryService#receiveAckDirFromCentral(java.io.File)
	 */
	public void receiveAckDirFromCentral(File ackDir) {
		// assuming we're on a remote server and need to remove the acked files
		// from the pending queue

		if (!ackDir.isDirectory())
			return;

		List<RemoteFormEntryPendingQueue> pendingQueueItems = getRemoteFormEntryPendingQueues();

		if (pendingQueueItems.size() > 0) {

			// TODO: is the first file the oldest file always?
			Date oldestDateModified = pendingQueueItems.get(0).getDateCreated();
			long oldestModified = oldestDateModified.getTime();

			File[] files = ackDir.listFiles();

			List<String> filenames = new ArrayList<String>();

			for (int i = files.length - 1; i >= 0; i--) {
				File file = files[i];
				// if the current file was modified before the current pending
				// queue item, quit out of the loop
				// TODO commented out this logic to make sure that this isn't screwing things up
				//if (file.lastModified() < oldestModified)
				//	break;

				// open file and split out the filenames
				try {
					String contents = OpenmrsUtil.getFileAsString(file);
					String[] nameArray = contents.split(RemoteFormEntryConstants.ACK_FILENAME_SEPARATOR);
					for (String name : nameArray) {
						if (name != null && name.length() > 0)
							filenames.add(name);
					}
				} catch (Exception e) {
					log.debug("Uh oh, error getting ack file contents: "
					        + file.getAbsolutePath(), e);
				}
			}

			FormEntryService fes = (FormEntryService) Context.getService(FormEntryService.class);

			// loop over each filename in the ack file and remove each from the 
			// pending queue here on the remote server
			for (String filename : filenames) {
				for (RemoteFormEntryPendingQueue queue : pendingQueueItems) {
					// if this filename matches the pending queue filename
					String fileSystemUrl = queue.getFileSystemUrl();
					String fileSystemName = WebUtil.stripFilename(fileSystemUrl);
					if (fileSystemName.equals(filename)) {
						// create and save formentry archive
						FormEntryArchive archive = new FormEntryArchive();
						archive.setFileSystemUrl(queue.getFileSystemUrl());
						fes.createFormEntryArchive(archive);

						// remote the pending queue item
						deleteRemoteFormEntryPendingQueue(queue);
					}
				}
			}
		}
	}

	/**
	 * @see org.openmrs.module.remoteformentry.RemoteFormEntryService#createAckFile(org.openmrs.Location,
	 *      java.util.List)
	 */
	public void createAckFile(Location location, List<String> filenames) {
		File dir = RemoteFormEntryUtil.getAckDir(location);

		if (filenames.size() == 0)
			return;

		// use the first object's filename as the name of the file
		// change the extension to 'ack'
		String filename = filenames.get(0);
		Integer period = filename.lastIndexOf(".");
		if (period != -1)
			filename = filename.substring(0, period) + ".ack";
		else
			filename = filename + ".ack";
		File outFile = new File(dir, filename);

		Writer writer = null;
		try {
			writer = new FileWriter(outFile);
			for (String ackName : filenames) {
				writer.append(ackName
				        + RemoteFormEntryConstants.ACK_FILENAME_SEPARATOR);
			}
			writer.close();
		} catch (Exception e) {
			log.warn("Unable to write ack file", e);
		} finally {
			try {
				writer.close();
			} catch (Exception e) {
				// essentally swallow the error
				log.debug("Error writing ack file", e);
			}
		}
	}

	/**
	 * @see org.openmrs.module.remoteformentry.RemoteFormEntryService#getLocationId()
	 */
	public Integer getLocationId() {
		AdministrationService as = Context.getAdministrationService();
		String id = as.getGlobalProperty(RemoteFormEntryConstants.GP_LOCATION_ID);

		if (id != null && id.length() > 0) {
			if (log.isDebugEnabled())
				log.debug("Loaded location id from global properties: " + id);
			return Integer.valueOf(id);
		}

		// return null if no location id was stored or not location was found
		return null;
	}

	/**
	 * @see org.openmrs.module.remoteformentry.RemoteFormEntryService#setLocationId(java.lang.Integer)
	 */
	public void setLocationId(Integer locationId) {
		AdministrationService as = Context.getAdministrationService();
		as.saveGlobalProperty(new GlobalProperty(RemoteFormEntryConstants.GP_LOCATION_ID,
		                     locationId.toString()));
	}

	/**
	 * @see org.openmrs.module.remoteformentry.RemoteFormEntryService#createFormEntryQueueForPatient(java.lang.String,
	 *      org.openmrs.Patient)
	 */
	public void createFormEntryQueueForPatient(String formData, Patient patient) {
		FormEntryService formEntryService = (FormEntryService) Context.getService(FormEntryService.class);

		// move this pending item to the formentry queue
		FormEntryQueue queue = new FormEntryQueue();

		// add the possibly new patient_id to the document so the subsequent
		// processing
		// by the formentry processor finds the right patient
		queue.setFormData(RemoteFormEntryUtil.replacePatientIdInDocument(patient.getPatientId(),
		                                                                 formData));
		queue.setCreator(Context.getAuthenticatedUser());
		queue.setDateCreated(new Date());

		formEntryService.createFormEntryQueue(queue);
	}

}
