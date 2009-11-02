package org.openmrs.module.remoteformentry.web.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.EncounterType;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.formentry.ProcessFormEntryQueueTask;
import org.openmrs.module.remoteformentry.RemoteFormEntryService;
import org.openmrs.module.remoteformentry.RemoteFormEntryUtil;
import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.web.WebConstants;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

/**
 * Sets the properties for the remote form entry module like setting 
 * the list of initial encounters
 */
public class PropertiesFormController extends SimpleFormController {
	
    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());

    /**
     * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
     */
    protected Object formBackingObject(HttpServletRequest request) throws ServletException {
		
    	// the form backing object is an integer that is the size of the form entry queue
    	
    	return "";
    }
    
    /**
     * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    @Override
    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object object, BindException bindException) throws Exception {
	    
    	// user must be authenticated (avoids auth errors)
    	if (Context.isAuthenticated()) {
    		
			// save the list of initial encounter types
    		
    		String[] encounterTypeIdsStrings = ServletRequestUtils.getStringParameters(request, "encounterTypeId");
    		
    		List<Integer> encounterTypeIds = new ArrayList<Integer>();
    		
    		for (String typeId : encounterTypeIdsStrings) {
    			encounterTypeIds.add(Integer.valueOf(typeId));
    		}
    		
    		// save the initial encounter types
    		RemoteFormEntryService remoteService = (RemoteFormEntryService) Context.getService(RemoteFormEntryService.class);
    		remoteService.setInitialEncounterTypes(encounterTypeIds);
    		
    		// save the location id
    		Integer locationId = ServletRequestUtils.getIntParameter(request, "locationId");
    		if (locationId != null)
    			remoteService.setLocationId(locationId);
    		
        	HttpSession httpSession = request.getSession();
			httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "remoteformentry.properties.saved");
    	}
    	
	    return showForm(request, response, bindException);
    }

	/**
     * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest, java.lang.Object, org.springframework.validation.Errors)
     */
    @Override
    protected Map referenceData(HttpServletRequest arg0, Object arg1, Errors arg2) throws Exception {
    	
    	Map<String, Object> map = new HashMap<String, Object>();
    	Map<EncounterType, Boolean> encounterTypes = new HashMap<EncounterType, Boolean>();
    	
    	// get the list of encounter types
    	EncounterService encounterService = Context.getEncounterService();
    	List<EncounterType> allEncounterTypes = encounterService.getEncounterTypes();
    	
    	// get the list of encounter types marked as initial
    	RemoteFormEntryService remoteService = (RemoteFormEntryService) Context.getService(RemoteFormEntryService.class);
    	List<EncounterType> selectedEncounterTypes = remoteService.getInitialEncounterTypes();
    	
    	// mark selected encounter types
    	for (EncounterType encType : allEncounterTypes) {
    		encounterTypes.put(encType, selectedEncounterTypes.contains(encType));
    	}
    	
    	map.put("encounterTypes", encounterTypes);
    	
    	// get the location id for this remote location
    	map.put("locationId", remoteService.getLocationId());
    	
    	// get the remote locations defined 
    	map.put("remoteLocations", RemoteFormEntryUtil.getRemoteLocations());
    	
    	// get the central vs remote status
    	Boolean isCentral = RemoteFormEntryUtil.isCentralServer();
    	Boolean isRemote = RemoteFormEntryUtil.isRemoteServer();
    	map.put("isCentral", isCentral);
    	map.put("isRemote", isRemote);
    	
    	if (isRemote) {
	    	// check to see if the scheduler task is running.  If it is, 
	    	// the effectiveness of this module could be compromised
    		SchedulerService schedService = Context.getSchedulerService();
    		String processorTask = ProcessFormEntryQueueTask.class.getName();
    		boolean processorTaskScheduled = false;
    		for (TaskDefinition task : schedService.getScheduledTasks()) {
    			if (task.getTaskClass().equals(processorTask) &&
    					task.getStarted())
    				processorTaskScheduled = true;
    		}
    			
    		map.put("formentryProcessorEnabled", processorTaskScheduled);
    	}
    	
		return map;
    }
	
}
