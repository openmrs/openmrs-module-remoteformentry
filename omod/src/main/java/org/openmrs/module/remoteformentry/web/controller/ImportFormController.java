package org.openmrs.module.remoteformentry.web.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.remoteformentry.RemoteFormEntryConstants;
import org.openmrs.module.remoteformentry.RemoteFormEntryService;
import org.openmrs.module.remoteformentry.RemoteFormEntryUtil;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.web.WebConstants;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

/**
 * Controller to allow a zip file to be uploaded to the central server
 */
public class ImportFormController extends SimpleFormController {
	
    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());

    /**
     * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
     */
    protected Object formBackingObject(HttpServletRequest request) throws ServletException {
		
    	// this page has no need for data on the front end.  Its only submitting a zip file
        return "";
    }
    
	/**
     * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    @Override
    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object object, BindException bindException) throws Exception {
	    
    	// user must be authenticated (avoids auth errors)
    	if (Context.isAuthenticated()) {
    		
    		File remoteFormEntryDirectory = OpenmrsUtil.getDirectoryInApplicationDataDirectory("remoteformentry");
			File importDirectory = new File(remoteFormEntryDirectory, RemoteFormEntryConstants.IMPORT_DIRECTORY_NAME);
			if (!importDirectory.exists()) {
				importDirectory.mkdir();
			}
    		
			// they're uploading/importing a remote queue zip file...
			
			if (request instanceof MultipartHttpServletRequest) {
				RemoteFormEntryService remoteService = (RemoteFormEntryService)Context.getService(RemoteFormEntryService.class);
				MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest)request;
				MultipartFile queueUpload = multipartRequest.getFile("queueImport");
				if (queueUpload != null && !queueUpload.isEmpty()) {
					
					// copy the request file to the import dir in the application data dir
					File importingFileFromRequest = new File(importDirectory, queueUpload.getOriginalFilename());
					OutputStream importDirOutputStream = new FileOutputStream(importingFileFromRequest);
					InputStream inputStreamFromRequest = queueUpload.getInputStream();
					OpenmrsUtil.copyFile(inputStreamFromRequest, importDirOutputStream);
					inputStreamFromRequest.close();
					
					// open that newly saved file for input to the zip
					InputStream inputStreamFromFile = new FileInputStream(importingFileFromRequest);
					
					// load the files in the zip into the remoteformentry_pending_queue 
					// create the ack file for the next time this site is downloaded
					ZipInputStream zis = new ZipInputStream(inputStreamFromFile);
					
					// the names of the files uploaded 
					List<String> ackFilenames = new Vector<String>();
					
					// get the location from the zip file
					String locationId = "";
					
					File pendingDir = RemoteFormEntryUtil.getPendingQueueDir();
					
					int buffer = 2048;
					BufferedOutputStream dest = null;
					ZipEntry entry;
					while((entry = zis.getNextEntry()) != null) {
						String pathName = entry.getName();
						try {
							log.debug("Extracting: " + entry);
							if (pathName.startsWith("locationId-")) {
								locationId = pathName.substring(11);
							}
							else {
								ackFilenames.add(pathName);
								
								int count;
								byte data[] = new byte[buffer];

								// do the work
								// write the imported files files to the disk
								File file = new File(pendingDir, pathName);
								FileOutputStream fos = new FileOutputStream(file);
								dest = new BufferedOutputStream(fos, buffer);
								while ((count = zis.read(data, 0, buffer)) != -1) {
									dest.write(data, 0, count);
								}
								dest.flush();
								dest.close();
							}
						}
						catch(Exception e) {
							log.error("The pending dir couldn't be written to for: " + pathName, e);
						}
					}
					
					zis.close();
					inputStreamFromFile.close();
					
					Location location = null;
					if (locationId.length() > 0) {
						EncounterService encounterService = Context.getEncounterService();
						location = encounterService.getLocation(Integer.valueOf(locationId));
					}
					
					if (location == null)
						log.warn("Unable to find location with id: " + locationId);
					
					remoteService.createAckFile(location, ackFilenames);
					
					request.getSession().setAttribute(WebConstants.OPENMRS_MSG_ATTR, "remoteformentry.import.success");
				}
				else {
					request.getSession().setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "remoteformentry.import.selectFile");
				}
			}
			else {
				request.getSession().setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "Bad Request.  Are you using the right enctype on the form tag?");
			}
			
    	}
    	
	    return showForm(request, response, bindException);
    }

    /**
     * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest, java.lang.Object, org.springframework.validation.Errors)
     */
    protected Map<String, Object> referenceData(HttpServletRequest request, Object command, Errors errors) throws Exception {
		File remoteFormEntryDirectory = OpenmrsUtil.getDirectoryInApplicationDataDirectory("remoteformentry");
		File importDirectory = new File(remoteFormEntryDirectory, RemoteFormEntryConstants.IMPORT_DIRECTORY_NAME);
		if (!importDirectory.exists()) {
			importDirectory.mkdir();
		}
		
		Map<String, Object> map = new HashMap<String, Object>();
		
		
		Map<String, Object> importedFiles = new LinkedHashMap<String, Object>();
		for (File file : importDirectory.listFiles()) {
			importedFiles.put(file.getName(), new Date(file.lastModified()));
		}
		
		map.put("importedFiles", importedFiles);
		map.put("importDirectory", importDirectory.getAbsolutePath());
		
	    return map;
    }
	
}
