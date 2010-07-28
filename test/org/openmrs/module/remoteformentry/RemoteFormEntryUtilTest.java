package org.openmrs.module.remoteformentry;


import java.io.File;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.Relationship;
import org.openmrs.User;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class RemoteFormEntryUtilTest extends BaseModuleContextSensitiveTest {

	// used in various tests
	public static final String SAMPLE_XML_PERSON_UUID = "2178037d-f86b-4f12-8d8b-be3ebc220022";

	/**
	 * @see {@link RemoteFormEntryUtil#getRelationships(Patient,Document,XPath,User)}
	 * 
	 */
	@Test
	@Verifies(value = "should not process relationship mappings with blank RelationshipTypeIds", 
			method = "getRelationships(Patient,Document,XPath,User)")
	public void getRelationships_shouldNotProcessRelationshipMappingsWithBlankRelationshipTypeIds()
			throws Exception {
		// build all the variables that are passed to getRelationships()
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = db.parse(new File("test/org/openmrs/module/remoteformentry/remotelyEnteredForm.xml"));
		XPath xp = XPathFactory.newInstance().newXPath();
		Patient createdPatient = new Patient(1);
		User enterer = new User(1);
		
		// need the list of nodes to get the pre-evaluation size
		NodeList nodeList = (NodeList) xp.evaluate(RemoteFormEntryConstants.nodePrefix
		        + RemoteFormEntryConstants.PERSON_RELATIONSHIP, doc, XPathConstants.NODESET);

		// call the utility to get the relationships
		List<Relationship> relationships = RemoteFormEntryUtil.getRelationships(createdPatient, doc, xp, enterer);
		
		// there should be only one relationship defined that has a blank relationship type id
		Assert.assertEquals("relationship with a blank type id is being processed", 
				nodeList.getLength() - 1, relationships.size());
	}
}