package org.openmrs.module.remoteformentry.db.hibernate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.remoteformentry.RemoteFormEntryCleanupProcessor;
import org.openmrs.module.remoteformentry.RemoteFormEntryConstants;
import org.openmrs.module.remoteformentry.RemoteFormEntryException;
import org.openmrs.module.remoteformentry.RemoteFormEntryUtil;
import org.openmrs.module.remoteformentry.db.RemoteFormEntryDAO;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.util.StringUtils;

public class HibernateRemoteFormEntryDAO implements RemoteFormEntryDAO {

	protected final Log log = LogFactory.getLog(getClass());
	
	/**
	 * Hibernate session factory
	 */
	private SessionFactory sessionFactory;
	
	/**
	 * Default public constructor
	 */
	public HibernateRemoteFormEntryDAO() { }
	
	/**
	 * Set session factory
	 * 
	 * @param sessionFactory
	 */
	public void setSessionFactory(SessionFactory sessionFactory) { 
		this.sessionFactory = sessionFactory;
	}

	/**
     * @see org.openmrs.module.remoteformentry.db.RemoteFormEntryDAO#generateDataFile(java.io.File)
     */
    public void generateDataFile(File outFile) {
	    
    	String tablesToIgnoreGP = Context.getAdministrationService().getGlobalProperty(RemoteFormEntryConstants.GP_GENERATED_DATA_IGNORE_TABLES, "");
    	String[] ignoreTables = StringUtils.commaDelimitedListToStringArray(tablesToIgnoreGP);
    	
    	// TODO totally breaks if someone isn't using mysql as the backend
    	// TODO get custom location of mysql instead of just relying on path?
    	// TODO make this linux compatible 
    	
    	String[] props = getConnectionProperties();
    	String username = props[0];
    	String password = props[1];
    	String database = props[2];
    	
    	try {
	    	if (!outFile.exists())
	    		outFile.createNewFile();
    	}
    	catch (IOException io) {
    		throw new RemoteFormEntryException("Error while trying to create out file for return data: " + outFile.getAbsolutePath(), io);
    	}
    	
    	List<String> commands = new ArrayList<String>();
    	commands.add("mysqldump");
    	commands.add("-u" + username);
    	commands.add("-p" + password);
    	commands.add("-q");
    	commands.add("-e");
    	commands.add("--single-transaction");
    	commands.add("-r");
    	commands.add(outFile.getAbsolutePath());
    	commands.add(database);
    	
    	// mark the tables to ignore
    	for (String table : ignoreTables) {
    		table = table.trim();
    		if (StringUtils.hasLength(table)) {
	    		commands.add("--ignore-table");
	    		commands.add(database + "." + table);
    		}
    	}
    	
    	String output;
    	if (OpenmrsConstants.UNIX_BASED_OPERATING_SYSTEM)
    		output = execCmd(outFile.getParentFile(), commands.toArray(new String[] {}));
		else
			output = execCmd(null, commands.toArray(new String[] {}));
    	
    	if (output != null && output.length() > 0) {
    		log.debug("Exec called: " + Arrays.asList(commands));
    		log.debug("Output of exec: " + output);
    	}
    	
    }
    
    /**
     * @see org.openmrs.module.remoteformentry.db.RemoteFormEntryDAO#generateDataFile(java.io.File)
     */
    public void generateDataFileForLocation(File outFolder, Location location) {
	    
    	String[] includedTables  = new String[] {"obs", "encounter"};
    	String[] columnToMatchOn = new String[] {"person_id", "patient_id"};
    	
    	Connection conn = sessionFactory.getCurrentSession().connection();
		
    	Map<Location, List<Location>> mapToSubLocations = RemoteFormEntryUtil.getRemoteLocations();
    	
    	List<Location> allLocations = mapToSubLocations.get(location);
    	
    	try {
			PreparedStatement ps = conn.prepareStatement("create table if not exists patients_for_location (patient_id int(11) not null, primary key (patient_id))");
			PreparedStatement ps1 = conn.prepareStatement("delete from patients_for_location"); 
			String sql = "insert into patients_for_location (select distinct(patient_id) from encounter where location_id in (?";
			for (int x=1; x<allLocations.size(); x++)
				sql += ", ?";
			sql += "))";
			PreparedStatement ps2 = conn.prepareStatement(sql);
		
			ps.executeUpdate();
			ps1.executeUpdate();
			for (int x=0; x<allLocations.size(); x++)
				ps2.setInt(x+1, allLocations.get(x).getLocationId());
			
			ps2.executeUpdate();
			
			// make sure this gets into the database before the mysqldump is done next
			if (!conn.getAutoCommit()) {
				conn.commit();
			}
			else {
				conn.setAutoCommit(false);
				conn.commit();
				conn.setAutoCommit(true);
			}
    
    	}
    	catch (SQLException sql) {
    		log.error("Error while setting up the temporary table", sql);
    	}
		
    	// TODO totally breaks if someone isn't using mysql as the backend
    	// TODO get custom location of mysql instead of just relying on path?
    	// TODO make this linux compatible 
    	
    	String[] props = getConnectionProperties();
    	String username = props[0];
    	String password = props[1];
    	String database = props[2];
    	
    	if (!outFolder.exists())
    		outFolder.mkdirs();
    	
    	for (int x = 0; x< includedTables.length; x++) {
    		String table = includedTables[x];
    		String column = columnToMatchOn[x];
    		File outFile = new File(outFolder, table + ".sql");
    		String[] command = {"mysqldump",
					"-u" + username,
					"-p" + password,
					"-x",
					"-q",
					"-e",
					"-r",
					outFile.getAbsolutePath(),
					"--where=exists (select 1 from patients_for_location p where p.patient_id = " + column + ")",
					database,
					table
				   };    		
    		
    		String output;
        	if (OpenmrsConstants.UNIX_BASED_OPERATING_SYSTEM)
        		output = execCmd(outFolder.getParentFile(), command);
    		else
    			output = execCmd(null, command);
        	
	    	if (output != null && output.length() > 0) {
	    		log.debug("Exec called: " + Arrays.toString(command));
	    		log.debug("Output of exec: " + output);
	    	}
    	}
    	
    }
    
    /**
     * Auto generated method comment
     * 
     * @return
     */
    private String[] getConnectionProperties() {
    	Properties props = Context.getRuntimeProperties();
    	
    	// username, password, database
    	String[] connProps = {"test", "test", "openmrs"};
    	
    	
    	String username = (String)props.get("database.username");
    	if (username == null)
    		username = (String)props.get("connection.username");
    	if (username != null)
    		connProps[0] = username;
    	
    	String password = (String)props.get("database.password");
    	if (password == null)
    		password = (String)props.get("connection.password");
    	if (password != null)
    		connProps[1] = password;
    	
    	// get database name
    	String database = "openmrs";
    	String connectionUrl = (String)props.get("connection.url");
    	if (connectionUrl == null)
    		connectionUrl = (String)props.get("connection.url");
    	if (connectionUrl != null) {
    		int qmark = connectionUrl.lastIndexOf("?");
    		int slash = connectionUrl.lastIndexOf("/");
    		database = connectionUrl.substring(slash+1, qmark);
    		connProps[2] = database;
    	}
    	
    	return connProps;
    }

	/**
     * 
     * @param cmdWithArguments
     * @param wd
     * @return
     */
    private String execCmd(File wd, String[] cmdWithArguments) {
		log.debug("executing command: " + Arrays.toString(cmdWithArguments));
		
		StringBuffer out = new StringBuffer();
		try {
			// Needed to add support for working directory because of a linux
			// file system permission issue.
			
			Process p = (wd != null) ? Runtime.getRuntime().exec(cmdWithArguments, null, wd)
						: Runtime.getRuntime().exec(cmdWithArguments);
			
			out.append("Normal cmd output:\n");
			Reader reader = new InputStreamReader(p.getInputStream());
			BufferedReader input = new BufferedReader(reader);
			int readChar = 0;
			while ((readChar = input.read()) != -1) {
				out.append((char)readChar);
			}
			input.close();
			reader.close();
			
			out.append("ErrorStream cmd output:\n");
			reader = new InputStreamReader(p.getErrorStream());
			input = new BufferedReader(reader);
			readChar = 0;
			while ((readChar = input.read()) != -1) {
				out.append((char)readChar);
				}
			input.close();
			reader.close();
			
			Integer exitValue = p.waitFor();
			
			log.debug("Process exit value: " + exitValue);
			
		} catch (Exception e) {
			log.error("Error while executing command: '" + cmdWithArguments + "'", e);
		}
		
		log.debug("execCmd output: \n" + out.toString());
    	
		return out.toString();
	}

	/**
     * @see org.openmrs.module.remoteformentry.db.RemoteFormEntryDAO#execGeneratedFile(java.io.File)
     */
    public void execGeneratedFile(File generatedDataFile) {
	    
    	// this seems to be an mysql or c3p0 bug.  If you delete and recreate a table in a 
    	// separate process (like with the exec below) the form and scheduler tables won't 
    	// be recognized. However, attempting to delete (and failing) the form table seems
    	// to tell the db connector to look for the form table and return some results from
    	// it. (if this try/catch wasn't here, the form table returns no rows).
    	
    	try {
    		Connection conn = sessionFactory.getCurrentSession().connection();
    		
			PreparedStatement ps = conn.prepareStatement("drop table if exists form");
			PreparedStatement ps2 = conn.prepareStatement("drop table if exists scheduler_task_config");
		
			ps.executeUpdate();
			ps2.executeUpdate();
			
    	} catch (SQLException e) {
    		// this should throw an exception because form is foreign keyed from
    		// elsewhere.
    		log.debug("Expected SQL error generated", e);
	    }
    	
    	// TODO this depends on mysql being on the path
    	// TODO fix this so that queries are parsed out and run linebyline?
    	
    	String[] props = getConnectionProperties();
    	String username = props[0];
    	String password = props[1];
    	String database = props[2];
    	
    	String path = generatedDataFile.getAbsolutePath();
    	path = path.replace("\\", "/"); // replace windows file separator with forward slash
    	
    	String[] commands = {"mysql",
    					"-e",
    					"source " + path,
    					"-f",
    					"-u" + username,
    					"-p" + password,
    					"-D" + database
    					};
    	
    	String output;
    	if (OpenmrsConstants.UNIX_BASED_OPERATING_SYSTEM)
    		output = execCmd(generatedDataFile.getParentFile(), commands);
		else
			output = execCmd(null, commands);
    	
    	if (output != null && output.length() > 0) {
    		log.error("Exec call: " + Arrays.asList(commands));
    		log.error("Output of exec: " + output);
    	}
    }
    
    /**
     * @see org.openmrs.module.remoteformentry.db.RemoteFormEntryDAO#commitSession()
     */
    public void commitSession() {
    	Connection conn = sessionFactory.getCurrentSession().connection();
    	
    	// make sure this gets into the database before the mysqldump is done next
		try {
			if (!conn.getAutoCommit()) {
				conn.commit();
			}
			else {
				conn.setAutoCommit(false);
				conn.commit();
				conn.setAutoCommit(true);
			}
		} catch (SQLException e) {
			log.error("Unable to commit the session", e);
		}
    }
    
}
