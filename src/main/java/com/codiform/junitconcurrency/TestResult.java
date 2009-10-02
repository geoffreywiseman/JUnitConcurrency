package com.codiform.junitconcurrency;

public class TestResult {

	public static enum Type {
		ABORTED(3), FAILED(2), SUCCESSFUL(1);
		
		int priority;
		
		private Type( int priority ) {
			this.priority = priority;
		}
	}

	private Type type;
	private Throwable cause;

	public static TestResult successful() {
		return new TestResult(Type.SUCCESSFUL, null);
	}

	public TestResult(Type type, Throwable cause) {
		this.type = type;
		this.cause = cause;
	}

	public boolean is(Type type) {
		return this.type == type;
	}

	public Throwable getCause() {
		return cause;
	}

	public static TestResult abort(Throwable cause) {
		return new TestResult(Type.ABORTED, cause);
	}

	public static TestResult fail(Throwable cause) {
		return new TestResult(Type.FAILED, cause);
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(type).append(" result");
		if (cause != null) {
			builder.append(" (").append(cause.getClass().getSimpleName())
					.append(": ").append(cause.getMessage());
		}
		return builder.toString();
	}

	public boolean takesPriorityOver(TestResult otherResult) {
		return type.priority > otherResult.type.priority;
	}
}
