package org.openmrs.module.remoteformentry;

/**
 * Represents often fatal errors that occur within the remote form 
 * entry module
 * 
 * If this error is found in the formentry_error_queue, this entry
 * is displayed on the ResolveRemoteFormEntryQueueErrors page
 * 
 */
public class RemoteFormEntryException extends RuntimeException {

	public static final long serialVersionUID = 12121234222L;

	public RemoteFormEntryException() {
	}

	public RemoteFormEntryException(String message) {
		super(message);
	}

	public RemoteFormEntryException(String message, Throwable cause) {
		super(message, cause);
	}

	public RemoteFormEntryException(Throwable cause) {
		super(cause);
	}

}
