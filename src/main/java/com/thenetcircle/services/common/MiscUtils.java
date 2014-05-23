package com.thenetcircle.services.common;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections4.iterators.ObjectArrayIterator;
import org.apache.commons.lang3.math.NumberUtils;

public class MiscUtils {
	public static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

	public static String invocationInfo() {
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		int i = 2;
		return String.format("%s\t%s.%s", ste[i].getFileName(), ste[i].getClassName(), ste[i].getMethodName());
	}

	public static String invocationInfo(final int i) {
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		return String.format("%s\t%s.%s", ste[i].getFileName(), ste[i].getClassName(), ste[i].getMethodName());
	}

	public static String byteCountToDisplaySize(long size) {
		String displaySize;
		if (size / 1073741824L > 0L) {
			displaySize = String.valueOf(size / 1073741824L) + " GB";
		} else {
			if (size / 1048576L > 0L) {
				displaySize = String.valueOf(size / 1048576L) + " MB";
			} else {
				if (size / 1024L > 0L)
					displaySize = String.valueOf(size / 1024L) + " KB";
				else
					displaySize = String.valueOf(size) + " bytes";
			}
		}
		return displaySize;
	}

	public static long getProcessId() {
		// Note: may fail in some JVM implementations
		// therefore fallback has to be provided

		// something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
		final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
		final int index = jvmName.indexOf('@');
		String pidStr = jvmName.substring(0, index);

		if (index < 1 || !NumberUtils.isNumber(pidStr)) {
			// part before '@' empty (index = 0) / '@' not found (index = -1)
			return 0;
		}

		return Long.parseLong(pidStr);
	}

	public static class LoopingArrayIterator<E> extends ObjectArrayIterator<E> {
		public LoopingArrayIterator(final E... array) {
			super(array, 0, array.length);
		}

		public LoopingArrayIterator(final E array[], final int start) {
			super(array, start, array.length);
		}

		public E loop() {		
			final E[] array = this.getArray();
			return array[loopIdx.getAndIncrement() % array.length];
		}
		
		private AtomicInteger loopIdx = new AtomicInteger();
	}
}
