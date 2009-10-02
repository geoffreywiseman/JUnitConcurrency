package com.codiform.junitconcurrency;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assume.AssumptionViolatedException;
import org.junit.Test.None;
import org.junit.internal.runners.TestClass;

public class ConcurrentTestInvocation implements Callable<TestResult> {

	private TestResult finalResult;
	private TestClass testClass;
	private Method testMethod;
	private Object testInstance;
	private int delay;

	public ConcurrentTestInvocation(TestClass testClass, Method method) {
		this(testClass, method, 0);
	}

	public ConcurrentTestInvocation(TestClass testClass, Method method,
			int delay) {
		this.testClass = testClass;
		this.testMethod = method;
		this.delay = delay;
	}

	public TestResult call() throws Exception {
		delay();
		try {
			testInstance = testClass.getConstructor().newInstance();
			runBeforesTestAndAfters();
		} catch (InvocationTargetException e) {
			setResult(TestResult.abort(e));
		} catch (Exception e) {
			setResult(TestResult.abort(e));
		}

		return finalResult;
	}

	private void delay() {
		if (delay > 0) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				setResult(TestResult.abort(e));
			}
		}
	}

	private void setResult(TestResult result) {
		if (finalResult == null) {
			finalResult = result;
		} else {
			if (result.takesPriorityOver(finalResult)) {
				System.err
						.printf(
								"New result %s takes priority over previous result: %s\n",
								result, finalResult);
				finalResult = result;
			} else {
				System.err
						.printf(
								"New result, %s, does not take priority over existing result, %s, and is discarded.\n",
								result, finalResult);
			}
		}
	}

	private void runBeforesTestAndAfters() {
		try {
			if (runBefores())
				runTest();
		} finally {
			runAfters();
		}

	}

	private void runAfters() {
		List<Method> afters = testClass.getAnnotatedMethods(After.class);
		for (Method after : afters)
			try {
				after.invoke(testInstance);
			} catch (InvocationTargetException e) {
				setResult(TestResult.fail(e.getTargetException()));
			} catch (Throwable e) {
				setResult(TestResult.fail(e)); // Untested, but seems impossible
			}
	}

	private void runTest() {
		Class<?> expectedException = getExpectedException();

		try {
			testMethod.invoke(testInstance);
			if (expectedException != null && expectedException != None.class )
				setResult(TestResult.fail(new AssertionError(
						"Expected exception: " + expectedException.getName())));
			else {
				setResult(TestResult.successful());
			}
		} catch (InvocationTargetException e) {
			Throwable actual = e.getTargetException();
			if (actual instanceof AssumptionViolatedException)
				return;
			else if (expectedException == null)
				setResult(TestResult.fail(actual));
			else if (!expectedException.isInstance(actual)) {
				String message = "Unexpected exception, expected<"
						+ expectedException.getName() + "> but was<"
						+ actual.getClass().getName() + ">";
				setResult(TestResult.fail(new Exception(message, actual)));
			} else {
				setResult(TestResult.successful());
			}
		} catch (Throwable e) {
			setResult(TestResult.fail(e));
		}
	}

	private Class<?> getExpectedException() {
		Test annotation = testMethod.getAnnotation(Test.class);
		if (annotation != null && annotation.expected() != null) {
			return annotation.expected();
		} else {
			return null;
		}
	}

	private boolean runBefores() {
		try {
			try {
				List<Method> befores = testClass
						.getAnnotatedMethods(Before.class);
				for (Method before : befores)
					before.invoke(testInstance);
				return true;
			} catch (InvocationTargetException e) {
				throw e.getTargetException();
			}
		} catch (AssumptionViolatedException e) {
			return false;
		} catch (Throwable cause) {
			setResult(TestResult.fail(cause));
			return false;
		}
	}

}
