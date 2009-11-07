package org.openmrs.module.remoteformentry;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Field;
import org.openmrs.FieldType;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.module.formentry.FormEntryConstants;

/**
 * Converts the form to have the fields/formFields necessary to be a remote form
 */
public class FormConverter {
	
	private static Log log = LogFactory.getLog(RemoteFormEntryUtil.class);
	
	// incremented value for the sort weights so that the fields show up in the order that they are in the hashmap
	private Integer formFieldSortWeight = 0;

	// cached FormService object
	private FormService formService;
	
	/**
	 * Adds the schema required by the remote form entry module to the given
	 * form
	 * 
	 * If the form already has the root formField, it and all child elements are
	 * removed and new formfields are added.
	 * 
	 * @param form Form to update
	 */
	public void addOrUpdateSchema(Form form) {

		Map<String, FormField> map = getFormFieldMap(form);

		// TODO move to constant?
		FormField rootFormField = map.get(RemoteFormEntryConstants.ALL_PATIENT_DATA.toUpperCase());

		if (rootFormField != null) {
			// there is an ALLPATIENTDATA element, remove it so
			// we can just add all of the new ones
			removeFormFieldAndChildren(form, rootFormField);
		}

		addAllFields(form);
	}

	/**
	 * Recursively removes the given FormField object and its children
	 * formFields on the given form
	 * 
	 * @param form Form which to remove to formField from
	 * @param parentFormField FormField to remove
	 */
	private void removeFormFieldAndChildren(Form form, FormField parentFormField) {

		for (FormField ff : form.getFormFields().toArray(new FormField[] {})) {
			if (ff.getParent() != null && ff.getParent().equals(parentFormField))
				removeFormFieldAndChildren(form, ff);
		}

		form.removeFormField(parentFormField);
	}

	/**
	 * Creates blank formFields and adds them to the form. If a field already
	 * exists in the db, that is used
	 * 
	 * @param form Form to add the fields to
	 */
	private void addAllFields(Form form) {
		log.debug("Add all fields to form: " + form);
		
		// TODO surely there is a better way to do this

		// example schema layout
		// ALL_PATIENT_DATA (assumed)
		//  PERSON NAME
		//   FIRST_NAME
		//   MIDDLE_NAME
		//   FAMILY_NAME
		//  PATIENT IDENTIFIER
		//   IDENTIFIER
		//   IDENTIFIER TYPE
		//   IDENTIFIER LOCATION
		//  PERSON ADDRESS
		//   ADDRESS1
		//   ...
		// ...
		FormField tmpFormField = null;

		FormField rootFormField = getNewFormField(RemoteFormEntryConstants.ALL_PATIENT_DATA, null, true, null, null, "");
		form.addFormField(rootFormField);

		// mapping from field name to default value
		Map<String, String> fieldNames = new LinkedHashMap<String, String>();
		
		
		// create the patient names
		FormField tmpRootFormField = getNewFormField(RemoteFormEntryConstants.PERSON_NAME,
			                           rootFormField, true, 0, -1,
			                           "$!{patient.getNames()}");
		form.addFormField(tmpRootFormField);
			
		fieldNames.put(RemoteFormEntryConstants.PERSON_NAME_GIVEN, "$!{listItem.getGivenName()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_NAME_MIDDLE, "$!{listItem.getMiddleName()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_NAME_FAMILY, "$!{listItem.getFamilyName()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_NAME_VOIDED, "$!{listItem.isVoided()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_NAME_PREFERRED, "$!{listItem.isPreferred()}");
		
		for (Map.Entry<String, String> entry : fieldNames.entrySet()) {
			tmpFormField = getNewFormField(entry.getKey(),
			                               tmpRootFormField, false, null, null,
			                               entry.getValue());
			form.addFormField(tmpFormField);
		}

		
		// create the patient identifiers
		tmpRootFormField = getNewFormField(RemoteFormEntryConstants.PATIENT_IDENTIFIER,
      			                           rootFormField, true, 0, -1,
	 			                           "$!{patient.getIdentifiers()}");
	    form.addFormField(tmpRootFormField);
		
		fieldNames.clear();
		fieldNames.put(RemoteFormEntryConstants.PATIENT_IDENTIFIER_IDENTIFIER,
		               "$!{listItem.getIdentifier()}");
		fieldNames.put(RemoteFormEntryConstants.PATIENT_IDENTIFIER_TYPE,
		               "$!{listItem.getIdentifierType().getPatientIdentifierTypeId()}");
		fieldNames.put(RemoteFormEntryConstants.PATIENT_IDENTIFIER_LOCATION,
		               "$!{listItem.getLocation().getLocationId()}");
		fieldNames.put(RemoteFormEntryConstants.PATIENT_IDENTIFIER_VOIDED, "$!{listItem.isVoided()}");
		fieldNames.put(RemoteFormEntryConstants.PATIENT_IDENTIFIER_PREFERRED, "$!{listItem.isPreferred()}");
		
		for (Map.Entry<String, String> entry : fieldNames.entrySet()) {
			tmpFormField = getNewFormField(entry.getKey(),
			                           tmpRootFormField, false, null, null,
			                           entry.getValue());
			form.addFormField(tmpFormField);
		}

		
		// create the patient default address
		tmpRootFormField = getNewFormField(RemoteFormEntryConstants.PERSON_ADDRESS,
      			                           rootFormField, true, 0, -1,
	 			                           "$!{patient.getAddresses()}");
	    form.addFormField(tmpRootFormField);
		
	    fieldNames.clear();
		fieldNames.put(RemoteFormEntryConstants.PERSON_ADDRESS_ADDRESS1,
		               "$!{listItem.getAddress1()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_ADDRESS_ADDRESS2,
		               "$!{listItem.getAddress2()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_ADDRESS_CITY_VILLAGE,
		               "$!{listItem.getCityVillage()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_ADDRESS_NEIGHBORHOOD_CELL,
		               "$!{listItem.getNeighborhoodCell()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_ADDRESS_COUNTY_DISTRICT,
		               "$!{listItem.getCountyDistrict()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_ADDRESS_TOWNSHIP_DIVISION,
		               "$!{listItem.getTownshipDivision()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_ADDRESS_REGION,
		               "$!{listItem.getRegion()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_ADDRESS_SUBREGION,
		               "$!{listItem.getSubregion()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_ADDRESS_STATE_PROVINCE,
		               "$!{listItem.getStateProvince()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_ADDRESS_COUNTRY,
		               "$!{listItem.getCountry()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_ADDRESS_POSTAL_CODE,
		               "$!{listItem.getPostalCode()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_ADDRESS_LATITUDE,
		               "$!{listItem.getLatitude()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_ADDRESS_LONGITUDE,
		               "$!{listItem.getLongitude()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_ADDRESS_VOIDED, "$!{listItem.isVoided()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_ADDRESS_PREFERRED, "$!{listItem.isPreferred()}");
		
		for (Map.Entry<String, String> entry : fieldNames.entrySet()) {
			tmpFormField = getNewFormField(entry.getKey(),
			                           tmpRootFormField, false, null, null,
			                           entry.getValue());
			form.addFormField(tmpFormField);
		}
		
		
		// create the PersonAttributes
		tmpRootFormField = getNewFormField(RemoteFormEntryConstants.PERSON_ATTRIBUTE,
			                           rootFormField, true, 0, -1,
			                           "$!{patient.getAttributes()}");
		form.addFormField(tmpRootFormField);
		
		fieldNames.clear();
		fieldNames.put(RemoteFormEntryConstants.PERSON_ATTRIBUTE_TYPE, "$!{listItem.getAttributeType().getPersonAttributeTypeId()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_ATTRIBUTE_VALUE, "$!{listItem.getValue()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_ATTRIBUTE_VOIDED, "$!{listItem.isVoided()}");
		for (Map.Entry<String, String> entry : fieldNames.entrySet()) {
			tmpFormField = getNewFormField(entry.getKey(),
			                               tmpRootFormField, false, null, null,
			                               entry.getValue());
			form.addFormField(tmpFormField);
		}
		
		// create the Relationships
		tmpRootFormField = getNewFormField(RemoteFormEntryConstants.PERSON_RELATIONSHIP,
			                           rootFormField, true, 0, -1,
			                           "$!{relationships}");
		form.addFormField(tmpRootFormField);
		
		fieldNames.clear();
		fieldNames.put(RemoteFormEntryConstants.PERSON_RELATIONSHIP_TYPE, "$!{listItem.getRelationshipType().getRelationshipTypeId()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_RELATIONSHIP_A_OR_B, "#if($!{listItem.getPersonA().getPersonId()} == ${patient.getPersonId()}) B #set($otherPerson=${listItem.getPersonB()}) #else A #set($otherPerson=$listItem.getPersonA()) #end");
		fieldNames.put(RemoteFormEntryConstants.PERSON_RELATIONSHIP_UUID, "$!{otherPerson.getUuid()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_RELATIONSHIP_IDENTIFIER, "$!{otherPerson.getPatientIdentifier()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_RELATIONSHIP_IDENTIFIER_TYPE, "$!{otherPerson.getPatientIdentifier().getIdentifierType().getPatientIdentifierTypeId()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_RELATIONSHIP_IDENTIFIER_LOC, "$!{otherPerson.getPatientIdentifier().getLocation().getLocationId()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_RELATIONSHIP_PERSON_BIRTHDATE, "$!{date.format($otherPerson.getBirthdate())}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_RELATIONSHIP_PERSON_GENDER, "$!{otherPerson.getGender()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_RELATIONSHIP_PERSON_GIVENNAME, "$!{otherPerson.getGivenName()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_RELATIONSHIP_PERSON_MIDDLENAME, "$!{otherPerson.getMiddleName()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_RELATIONSHIP_PERSON_FAMILYNAME, "$!{otherPerson.getFamilyName()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_RELATIONSHIP_VOIDED, "$!{listItem.isVoided()}");
		for (Map.Entry<String, String> entry : fieldNames.entrySet()) {
			tmpFormField = getNewFormField(entry.getKey(),
			                               tmpRootFormField, false, null, null,
			                               entry.getValue());
			form.addFormField(tmpFormField);
		}
		
		
		// other base person and patient demographics
		fieldNames.clear();
		fieldNames.put(RemoteFormEntryConstants.PERSON_BIRTHDATE,
		               "$!{date.format($patient.getBirthdate())}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_BIRTHDATE_ESTIMATED,
        				"$!{patient.getBirthDateEstimated()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_GENDER,
						"$!{patient.getGender()}");
		
		fieldNames.put(RemoteFormEntryConstants.PERSON_DEAD, "$!{patient.getDead()}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_DEATH_DATE, "$!{date.format($patient.getDeathDate())}");
		fieldNames.put(RemoteFormEntryConstants.PERSON_DEATH_REASON,
			"$!{patient.getDeathReason().getConceptId()}^$!{patient.getDeathReason().getName()}");
		
		for (Map.Entry<String, String> entry : fieldNames.entrySet()) {
			tmpFormField = getNewFormField(entry.getKey(),
			                           rootFormField, false, null, null,
			                           entry.getValue());
			form.addFormField(tmpFormField);
		}
	}

	/**
	 * Creates a new form field with its field having the given name. The db is
	 * searched for the given field name and if found, that field is used
	 * instead
	 * 
	 * If this is not a section, database element is assumed and the db/attr names are derived from the fieldName.  
	 * Assumed to be DB.ATTR
	 * 
	 * @param fieldName name of the Field to fetch or create
	 * @param parentFormField FormField this object will be the parent of the
	 *        newly created/returned formField
	 * @param isSection true/false whether or not this field should be a section type.
	 * @param minOccurs Integer minimum number of occurances for this formfield.  If 
	 * 		  not required, should be 0, otherwise, null
	 * @param maxOccurs Integer maximum occurances for this formfield. Commonly -1 or null    
	 * @param defaultValue String to be to the default value of the field if it
	 *        doesn't exist
	 * @return FormField with a null formFieldId
	 */
	private FormField getNewFormField(String fieldName, FormField parentFormField, 
			boolean isSection, Integer minOccurs, Integer maxOccurs, String defaultValue) {
		
		String upperFieldName = fieldName.toUpperCase();
		
		// try and find the field with the given fieldName
		Field field = null;
		List<Field> fields = getFormService().getFields(upperFieldName);
		for (Field tmpField : fields) {
			if (tmpField.getName().equals(upperFieldName)) {
				field = tmpField;
				break;
			}
		}

		// create a blank field with this name and value if none exists
		if (field == null) {
			field = new Field();
			field.setName(upperFieldName);
			field.setCreator(Context.getAuthenticatedUser());
			field.setDateCreated(new Date());
			field.setUuid(UUID.randomUUID().toString());
		}
		field.setDefaultValue(defaultValue);
		
		if (isSection)
			field.setFieldType(new FieldType(FormEntryConstants.FIELD_TYPE_SECTION));
		else {
			field.setFieldType(new FieldType(FormEntryConstants.FIELD_TYPE_DATABASE));
			// all non-section names are expected to be in format db.attr
			String[] tableAndAttr = fieldName.split("\\.");
			field.setTableName(tableAndAttr[0]);
			field.setAttributeName(tableAndAttr[1]);
		}
		
		// create the new formfield object
		FormField newFormField = new FormField();
		newFormField.setField(field);
		newFormField.setMinOccurs(minOccurs);
		newFormField.setMaxOccurs(maxOccurs);
		newFormField.setParent(parentFormField);
		newFormField.setSortWeight(Float.valueOf(formFieldSortWeight++));

		return newFormField;
	}

	/**
	 * Turns all of the formFields in the given form into a map from name to
	 * formfield object
	 * 
	 * @param form Form to get the formFields from
	 * @return Map<String, FormFiel> mapping from each formField's field.name
	 *         to the formField object
	 */
	protected Map<String, FormField> getFormFieldMap(Form form) {

		Map<String, FormField> map = new HashMap<String, FormField>();

		for (FormField formField : form.getFormFields()) {
			String name = formField.getField().getName();
			map.put(name, formField);
		}

		return map;
	}

	/**
	 * Get and cache the OpenMRS FormService
	 * 
	 * @return FormService from Context
	 */
	private FormService getFormService() {
		if (formService == null)
			formService = Context.getFormService();

		return formService;
	}
	
}
