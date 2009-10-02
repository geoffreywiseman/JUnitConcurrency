package com.codiform.junitconcurrency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Annotation for marking tests to be run more than once and concurrently using
 * the {@link ConcurrentRunner} in JUnit.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConcurrentTest {
	
	/**
	 * The number of threads to use when running the test concurrently, defaults to 2.
	 */
	int threads() default 2;

	/**
	 * The number of times to execute the test.  This is /not/ the number of times each thread should
	 * execute the test, but rather the total number of executions.
	 */
	int executions() default 5;

	/**
	 * The timeout (in {@link #unit}s) for the concurrent test; if it takes longer than this, mark the test as failed.
	 * 
	 * @see #timeOutUnit()
	 */
	int timeout() default 0;
	
	/**
	 * The unit for the {@link #timeout}.
	 * 
	 * @see #timeout()
	 */
	TimeUnit timeOutUnit() default TimeUnit.MILLISECONDS;
	
	/**
	 * The maximum number of milliseconds to delay before beginning.  
	 * 
	 * This helps to stagger threads so that they're not always hitting the same part of the test at the same time.  
	 * 
	 * Defaults to 0, wherein thread staggering will only occur by virtue of thread scheduling.
	 */
	int randomDelay() default 0;
	
}
