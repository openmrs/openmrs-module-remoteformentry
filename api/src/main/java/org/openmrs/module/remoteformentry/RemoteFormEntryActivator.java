package org.openmrs.module.remoteformentry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.Activator;

/**
 * Activator startup/shutdown methods for the remote form entry module
 */
public class RemoteFormEntryActivator implements Activator {

	private Log log = LogFactory.getLog(this.getClass());
	
	/**
	 * @see org.openmrs.module.Activator#startup()
	 */
	@SuppressWarnings("deprecation")
    public void startup() {
		log.info("Starting the Remote Form Entry module");
	}
	
	/**
	 *  @see org.openmrs.module.Activator#shutdown()
	 */
	@SuppressWarnings("deprecation")
    public void shutdown() {
		log.info("Shutting down the Remote Form Entry module");
	}
	
}
