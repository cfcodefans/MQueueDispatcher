import com.thenetcircle.services.cluster.JGroupsActor;


public class ClusterTest {
	public void testCluster() throws Exception {
		JGroupsActor.instance().start();
		Thread.currentThread().join();
	}
	
	public static void main(String[] args) {
		try {
			new ClusterTest().testCluster();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
