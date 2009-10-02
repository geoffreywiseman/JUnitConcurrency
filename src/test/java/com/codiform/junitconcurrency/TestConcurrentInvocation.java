package com.codiform.junitconcurrency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

@RunWith(ConcurrentRunner.class)
public class TestConcurrentInvocation {

	private static final int EXPECTED_EXECUTIONS = 50;
	private static final int EXPECTED_THREADS = 10;
	
	private static AtomicInteger counter;
	private static Set<String> threadNames;
	private static List<Long> startTimes;

	@BeforeClass
	public static void setUp() {
		counter = new AtomicInteger();
		threadNames = Collections.synchronizedSet(new HashSet<String>());
		startTimes = Collections.synchronizedList(new ArrayList<Long>());
	}

	@ConcurrentTest(executions = EXPECTED_EXECUTIONS, threads = EXPECTED_THREADS, randomDelay=500)
	public void execute() {
		counter.incrementAndGet();
		if( threadNames.add( Thread.currentThread().getName() ) ) {
			// first pass, check staggering
			startTimes.add(System.currentTimeMillis());
		}
	}

	@AfterClass
	public static void testRandomDelay() {
		long min = Collections.min(startTimes);
		long max = Collections.max(startTimes);
		long delta = max - min;
		Assert.assertTrue( "Test start delays are not within expected range.  Expected 25-475, was: " + delta, 25 < delta && delta < 475 );
	}
	
	@AfterClass
	public static void testExecutions() {
		Assert.assertEquals("Test did not complete required number of executions.", EXPECTED_EXECUTIONS, counter
				.get());
	}

	@AfterClass
	public static void testThreads() {
		Assert.assertEquals("Test did not use the required number of threads.", EXPECTED_THREADS, threadNames.size());
	}
}
