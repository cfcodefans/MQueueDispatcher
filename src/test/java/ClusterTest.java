import org.junit.Test;

import com.thenetcircle.services.cluster.JGroupsActor;


public class ClusterTest {

	@Test
	public void testCluster() throws Exception {
		JGroupsActor.instance().start();
		Thread.currentThread().join();
	}
}
