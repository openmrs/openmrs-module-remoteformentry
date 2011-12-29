package org.openmrs.module.remoteformentry.web.controller;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.remoteformentry.RemoteFormEntryConstants;
import org.openmrs.module.remoteformentry.RemoteFormEntryUtil;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.mvc.SimpleFormController;

/**
 * Controls the downloading of the data to return to the remote sites
 */
public class ReturnDataToRemoteFormController extends SimpleFormController {
	
    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());
    
	/**
     * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
     */
    protected Object formBackingObject(HttpServletRequest request) throws Exception {
    	return "";
    }

	/**
     * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest, java.lang.Object, org.springframework.validation.Errors)
     */
    protected Map<String, Object> referenceData(HttpServletRequest request, Object command, Errors errors) throws Exception {
	    // 
    	Map<String, Object> map = new HashMap<String, Object>();
    	
    	Boolean staleData = false;
    	
    	File generatedData = RemoteFormEntryUtil.getGeneratedReturnDataFile();
    	
    	Calendar calendar = Calendar.getInstance();
    	calendar.add(Calendar.DATE, -2);
    	
    	if (calendar.getTimeInMillis() > generatedData.lastModified()) {
    		staleData = true;
    		map.put("lastModified", new Date(generatedData.lastModified()));
    	}
    	
    	map.put("staleData", staleData);
    	
    	// get the remote locations defined 
    	map.put("remoteLocations", RemoteFormEntryUtil.getRemoteLocations());
    	
    	
    	File remoteFormEntryDirectory = OpenmrsUtil.getDirectoryInApplicationDataDirectory("remoteformentry");
		File returnedDataDirectory = new File(remoteFormEntryDirectory, RemoteFormEntryConstants.RETURNED_DATA_DIRECTORY_NAME);
		if (!returnedDataDirectory.exists()) {
			returnedDataDirectory.mkdir();
		}
		
		Map<String, Object> returnedFiles = new LinkedHashMap<String, Object>();
		for (File file : returnedDataDirectory.listFiles()) {
			returnedFiles.put(file.getName(), new Date(file.lastModified()));
		}
		
		map.put("returnedFiles", returnedFiles);
		map.put("returnDirectory", returnedDataDirectory.getAbsolutePath());
		
	    return map;
    }

}
