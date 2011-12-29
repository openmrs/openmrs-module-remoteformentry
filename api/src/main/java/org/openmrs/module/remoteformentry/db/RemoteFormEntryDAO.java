package org.openmrs.module.remoteformentry.db;

import java.io.File;

import org.openmrs.Location;
import org.openmrs.module.remoteformentry.RemoteFormEntryService;

/**
 * Data access layer for the RemoteFormEntryService
 * 
 * This this should not be called or used directly.  All calls
 * should go through the methods on the RemFES
 * 
 * @see RemoteFormEntryService
 */
public interface RemoteFormEntryDAO {

	/**
     * @see org.openmrs.module.remoteformentry.RemoteFormEntryService#generateDataFile()
     */
    public void generateDataFile(File outFile);
    
    /**
     * Will create a series of sql files in this folder that are specific
     * dumps of tables with data that only the given location cares about
     * 
     * @see org.openmrs.module.remoteformentry.RemoteFormEntryService#generateDataFile()
     */
    public void generateDataFileForLocation(File outFolder, Location location);

	/**
     * Runs the sql contained in the given generatedDataFile
     * 
     * @param generatedDataFile sql file to run on the database
     * @see org.openmrs.module.remoteformentry.RemoteFormEntryService#receieveGeneratedDataFromCentral(File)
     */
    public void execGeneratedFile(File generatedDataFile);

    /**
     * Convenience method to clear/commit the current session to the database.
     */
    public void commitSession();
}
