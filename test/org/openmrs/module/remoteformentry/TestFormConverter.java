package org.openmrs.module.remoteformentry;

import org.junit.Test;
import org.openmrs.Form;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class tests the different methods in the FormConverter
 */
public class TestFormConverter extends BaseModuleContextSensitiveTest {
	
	/**
	 * Tests relationships in the form schema
	 * 
	 * @throws Exception
	 */
	@Test
	@Transactional(readOnly = true)
	public void testFormConverterWithRelationships() throws Exception {
		
		Form form = Context.getFormService().getForm(1);
		
		FormConverter converter = new FormConverter();
		
		converter.addOrUpdateSchema(form);
	}
	
}
