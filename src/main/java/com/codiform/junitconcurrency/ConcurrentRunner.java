package com.codiform.junitconcurrency;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.junit.Ignore;
import org.junit.internal.runners.InitializationError;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

public class ConcurrentRunner extends JUnit4ClassRunner {

	public ConcurrentRunner(Class<?> klass) throws InitializationError {
		super(klass);
	}

	@Override
	protected void validate() throws InitializationError {
		ConcurrentMethodValidator methodValidator = new ConcurrentMethodValidator(
				getTestClass());
		methodValidator.validateMethodsForDefaultRunner();
		methodValidator.assertValid();
	}

	@Override
	protected List<Method> getTestMethods() {
		List<Method> testMethods = new ArrayList<Method>();
		
		testMethods.addAll(super.getTestMethods());
		
		List<Method> concurrentTestMethods = getTestClass()
				.getAnnotatedMethods(ConcurrentTest.class);
		for(Method item: concurrentTestMethods ) {
			if( !testMethods.contains(item) ) {
				testMethods.add(item);
			}
		}
		
		return testMethods;
	}

	@Override
	protected void invokeTestMethod(Method method, RunNotifier notifier) {
		ConcurrentTest annotation = method.getAnnotation(ConcurrentTest.class);

		if (annotation == null || annotation.executions() == 1) {
			// Just Run it the Normal Way
			super.invokeTestMethod(method, notifier);
		} else if (annotation.executions() == 0) {
			notifier.fireTestIgnored(methodDescription(method));
		} else {
			// Run the Method Concurrently
			concurrentlyInvokeTestMethod(method, notifier, annotation);
		}
	}

	private void concurrentlyInvokeTestMethod(Method method,
			RunNotifier notifier, ConcurrentTest annotation) {
		List<Callable<TestResult>> invocations = createInvocations(method,
				annotation.executions(), annotation.randomDelay());
		ExecutorService executor = createExecutor(annotation);
		Description description = methodDescription(method);

		if (method.getAnnotation(Ignore.class) != null) {
			notifier.fireTestIgnored(description);
		} else {
			concurrentlyInvokeTestMethod(notifier, annotation, invocations,
					executor, description);
		}
	}

	private void concurrentlyInvokeTestMethod(RunNotifier notifier,
			ConcurrentTest annotation, List<Callable<TestResult>> invocations,
			ExecutorService executor, Description description) {
		notifier.fireTestStarted(description);
		try {
			List<Future<TestResult>> results = executor.invokeAll(invocations);
			if (annotation.timeout() > 0) {
				if (executor.awaitTermination(annotation.timeout(), annotation
						.timeOutUnit()))
					notifyResult(notifier, description, results);
				else
					notifier.fireTestFailure(new Failure(description,
							new TimeoutException("Concurrent test invocations took longer than desired timeout (" + annotation.timeout() + " " + annotation.timeOutUnit() + ").")));
			} else {
				notifyResult(notifier, description, results);
			}
		} catch (InterruptedException exception) {
			notifier.fireTestFailure(new Failure(description, exception));
		} finally {
			notifier.fireTestFinished(description);
			executor.shutdown();
		}
	}

	private void notifyResult(RunNotifier notifier, Description description,
			List<Future<TestResult>> results) {

		TestResult failure = null;
		for (Future<TestResult> future : results) {
			try {
				TestResult item = future.get();
				if (item.is(TestResult.Type.ABORTED)) {
					notifier.testAborted(description, item.getCause());
				} else if (item.is(TestResult.Type.FAILED)) {
					failure = item;
				}
			} catch (Exception exception) {
				notifier.testAborted(description, exception);
			}
		}

		if (failure != null) {
			// Failure
			notifier.fireTestFailure(new Failure(description, failure
					.getCause()));
		}

	}

	private List<Callable<TestResult>> createInvocations(Method method,
			int count, int delay) {
		Random random = new Random();
		List<Callable<TestResult>> invocations = new ArrayList<Callable<TestResult>>();
		for (int index = 0; index < count; index++) {
			if (delay > 0) {
				invocations.add(new ConcurrentTestInvocation(getTestClass(),
						method, random.nextInt(delay)));
			} else {
				invocations.add(new ConcurrentTestInvocation(getTestClass(),
						method));
			}
		}
		return invocations;
	}

	private ExecutorService createExecutor(ConcurrentTest annotation) {
		if (annotation.threads() > 1)
			return Executors.newFixedThreadPool(annotation.threads());
		else
			return Executors.newSingleThreadExecutor();
	}

}
