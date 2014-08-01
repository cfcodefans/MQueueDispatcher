import com.thenetcircle.services.cluster.JGroupsActor;


public class JGroupTest {
	public void testCluster() throws Exception {
		JGroupsActor.instance().start();
		Thread.currentThread().join();
	}
	
	public static void main(String[] args) {
		try {
			new JGroupTest().testCluster();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
