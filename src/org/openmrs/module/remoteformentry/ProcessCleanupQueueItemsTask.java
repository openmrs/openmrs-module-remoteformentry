package org.openmrs.module.remoteformentry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.scheduler.tasks.AbstractTask;

/**
 * Clean up the encounters+obs that are pointed to by these xml files
 * 
 * @see org.openmrs.module.remoteformentry.RemoteFormEntryCleanupProcessor
 */
public class ProcessCleanupQueueItemsTask extends AbstractTask {

	// Logger
	private static Log log = LogFactory.getLog(ProcessCleanupQueueItemsTask.class);

	// Instance of form processor
	private RemoteFormEntryCleanupProcessor processor = null;
	
	/**
	 * 
	 */
	public ProcessCleanupQueueItemsTask() {
		if (processor == null)
			processor = new RemoteFormEntryCleanupProcessor();
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
			processor.processRemoteFormEntryCleanupQueue();
			
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
