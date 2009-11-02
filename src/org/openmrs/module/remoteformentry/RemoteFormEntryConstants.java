/**
 * Auto generated file comment
 */
package org.openmrs.module.remoteformentry;


/**
 * Constants for the remote form entry module
 */
public class RemoteFormEntryConstants {

	/**
	 * Directory that the pending queue items are stored in
	 */
	public static final String GP_PENDING_QUEUE_DIR = "remoteformentry.pending_queue_dir";
	
	/**
	 * Directory that the ack items are stored in
	 */
	public static final String GP_ACK_DIR = "remoteformentry.ack_dir";

	public static final String GP_ACK_DIR_DEFAULT = "remoteformentry/acks";

	/**
	 * Global property holding the list of initial encounter types
	 */
	public static final String GP_INITIAL_ENCOUNTER_TYPES = "remoteformentry.initialEncounterTypes";

	/**
	 * Default name for the directory that the pending queue items are stored in
	 */
	public static final String GP_PENDING_QUEUE_DIR_DEFAULT = "remoteformentry/pendingQueue";
	
	public static final String PRIV_IMPORT_REMOTE_FORM_ENTRY = "Remote Form Entry Import";
	
	public static final String PRIV_EXPORT_REMOTE_FORM_ENTRY = "Remote Form Entry Export";
	
	public static final String PRIV_RESOLVE_REMOTE_FORM_ENTRY = "Remote Form Entry Resolve Errors";
	
	public static final String PRIV_EDIT_PROPS_REMOTE_FORM_ENTRY = "Remote Form Entry Edit Properties";
	public static final String PRIV_RECEIVE_DATA_REMOTE_FORM_ENTRY = "Remote Form Entry Recieve Data";
	public static final String PRIV_RETURN_TO_REMOTE_REMOTE_FORM_ENTRY = "Remote Form Entry Return To Remote";
	
	/**
	 * List of remote sites. It is a comma separated list of space separated groups. 
	 * e.g.: "1,2,10,23" or "1 3 10, 4 5 7, 18, 21".  (In the latter, locations 3 and 
	 * 10 are sublocations of 1 and all encounters/obs are sent to that same location)   
	 */
	public static final String GP_REMOTE_LOCATIONS = "remoteformentry.remote_locations";
	
	/**
	 * This runtime property controls how the remoteformentry module works.
	 * Options are "central" or "remote". If neither is defined, this is assumed
	 * to be a dual role machine (for testing purposes only)
	 * @see #RP_SERVER_TYPE_DEFAULT
	 * @see #RP_SERVER_TYPES
	 */
	public static final String RP_SERVER_TYPE = "remoteformentry.server_type";
	
	/**
	 * Allowed answers for the runtime property 'RP_SERVER_TYPE'
	 * @see #RP_SERVER_TYPE
	 */
	public static enum RP_SERVER_TYPES {
		central, remote
	};

	/**
	 * Server type default. If not null, an unset server type runtime property
	 * will be assumed to be this type. 'null' makes the server dual duty when
	 * the property is not set
	 */
	public static final String RP_SERVER_TYPE_DEFAULT = null;

	/**
	 * global property allowing the user to specify where to generate the return data
	 */
	public static final String GP_GENERATED_DATA_DIR = "remoteformentry.generated_return_data_dir";

	public static final String GP_GENERATED_DATA_DIR_DEFAULT = "remoteformentry/generatedReturnData";
	
	/**
	 * This gp will contain a comma separated list of tables to ignore when creating the large sql file.
	 * <br/><br/>
	 * Tables like obs and encounter are ignored because they are generated in other files 
	 * <br/><br/>
	 * Tables hl7_in_archive, formentry_error, are ignored because they are large and the remote sites don't care about them
	 */
	public static final String GP_GENERATED_DATA_IGNORE_TABLES = "remoteformentry.generated_return_data_tables_to_ignore";
	
	public static final String GP_LOCATION_ID = "remoteformentry.location_id";
	
	/**
	 * A PatientIdentifier.patientIdentifierTypeId of the type that is used to mark identifiers with
	 * invalid check digits. If this is non-empty and an patient's identifier comes in that is
	 * invalid, the type of that identifier will be changed to this type. If this is empty and an
	 * invalid identifier comes in, the patient will not be able to be saved to the database and
	 * will end up in the error queue.
	 */
	public static final String GP_INVALID_IDENTIFIER_TYPE = "remoteformentry.invalid_identifier_type";
	
	public static final String ACK_FILENAME_SEPARATOR = ",";
	
	
	/**
	 * Root name of the formfield for remote form entry
	 */
	public static final String ALL_PATIENT_DATA = "all patient data";

	public static final String PERSON_NAME = "person_name";
	public static final String PERSON_NAME_GIVEN = "person_name.given_name";
	public static final String PERSON_NAME_MIDDLE = "person_name.middle_name";
	public static final String PERSON_NAME_FAMILY = "person_name.family_name";
	public static final String PERSON_NAME_VOIDED = "person_name.voided";
	public static final String PERSON_NAME_PREFERRED = "person_name.preferred";

	public static final String PATIENT_IDENTIFIER = "patient_identifier";
	public static final String PATIENT_IDENTIFIER_IDENTIFIER = "patient_identifier.identifier";
	public static final String PATIENT_IDENTIFIER_TYPE = "patient_identifier.identifier_type_id";
	public static final String PATIENT_IDENTIFIER_LOCATION = "patient_identifier.location_id";
	public static final String PATIENT_IDENTIFIER_VOIDED = "patient_identifier.voided";
	public static final String PATIENT_IDENTIFIER_PREFERRED = "patient_identifier.preferred";
	
	public static final String PERSON_ADDRESS = "person_address";
	public static final String PERSON_ADDRESS_ADDRESS1 = "person_address.address1";
	public static final String PERSON_ADDRESS_ADDRESS2 = "person_address.address2";
	public static final String PERSON_ADDRESS_CITY_VILLAGE = "person_address.city_village";
	public static final String PERSON_ADDRESS_NEIGHBORHOOD_CELL = "person_address.neighborhood_cell";
	public static final String PERSON_ADDRESS_COUNTY_DISTRICT = "person_address.county_district";
	public static final String PERSON_ADDRESS_TOWNSHIP_DIVISION = "person_address.township_division";
	public static final String PERSON_ADDRESS_REGION = "person_address.region";
	public static final String PERSON_ADDRESS_SUBREGION = "person_address.subregion";
	public static final String PERSON_ADDRESS_STATE_PROVINCE = "person_address.state_province";
	public static final String PERSON_ADDRESS_COUNTRY = "person_address.country";
	public static final String PERSON_ADDRESS_POSTAL_CODE = "person_address.postal_code";
	public static final String PERSON_ADDRESS_LATITUDE = "person_address.latitude";
	public static final String PERSON_ADDRESS_LONGITUDE = "person_address.longitude";
	public static final String PERSON_ADDRESS_VOIDED = "person_address.voided";
	public static final String PERSON_ADDRESS_PREFERRED = "person_address.preferred";
	
	public static final String PATIENT_TRIBE = "patient.tribe";

	public static final String PERSON_BIRTHDATE = "person.birthdate";
	public static final String PERSON_BIRTHDATE_ESTIMATED = "person.birthdate_estimated";
	public static final String PERSON_GENDER = "person.gender";
	public static final String PERSON_DEAD = "person.dead";
	public static final String PERSON_DEATH_DATE = "person.death_date";
	public static final String PERSON_DEATH_REASON = "person.death_reason";

	public static final String PERSON_ATTRIBUTE = "person_attribute";
	public static final String PERSON_ATTRIBUTE_TYPE = "person_attribute.person_attribute_type_id";
	public static final String PERSON_ATTRIBUTE_VALUE = "person_attribute.value";
	public static final String PERSON_ATTRIBUTE_VOIDED = "person_attribute.voided";

	// all first level property nodes can be found in this section:
    public static final String nodePrefix = "/form/"
            + ALL_PATIENT_DATA.replace(" ", "_") + "/";

	/**
	 * Directory name within the remoteformentry application data dir that 
	 * contains the list of files that have been downloaded by users 
	 */
	public static final String EXPORT_DIRECTORY_NAME = "exportedFilesFromRemote";

	/**
	 * Directory name within the remoteformentry application data dir that
	 * contains the list of files that have been uploaded by users
	 */
	public static final String IMPORT_DIRECTORY_NAME = "importedFilesIntoCentral";

	/**
	 * Directory name within the remoteformentry application data dir that
	 * contains the list of files that have been uploaded by users from central
	 */
	public static final String RECEIVE_FILES_DIRECTORY_NAME = "receivedFilesFromCentral";

	/**
	 * Directory name within the remoteformentry application data dir that
	 * contains the list of files that have been downloaded on central for returning to remote sites
	 */
	public static final String RETURNED_DATA_DIRECTORY_NAME = "returnedDataToRemote";
	
}
