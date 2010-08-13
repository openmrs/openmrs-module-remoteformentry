package org.openmrs.module.remoteformentry.web;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.remotedatatransfer.RemoteDataTransferServer;
import org.openmrs.module.remotedatatransfer.RemoteDataTransferService;
import org.openmrs.module.remoteformentry.RemoteFormEntryService;
import org.openmrs.module.remoteformentry.RemoteFormEntryUtil;
import org.openmrs.web.WebConstants;

/**
 * This class forces the generation of the "return data" sql file so that you 
 * don't have to wait until the middle of the night to run the auto generating 
 * openmrs scheduled task to do this same thing
 * 
 * @see org.openmrs.module.remoteformentry.GenerateReturnDataFileTask
 * @see org.openmrs.module.remoteformentry.RemoteFormEntryService#generateDataFile()
 */
public class GenerateReturnDataServlet extends HttpServlet {

	public static final long serialVersionUID = 123423231212L;

	private static final Log log = LogFactory.getLog(GenerateReturnDataServlet.class);

	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		if (Context.isAuthenticated() == false) {
			request.getSession().setAttribute(WebConstants.OPENMRS_ERROR_ATTR,
					"auth.session.expired");
			response.sendRedirect(request.getContextPath() + "/logout");
			return;
		}
		
		log.debug("Starting the generation process for the 'return data'");
		
		Date startTime = new Date();
		
		RemoteFormEntryService remoteService = (RemoteFormEntryService)Context.getService(RemoteFormEntryService.class);
		remoteService.generateDataFile();
		
		RemoteDataTransferService rdtService = Context.getService(RemoteDataTransferService.class);
		
		Collection<Location> locations = RemoteFormEntryUtil.getRemoteLocations().keySet();
		for(Location loc:locations){
			for(Location locInGroup:RemoteFormEntryUtil.getRemoteLocations().get(loc)){
				for(RemoteDataTransferServer server:rdtService.getServersByLocation(locInGroup)){
					FileInputStream fis = new FileInputStream(RemoteFormEntryUtil.getGeneratedReturnZipForLocation(locInGroup));
					rdtService.sendData(fis, server, "remoteformentry", "RFE_ReturnData.zip");
				}
			}
		}

		Date endTime = new Date();
		
		log.debug("Done calling generation process for the 'return data'");
		
		response.setHeader("Content-Type", "text");
		response.getOutputStream().println(" Done generating 'return data' sql file. ");
		response.getOutputStream().println(" Start time: " + startTime);
		response.getOutputStream().println(" End time: " + endTime);
		
	}

}
