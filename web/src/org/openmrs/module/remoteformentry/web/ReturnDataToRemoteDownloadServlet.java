package org.openmrs.module.remoteformentry.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.remoteformentry.RemoteFormEntryConstants;
import org.openmrs.module.remoteformentry.RemoteFormEntryUtil;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.web.WebConstants;
import org.springframework.web.bind.ServletRequestUtils;

/**
 * Downloads the current data to return to the select remote site.
 * 
 * The file contains:
 * 1) A dump of the database (minus hl7* and obs*, orders*)
 * 2) An ack file listing all files received
 * 
 */
public class ReturnDataToRemoteDownloadServlet extends HttpServlet {

	/**
	 * Name of the ack directory in the zip file going back to the remote site
	 */
	public static final String ACK_DIR = "ackDir";

	public static final long serialVersionUID = 123332222423L;

	private static final Log log = LogFactory.getLog(ReturnDataToRemoteDownloadServlet.class);

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
		
		File remoteFormEntryDirectory = OpenmrsUtil.getDirectoryInApplicationDataDirectory("remoteformentry");
		File returnedDataDirectory = new File(remoteFormEntryDirectory, RemoteFormEntryConstants.RETURNED_DATA_DIRECTORY_NAME);
		if (!returnedDataDirectory.exists()) {
			returnedDataDirectory.mkdir();
		}
		
		Integer locationId = ServletRequestUtils.getRequiredIntParameter(request, "locationId");

		if (log.isDebugEnabled())
			log.debug("Downloading for the remote site: " + locationId);

		EncounterService encounterService = Context.getEncounterService();

		Location location = encounterService.getLocation(locationId);

		response.setHeader("Content-Type", "application/zip");
		String filename = "returnData-for-remote-site-" + RemoteFormEntryUtil.getDownloadSuffix(location) + ".zip";
		response.setHeader("Content-Disposition", "attachment; filename=" + filename);
		
		File outputFile = new File(returnedDataDirectory, filename);
		FileOutputStream zipFileOutputStream = new FileOutputStream(outputFile);
		
		ZipOutputStream zos			= new ZipOutputStream(zipFileOutputStream);
		ZipEntry zipEntry			= null;
		long compressedSize			= 0L;
		
		// add the main database dump
		File generatedData = RemoteFormEntryUtil.getGeneratedReturnDataFile();
		if (log.isDebugEnabled())
			log.debug("Zipping generated sql: " + generatedData.getAbsolutePath());

		FileInputStream inputStream = new FileInputStream(generatedData);

		// name this entry so we can retrieve it later
		zipEntry = new ZipEntry(RemoteFormEntryUtil.GENERATED_DATA_FILENAME);
		// Add ZIP entry to output stream.
		zos.putNextEntry(zipEntry);
		
		// Transfer bytes from the generated zip to the new return zip
		writeEntry(inputStream, zos);

		zos.closeEntry();
		compressedSize += zipEntry.getCompressedSize();

		// add each site/location specific sql file
		File locationSpecificFolder = RemoteFormEntryUtil.getGeneratedReturnDataFolderForLocation(location);
		if (log.isDebugEnabled())
			log.debug("Zipping the site specific data from folder: " + locationSpecificFolder.getAbsolutePath());
		
		for (File sqlFile : locationSpecificFolder.listFiles()) {
			FileInputStream sqlFileInputStream = new FileInputStream(sqlFile);
			// name this entry so we can retrieve it later
			zipEntry = new ZipEntry(sqlFile.getName());
			// Add ZIP entry to output stream.
			zos.putNextEntry(zipEntry);
			
			// Transfer bytes from the generated zip to the new return zip
			writeEntry(sqlFileInputStream, zos);

			zos.closeEntry();
			
			compressedSize += zipEntry.getCompressedSize();
		}
		
		// add the ack file to this zip download
		File ackDir = RemoteFormEntryUtil.getAckDir(location);
		for (File ack : ackDir.listFiles()) {
			zipEntry = new ZipEntry(ACK_DIR + "/" + ack.getName());
			zos.putNextEntry(zipEntry);
			inputStream = new FileInputStream(ack);
			writeEntry(inputStream, zos);
			inputStream.close();
			zos.closeEntry();
		}
		
		compressedSize += zipEntry.getCompressedSize();

		if (log.isDebugEnabled())
			log.debug("response total compressed size: " + compressedSize);

		// buggy
		//response.setContentLength((int)compressedSize);

		zos.close();
		zipFileOutputStream.close();
		
		InputStream fileInputStream = new FileInputStream(outputFile);
		
		OpenmrsUtil.copyFile(fileInputStream, response.getOutputStream());
		
		fileInputStream.close();

	}

	/**
	 * Write the given inputStream to the given zipoutputstream
	 * 
	 * @param inputStream
	 * @param zos
	 * @throws IOException 
	 */
	private void writeEntry(FileInputStream inputStream, ZipOutputStream zos) throws IOException {
		int buffer = 2048;
		int count;
		byte data[] = new byte[buffer];

		// write the entry data
		while ((count = inputStream.read(data, 0, buffer)) != -1) {
			zos.write(data, 0, count);
		}
	}

}
