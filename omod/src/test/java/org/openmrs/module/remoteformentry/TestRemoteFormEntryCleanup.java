package org.openmrs.module.remoteformentry;

import org.junit.Test;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.SkipBaseSetup;

/**
 * This class tests the different methods in the RemoteFormEntryService
 */
public class TestRemoteFormEntryCleanup extends BaseModuleContextSensitiveTest {

	/**
	 * @see org.openmrs.test.testutil.BaseContextSensitiveTest#useInMemoryDatabase()
	 */
	@Override
	public Boolean useInMemoryDatabase() {
		return false;
	}

	/**
	 * Tests create/delete/get of RemoteFormEntryPendingQueues
	 * 
	 * @throws Exception
	 */
	@SkipBaseSetup
	@Test
	public void testRemoteFormEntryPendingQueue() throws Exception {
		authenticate();
		
		RemoteFormEntryCleanupProcessor processor = new RemoteFormEntryCleanupProcessor();
		
		processor.processRemoteFormEntryCleanupQueue();
    }

}
