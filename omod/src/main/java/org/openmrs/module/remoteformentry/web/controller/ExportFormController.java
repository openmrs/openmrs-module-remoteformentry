package org.openmrs.module.remoteformentry.web.controller;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.formentry.FormEntryService;
import org.openmrs.module.remoteformentry.RemoteFormEntryConstants;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.mvc.SimpleFormController;

/**
 * Controls the exporting of form entry queue items
 */
public class ExportFormController extends SimpleFormController {
	
    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());

    /**
     * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
     */
    protected Object formBackingObject(HttpServletRequest request) throws ServletException {
		
    	// the form backing object is an integer that is the size of the form entry queue
    	
    	Integer queueSize = 0;
		
		//only fill the objects if the user has authenticated properly
		if (Context.isAuthenticated()) {
			FormEntryService fs = (FormEntryService)Context.getService(FormEntryService.class);
			queueSize = fs.getFormEntryQueueSize();
		}
    	
		return queueSize;
    }

    /**
     * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest, java.lang.Object, org.springframework.validation.Errors)
     */
    protected Map<String, Object> referenceData(HttpServletRequest request, Object command, Errors errors) throws Exception {
		File remoteFormEntryDirectory = OpenmrsUtil.getDirectoryInApplicationDataDirectory("remoteformentry");
		File exportDirectory = new File(remoteFormEntryDirectory, RemoteFormEntryConstants.EXPORT_DIRECTORY_NAME);
		if (!exportDirectory.exists()) {
			exportDirectory.mkdir();
		}
		
		Map<String, Object> map = new HashMap<String, Object>();
		
		
		Map<String, Object> exportedFiles = new LinkedHashMap<String, Object>();
		for (File file : exportDirectory.listFiles()) {
			exportedFiles.put(file.getName(), new Date(file.lastModified()));
		}
		
		map.put("exportedFiles", exportedFiles);
		map.put("exportDirectory", exportDirectory.getAbsolutePath());
		
	    return map;
    }
    
    
	
}
