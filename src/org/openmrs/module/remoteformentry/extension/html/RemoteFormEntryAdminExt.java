package org.openmrs.module.remoteformentry.extension.html;

import java.util.Map;
import java.util.TreeMap;

import org.openmrs.module.Extension;
import org.openmrs.module.remoteformentry.RemoteFormEntryConstants;
import org.openmrs.module.remoteformentry.RemoteFormEntryUtil;
import org.openmrs.module.web.extension.AdministrationSectionExt;
import org.openmrs.util.InsertedOrderComparator;

/**
 *
 */
public class RemoteFormEntryAdminExt extends AdministrationSectionExt {
	
	private static String requiredPrivileges = null;
	
	/**
	 * @see org.openmrs.module.web.extension.AdministrationSectionExt#getMediaType()
	 */
	public Extension.MEDIA_TYPE getMediaType() {
		return Extension.MEDIA_TYPE.html;
	}
	
	/**
	 * @see org.openmrs.module.web.extension.AdministrationSectionExt#getTitle()
	 */
	public String getTitle() {
		return "remoteformentry.title";
	}
	
	/**
	 * @see org.openmrs.module.web.extension.AdministrationSectionExt#getRequiredPrivilege()
	 */
	public String getRequiredPrivilege() {
		if (requiredPrivileges == null) {
			StringBuilder builder = new StringBuilder();
			builder.append(RemoteFormEntryConstants.PRIV_IMPORT_REMOTE_FORM_ENTRY);
			builder.append(",");
			builder.append(RemoteFormEntryConstants.PRIV_EXPORT_REMOTE_FORM_ENTRY);
			builder.append(",");
			builder.append(RemoteFormEntryConstants.PRIV_RESOLVE_REMOTE_FORM_ENTRY);
			builder.append(",");
			
			requiredPrivileges = builder.toString();
		}
		
		return requiredPrivileges;
	}
	
	/**
	 * @see org.openmrs.module.web.extension.AdministrationSectionExt#getLinks()
	 */
	public Map<String, String> getLinks() {
		
		Map<String, String> map = new TreeMap<String, String>(new InsertedOrderComparator());
		
		map.put("module/remoteformentry/properties.form", "remoteformentry.properties.manage");
		
		if (RemoteFormEntryUtil.isCentralServer()) {
			map.put("module/remoteformentry/setupForms.form", "remoteformentry.setupForms.manage");
			map.put("module/remoteformentry/import.form", "remoteformentry.import.manage");
			map.put("module/remoteformentry/resolveErrors.form", "remoteformentry.resolveErrors.manage");
			map.put("module/remoteformentry/returnDataToRemote.form", "remoteformentry.returnToRemote.manage");
			map.put("module/remoteformentry/generateReturnData.form", "remoteformentry.generateReturnData.manage");
		}
		
		if (RemoteFormEntryUtil.isRemoteServer()) {
			map.put("module/remoteformentry/export.form", "remoteformentry.export.manage");
			map.put("module/remoteformentry/receiveDataFromCentral.form", "remoteformentry.receiveDataFromCentral.manage");
		}
		
		return map;
	}
	
}
