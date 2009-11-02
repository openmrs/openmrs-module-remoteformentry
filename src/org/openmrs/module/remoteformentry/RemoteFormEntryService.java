package org.openmrs.module.remoteformentry;

import java.io.File;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.remoteformentry.db.RemoteFormEntryDAO;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;

/**
 * Required methods for any remote form entry service
 */
@Transactional
public interface RemoteFormEntryService {

	public void setRemoteFormEntryDAO(RemoteFormEntryDAO dao);

	/**
	 * Creates a file in RemoteFormEntryConstants.GP_PENDING_QUEUE_DIR for the
	 * data in this queue item
	 * 
	 * @param RemoteFormEntryPendingQueue object containing form data to save in
	 *        the pending queue
	 */
	@Authorized( { RemoteFormEntryConstants.PRIV_IMPORT_REMOTE_FORM_ENTRY,
	        RemoteFormEntryConstants.PRIV_EXPORT_REMOTE_FORM_ENTRY })
	public void createRemoteFormEntryPendingQueue(
	        RemoteFormEntryPendingQueue pendingQueue);

	/**
	 * Delete the given pending queue item
	 * 
	 * @param RemoteFormEntryPendingQueue to delete
	 */
	@Authorized( { RemoteFormEntryConstants.PRIV_IMPORT_REMOTE_FORM_ENTRY,
	        RemoteFormEntryConstants.PRIV_EXPORT_REMOTE_FORM_ENTRY })
	public void deleteRemoteFormEntryPendingQueue(
	        RemoteFormEntryPendingQueue pendingQueue);

	/**
	 * Find and return all pending queue items
	 * 
	 * @return list of pending queue items
	 */
	@Transactional(readOnly = true)
	@Authorized( { RemoteFormEntryConstants.PRIV_IMPORT_REMOTE_FORM_ENTRY,
	        RemoteFormEntryConstants.PRIV_EXPORT_REMOTE_FORM_ENTRY })
	public List<RemoteFormEntryPendingQueue> getRemoteFormEntryPendingQueues();

	/**
	 * Find the next pending queue item to be processed
	 * 
	 * @return next queue item or null if none
	 */
	@Transactional(readOnly = true)
	@Authorized( { RemoteFormEntryConstants.PRIV_IMPORT_REMOTE_FORM_ENTRY,
	        RemoteFormEntryConstants.PRIV_EXPORT_REMOTE_FORM_ENTRY })
	public RemoteFormEntryPendingQueue getNextRemoteFormEntryPendingQueue();

	/**
	 * Get the number of queue items waiting to be processed
	 * 
	 * @return integer size of queue item list
	 */
	@Transactional(readOnly = true)
	@Authorized( { RemoteFormEntryConstants.PRIV_IMPORT_REMOTE_FORM_ENTRY,
	        RemoteFormEntryConstants.PRIV_EXPORT_REMOTE_FORM_ENTRY })
	public Integer getRemoteFormEntryPendingQueueSize();

	/**
	 * Returns the defined list of encounter types used by the remote form entry
	 * module
	 * 
	 * @return list of encounter types that are initial forms for patients
	 */
	public List<EncounterType> getInitialEncounterTypes();

	/**
	 * Defines the list of encounter types that are used by the remote form
	 * entry module
	 * 
	 * @param Integer that are the encounterTypeId objects that represent
	 *        initial forms
	 */
	public void setInitialEncounterTypes(List<Integer> encounterTypes);

	/**
	 * Creates a patient from the given form information
	 * 
	 * @param doc document that is the form
	 * @param xp xpath to aid in getting attributes
	 * @return newly created Patient
	 * @throws XPathExpressionException
	 */
	public Patient createPatientInDatabase(Document doc, XPath xp)
	        throws XPathExpressionException, Exception;

	/**
	 * Updates all of the patient data that is different between the given
	 * patient and the given patient's document
	 * 
	 * @param patient patient to modify
	 * @param doc document that is the form
	 * @param xp xpath to aid in getting the patient's attributes
	 * @throws XPathExpressionException 
	 */
	public void updatePatientInDatabase(Patient patient, Document doc, XPath xp) throws XPathExpressionException, Exception;

	/**
	 * Receive and act upon the generated data from the central.
	 * 
	 * The data in this file is assumed to be the same as what was made with
	 * #generateDataFile The formentry.* global properties should be preserved
	 * for this remote system
	 * 
	 * @param generatedData File where the generated sql file was uploaded
	 * @see #generateDataFile()
	 */
	public void receiveGeneratedDataFromCentral(File generatedData);
	
	/**
	 * Receive and act upon the generated data from the central.
	 * 
	 * The data in this file is assumed to be the same as what was made with
	 * #generateDataFileForLocation
	 * 
	 * @param generatedData File where the generated sql file was uploaded
	 * @see #generateDataFileForLocation()
	 */
	public void receiveGeneratedDataFromCentralForLocation(File generatedData);
	
	/**
	 * Create a data file to return to the remote sites.  This will probably be
	 * time intensive and should be run once a day
	 * 
	 * @see #receieveGeneratedDataFromCentral(File)
	 */
	public void generateDataFile();
	
	/**
	 * Receive and act upon the ack files. All pending formentry queue items
	 * that are in this ack file should be moved to the formentry archive
	 * 
	 * @param ackDir File pointing at the temp directory where the ack files
	 *        were uploaded
	 * @see #generatedAckFile
	 */
	public void receiveAckDirFromCentral(File ackDir);

	/**
     * Create an ack file for the given location with the given filenames
     * 
     * @param location Location where these files were uploaded from
     * @param filenames List<String> filenames to put in the ack file
     */
    public void createAckFile(Location location, List<String> filenames);

    /**
     * Gets the defined location id
     * Returns null if none defined
     * 
     * @return Integer location id of this remote server
     */
    public Integer getLocationId();
    
    /**
     * Set the location id for this remote server.  
     * 
     * @param locationId The location id of the defined server.  
     */
    public void setLocationId(Integer locationId);
	
    /**
     * Assigns the given pending queue item to the given patient, creates 
     * a formentry queue item (so it can be processed now by formentry normally)
     * and then deletes the pending queue item
     * 
     * @param formData String data on the form which to modify and then put in the formentry queue item
     * @param patient Patient new/updated patient that this queue item should be attributed to
     */
    public void createFormEntryQueueForPatient(String formData, Patient patient);

}
