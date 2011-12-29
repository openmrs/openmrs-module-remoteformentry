package org.openmrs.module.remoteformentry;

import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.scheduler.tasks.AbstractTask;

/**
 * Calls the service method to generate the return data file
 * 
 * Will only run if the time is between 1 and 5 am
 */
public class GenerateReturnDataFileTask extends AbstractTask {

	// Logger
	private static Log log = LogFactory.getLog(GenerateReturnDataFileTask.class);

	/**
	 * Process the next form entry in the database and then remove the form
	 * entry from the database.
	 */
	public void execute() {
		Context.openSession();
		log.debug("Generating return data ... ");
		try {
			if (Context.isAuthenticated() == false)
				authenticate();
			
			// the openmrs task system has a bug right now that runs all tasks at
			// start up (instead of just starting it)
			// check here to make sure its the middle of the night before we run this
			
			Calendar calendar = Calendar.getInstance();
			int hour = calendar.get(Calendar.HOUR_OF_DAY);
			if (hour >= 1 && hour < 5) {
				// do the file generating
				RemoteFormEntryService remoteService = (RemoteFormEntryService)Context.getService(RemoteFormEntryService.class);
				remoteService.generateDataFile();
			}
			else {
				log.error("The current time is not between 1am and 5am so the generation is not allowed to happen");
			}
			
			
		} catch (APIException e) {
			log.error("Error running generate return data task", e);
			throw e;
		} finally {
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
