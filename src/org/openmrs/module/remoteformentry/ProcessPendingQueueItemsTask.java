package org.openmrs.module.remoteformentry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.scheduler.tasks.AbstractTask;

/**
 * Assuming this is on a central server, process the pending queue items and 
 * send the current patient ones to the normal formentry queue and send the 
 * ones that are missing patients to the normal formentry error queue
 * 
 * @see org.openmrs.module.remoteformentry.RemoteFormEntryPendingProcessor
 */
public class ProcessPendingQueueItemsTask extends AbstractTask {

	// Logger
	private static Log log = LogFactory.getLog(ProcessPendingQueueItemsTask.class);

	// Instance of form processor
	private RemoteFormEntryPendingProcessor processor = null;
	
	/**
	 * 
	 */
	public ProcessPendingQueueItemsTask() {
		if (processor == null)
			processor = new RemoteFormEntryPendingProcessor();
	}
	
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
			// do the processing
			processor.processRemoteFormEntryPendingQueue();
			
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
	 * Clean up any resources here
	 *
	 */
	public void shutdown() {
		
	}
	
}
