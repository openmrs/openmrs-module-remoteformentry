package org.openmrs.module.remoteformentry.web.controller;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.remoteformentry.RemoteFormEntryUtil;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.mvc.SimpleFormController;

/**
 * Controls the form that provides a link to force the regeneration of the return data
 */
public class GenerateReturnDataFormController extends SimpleFormController {
	
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
    	Map<String, Object> map = new HashMap<String, Object>();
    	
    	File generatedData = RemoteFormEntryUtil.getGeneratedReturnDataFile();
		map.put("lastModified", new Date(generatedData.lastModified()));
    	
	    return map;
    }

}
