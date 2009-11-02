package org.openmrs.module.remoteformentry;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.openmrs.util.OpenmrsUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Pulls the source_key uid out of formentry queue items
 * 
 * @see org.openmrs.module.remoteformentry.RemoteFormEntryCleanupProcessor
 */
public class SourceKeyExtractorTask extends AbstractTask {

	// Logger
	private static Log log = LogFactory.getLog(SourceKeyExtractorTask.class);
	
	private DocumentBuilderFactory documentBuilderFactory;
	private XPathFactory xPathFactory;
	private static Boolean isRunning = false; // allow only one running
	
	private AdministrationService adminService = null;

	/**
	 * Process the next form entry in the database and then remove the form
	 * entry from the database.
	 */
	public void execute() {
		Context.openSession();
		log.debug("Processing pending queue items (as central server) ... ");
		try {
			if (Context.isAuthenticated() == false)
				authenticate();
			
			adminService = Context.getAdministrationService();
			
			synchronized (isRunning) {
				if (isRunning) {
					log.warn("Processor aborting (another processor already running)");
					return;
				}
				isRunning = true;
			}
			try {
				log.debug("Start processing RemoteFormEntry pending queue");
				processAllRemoteFormEntryCleanupQueues();
				log.debug("Done processing RemoteFormEntry pending queue");
			}
			catch (Exception e) {
				log.error("Uh oh.  Got an error", e);
			}
			finally {
				isRunning = false;
			}
			
			
			
		} catch (APIException e) {
			log.error("Error processing pending queue task", e);
			throw e;
		}
		catch (Exception e) {
			log.error("Error while processing pending queue", e);
		}
		finally {
			Context.closeSession();
		}
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
		
		// First we parse the FormEntry xml data to obtain the formId of the
		// form that was used to create the xml data
		DocumentBuilderFactory dbf = getDocumentBuilderFactory();
		DocumentBuilder db = dbf.newDocumentBuilder();
		XPathFactory xpf = getXPathFactory();
		XPath xp = xpf.newXPath();
		Document doc = db.parse(IOUtils.toInputStream(formData));
		String uid = null;
		try {
			uid = xp.evaluate("/form/header/uid", doc);
		}
		catch (Throwable t) {
			log.error("Error!", t);
		}
						
		List<List<Object>> returnValues = adminService.executeSQL("select count(*) from temp_extracted_source_keys where uid = '" + uid + "'", false); 
		if ((Long)returnValues.get(0).get(0) < 1) {
			adminService.executeSQL("insert into temp_extracted_source_keys (uid) values ('" + uid + "')", false);
		}
			
	}

	/**
	 * Transform the next pending RemoteFormEntryPendingQueue entry. If there are no pending
	 * items in the queue, this method simply returns quietly.
	 * 
	 * @return true if a queue entry was processed, false if queue was empty
	 */
	public void processAllRemoteFormEntryCleanupQueues() throws Exception {
		String folder = adminService.getGlobalProperty(
				"remoteformentry.cleanup_queue_dir",
		        "cleanup_dir");
		
		File cleanupDir = OpenmrsUtil.getDirectoryInApplicationDataDirectory(folder);
		
		log.debug("Starting processing of cleanup dir...");
		int count = 0;
		for (File file : cleanupDir.listFiles()) {
			RemoteFormEntryPendingQueue rfeq = new RemoteFormEntryPendingQueue();
			rfeq.setFileSystemUrl(file.getAbsolutePath());
			cleanupRemoteFormEntryQueue(rfeq);
			count = count + 1;
		}
		
		log.debug("...Finished processing of cleanup dir.");
		log.debug("processed " + count + " files");
		
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
	 * Clean up any resources here
	 *
	 */
	public void shutdown() {
		
	}
	
}
