package org.openmrs.module.remoteformentry.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.formentry.FormEntryQueue;
import org.openmrs.module.formentry.FormEntryService;
import org.openmrs.module.remoteformentry.RemoteFormEntryConstants;
import org.openmrs.module.remoteformentry.RemoteFormEntryException;
import org.openmrs.module.remoteformentry.RemoteFormEntryPendingQueue;
import org.openmrs.module.remoteformentry.RemoteFormEntryService;
import org.openmrs.module.remoteformentry.RemoteFormEntryUtil;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.web.WebConstants;
import org.openmrs.web.WebUtil;

/**
 * Downloads all FormEntryQueue items in a zipped form.  All zipped queue items
 * are moved from the queue to the archive 
 * 
 * The zipped queue items are meant to be taken to another server and uploaded 
 * and processed using the same RemoteFormEntryForm that this 
 */
public class QueueDownloadServlet extends HttpServlet {

	public static final long serialVersionUID = 123423L;

	private static final Log log = LogFactory.getLog(QueueDownloadServlet.class);

	/**
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		if (Context.isAuthenticated() == false) {
			HttpSession httpSession = request.getSession();
			httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR,
					"auth.session.expired");
			response.sendRedirect(request.getContextPath() + "/logout");
			return;
		}
		
		log.debug("Getting formentry queue items");
		
		try {
			// get the defined location for this remote site
			RemoteFormEntryService remoteService = (RemoteFormEntryService)Context.getService(RemoteFormEntryService.class);
			Integer locationId = remoteService.getLocationId();
			if (locationId == null)
				throw new RemoteFormEntryException("You must have the location id defined.  See Remote Form Entry Properties page");
			Location location = Context.getEncounterService().getLocation(Integer.valueOf(locationId));
			if (location == null)
				throw new RemoteFormEntryException("The defined location id for this remote site is invalid.  There is no location defined with id: " + locationId);
			
			File remoteFormEntryDirectory = OpenmrsUtil.getDirectoryInApplicationDataDirectory("remoteformentry");
			File exportDirectory = new File(remoteFormEntryDirectory, RemoteFormEntryConstants.EXPORT_DIRECTORY_NAME);
			if (!exportDirectory.exists()) {
				exportDirectory.mkdir();
			}
			
			// set up the response so the user/browser doesn't get ancy while waiting 
			// for the zip file to be generated
			response.setHeader("Content-Type", "application/zip");
			String outputFilename = "formEntryQueues-from-remote-site-" + RemoteFormEntryUtil.getDownloadSuffix(location);
			response.setHeader("Content-Disposition", "attachment; filename=" + outputFilename + ".zip");
			
			File outputFile = new File(exportDirectory, outputFilename + ".zip");
			FileOutputStream zipFileOutputStream = new FileOutputStream(outputFile);
			ZipOutputStream zos	= new ZipOutputStream(zipFileOutputStream);
			FormEntryService fs	= (FormEntryService)Context.getService(FormEntryService.class);
			
			Collection<FormEntryQueue> queueItems = fs.getFormEntryQueues();
			
			// they clicked the button and there aren't any queue items
			if (queueItems.size() < 1)
				throw new RemoteFormEntryException("There are no queue items available at this time.  Perhaps they were downloaded already?");
			
			 byte[] buf = new byte[1024];
			 
			 int cleanupInterval = 200;
			 int counter = 0;

			// loop over all queue items and add each to the zip file.  The
			// zip file is stored on disk in a tmp folder and then downloaded to 
			// prevent all of that backed up into java memory
			for (FormEntryQueue queueItem : queueItems) {

				String fileSystemUrl = queueItem.getFileSystemUrl();
				
				// if file is on the filesystem, do the zipping in this memory conscious way
				if (fileSystemUrl != null) {
					
					String filename = WebUtil.stripFilename(queueItem.getFileSystemUrl());
					
					if (log.isDebugEnabled())
						log.debug("Writing filesystem file to zip file: " + fileSystemUrl);
					
					FileInputStream in = new FileInputStream(fileSystemUrl);
				    
		            // Add ZIP entry to output stream.
					zos.putNextEntry(new ZipEntry(filename));
		    
		            // Transfer bytes from the file to the ZIP file
		            int len;
		            while ((len = in.read(buf)) > 0) {
		            	zos.write(buf, 0, len);
		            }
		            
		         // Complete the entry
					zos.closeEntry();
		            in.close();
		            
				}
				else {
					// there is no filesystem url...so we're operating on a string for some reason

					// formData of queue
					String formData = queueItem.getFormData();
			        
					byte [] uncompressedBytes = formData.getBytes();
			        
					String filename = WebUtil.stripFilename(fileSystemUrl);
					
					if (log.isDebugEnabled())
						log.debug("Zipping queue item: " + filename);
					
					// name this entry the same as the queue item
			        ZipEntry zipEntry = new ZipEntry(filename);
			        
			        // Add ZIP entry to output stream.
		            zos.putNextEntry(zipEntry);
		    
		            // Transfer bytes from the formData to the ZIP file
		            zos.write(uncompressedBytes, 0, uncompressedBytes.length);
		            
		            zos.closeEntry();
				}
								
				if (counter++ > cleanupInterval) {
					counter = 0;
					System.gc();
				}
	            
			}
			
			// add location id file to the zip
			String filename = "locationId-" + locationId;
			ZipEntry zipEntry = new ZipEntry(filename);
			byte[] filenameBytes = filename.getBytes();
	        
		    // Add ZIP entry to output stream.
	        zos.putNextEntry(zipEntry);
	        
	        // Add the location file content to the ZIP file
	        zos.write(filenameBytes, 0, filenameBytes.length);
	        zos.closeEntry();
			
			// loop over queue items and move them to the archive
			for (FormEntryQueue queueItem : queueItems) {
				// move the queueItem to the remote entry pending queue
				RemoteFormEntryPendingQueue pendingQueue = new RemoteFormEntryPendingQueue();
				pendingQueue.setFormData(queueItem.getFormData());
				pendingQueue.setCreator(queueItem.getCreator());
				pendingQueue.setDateCreated(queueItem.getDateCreated());
				pendingQueue.setFileSystemUrl(queueItem.getFileSystemUrl()); // so that when creating it, it uses the same filename as was passed to central
				remoteService.createRemoteFormEntryPendingQueue(pendingQueue);
				log.debug("Done creating remote pending queue item: " + pendingQueue.getFileSystemUrl());
				
				// delete the queue item so its not downloaded again
				fs.deleteFormEntryQueue(queueItem);
				log.debug("Deleted queue item: " + queueItem.getFileSystemUrl());
				
				if (counter++ > cleanupInterval) {
					counter = 0;
					System.gc();
				}
				
			}
			
			log.debug("Closing zos");
			zos.close();
			// after downloading, redirect the user back to the same page so the # of queue items refreshes
			// redirect must be done in javascript/html in order to have the zip printed to the output stream
			
			// close the file for writing so that we can read from it
			zipFileOutputStream.close();
			
			// set the size on the response so the user knows how much is to
			// be downloaded and how much is left to go
			response.setContentLength((int)outputFile.length());
			
			// copy the file to the user's request
			FileInputStream tmpFileInputStream = new FileInputStream(outputFile);
			OpenmrsUtil.copyFile(tmpFileInputStream, response.getOutputStream());
			tmpFileInputStream.close();
			
		}
		catch (Throwable t) {
			log.error("Error while downloading queue items", t);
		}
	}

}
