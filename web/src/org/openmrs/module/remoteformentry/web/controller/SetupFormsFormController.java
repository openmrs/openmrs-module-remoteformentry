package org.openmrs.module.remoteformentry.web.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Form;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.module.formentry.FormEntryUtil;
import org.openmrs.module.remoteformentry.FormConverter;
import org.openmrs.web.WebConstants;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

/**
 * Allows the user to add the needed form schema elements to selected forms
 * 
 */
public class SetupFormsFormController extends SimpleFormController {
	
    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());

    /**
     * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
     */
    protected Object formBackingObject(HttpServletRequest request) throws ServletException {
    	
    	// the form backing object is all UNretired forms in the database
    	return Context.getFormService().getForms(false);
    }
    
    /**
     * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object object, BindException bindException) throws Exception {
	    
    	// user must be authenticated (avoids auth errors)
    	if (Context.isAuthenticated()) {
    		HttpSession httpSession = request.getSession();
			
    		FormService formService = Context.getFormService();
    		
    		FormConverter formConverter = new FormConverter();
    		
    		String[] formIds = request.getParameterValues("formId");
    		if (formIds != null) {
	    		for (String formId : formIds) {
	    			Form form = formService.getForm(Integer.valueOf(formId));
	    			
	    			// make sure the form is unpublished
    				formConverter.addOrUpdateSchema(form);
	    			formService.updateForm(form);
	    			
	    			// get the new form fields into the xsn
	    			try {
	    				FormEntryUtil.rebuildXSN(form);
	    			}
	    			catch (IOException io) {
	    				log.warn("unable to rebuild the xsn for form" + form, io);
	    			}
	    			
	    			// at least one form was successful
	    			httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "remoteformentry.setupForms.formsSaved");
	    			// at least one form was not successful
	    			//httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "remoteformentry.setupForms.publishedAlready");
	    		}
    		}
    		else
    			// error. nothing to loop over.
    			httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "remoteformentry.setupForms.selectAForm");
    		
    	}
    	
	    return showForm(request, response, bindException);
    }

	/**
     * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest, java.lang.Object, org.springframework.validation.Errors)
     */
    protected Map referenceData(HttpServletRequest arg0, Object arg1, Errors arg2) throws Exception {
    	
    	Map<String, Object> map = new HashMap<String, Object>();
    	    	
		return map;
    }
	
}
