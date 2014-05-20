import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.ClassUtils;
import org.junit.Test;

import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.common.MiscUtils.LoopingArrayIterator;


public class MiscTests {

	@Test
	public void testLoopIterator() {
		final LoopingArrayIterator<Integer> lai = new LoopingArrayIterator<Integer>(0,1,2,3);
		for (int i = 0; i < 10; i++) {
			System.out.println(lai.loop());
		}
	}
	
	@Test
	public void testClassName() {
		System.out.println(String.class.getCanonicalName());
		System.out.println(String.class.getSimpleName());
		System.out.println(ClassUtils.getShortCanonicalName(String.class));
		System.out.println(ClassUtils.getShortClassName(String.class));
	}
	
	@Test
	public void testLoopIteratorWithThreads() {
		final LoopingArrayIterator<AtomicInteger> lai = new LoopingArrayIterator<AtomicInteger>(new AtomicInteger(0), new AtomicInteger(0), new AtomicInteger(0), new AtomicInteger(0));
		final ExecutorService es = Executors.newFixedThreadPool(MiscUtils.AVAILABLE_PROCESSORS);
		
		final List<Future> ftList = new ArrayList<Future>();
		
		for (int i = 0; i < MiscUtils.AVAILABLE_PROCESSORS; i++) {
			final Future ft = es.submit(new Runnable() {
				
				@Override
				public void run() {
					for (int i = 0; i < 10000; i++) {
						System.out.println(Thread.currentThread().getName() + "\t" + i + " : " + lai.loop().getAndIncrement());
					}
				}
			});
			ftList.add(ft);
		}

		for (final Future ft : ftList) {
			try {
				ft.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println(Arrays.toString(lai.getArray()));
	}
}
