package antijoiner;

import antijoiner.Library.AntijoinResult;
import antijoiner.Library.JoinedPair;
import antijoiner.Library.NullValuePolicy;
import antijoiner.testutils.NonBasicType;
import antijoiner.testutils.TestData;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.junit.Ignore;
import org.junit.Test;

import static java.util.stream.Collectors.toList;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;


/**
 * An extremely rudimentary performance test class. The goal here was a configuration:
 * <ul>
 * <li>runnable via an IDE or Gradle task</li>
 * <li>that would not be distributed with a production build</li>
 * <li>that could depend on other utility methods also used in the test configuration</li>
 * </ul>
 * I spent a while trying to figure out how to get Gradle to do something like the above -
 * unfortunately, documentation and examples are sparse, and apparently people don't typically try
 * to get profilable configurations done in Java. Once again I find the hardest part of programming
 * is fighting with build tools, even though I would have assumed that making arbitrary numbers of
 * source sets which may or may not be distributed, and which may or may not depend on each other,
 * would be a pretty common use case in a build tool.
 */
public class PerfTest {

	/**
	 * To run: comment out the @Ignore annotation and run this test using whatever mechanism you
	 * prefer to run tests with.
	 */
	@Ignore
	@Test
	public void runImplementations() {
		TestData<NonBasicType> testData = new TestData<>(100_000, 30_000, x -> new NonBasicType(x));

		ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

 		long naiveStartTime = threadBean.getCurrentThreadCpuTime();
 		NaiveImpl.antijoin(
 			testData.left,
 			testData.right,
 			NonBasicType::getValue,
 			NonBasicType::getValue,
 			NullValuePolicy.RETAIN_NONE
 		);
 		long naiveRunTimeNanos = threadBean.getCurrentThreadCpuTime() - naiveStartTime;

		long ourImplStartTime = threadBean.getCurrentThreadCpuTime();
		Library.antijoin(
			testData.left,
			testData.right,
			NonBasicType::getValue,
			NonBasicType::getValue,
			NullValuePolicy.RETAIN_NONE
		);
		long ourImplRunTimeNanos = threadBean.getCurrentThreadCpuTime() - ourImplStartTime;

		System.out.println(String.format("Naive impl: %,d nanoseconds", naiveRunTimeNanos));
		System.out.println(String.format("Our impl: %,d nanoseconds", ourImplRunTimeNanos));
	}


	/**
	 * This is the basic form of the types of things I want to replace.
	 */
	private static class NaiveImpl {

		public static <L, R, X> AntijoinResult<L, R> antijoin(
			Collection<L> left,
			Collection<R> right,
			Function<L, X> leftAccessor,
			Function<R, X> rightAccessor,
			NullValuePolicy nullPolicy
		) {
			AntijoinResult<L, R> results = new AntijoinResult<>();

			results.leftComplement = left.stream()
				.filter(leftItem ->
					(
						leftAccessor.apply(leftItem) == null
						&& (
							nullPolicy == NullValuePolicy.RETAIN_LEFT
							|| nullPolicy == NullValuePolicy.RETAIN_FULL
						)
					) ||
					right.stream().noneMatch(rightItem ->
						Objects.equals(leftAccessor.apply(leftItem), rightAccessor.apply(rightItem))
					))
				.collect(toList());

			results.rightComplement = right.stream()
				.filter(rightItem ->
					(
						rightAccessor.apply(rightItem) == null
						&& (
							nullPolicy == NullValuePolicy.RETAIN_RIGHT
							|| nullPolicy == NullValuePolicy.RETAIN_FULL
						)
					) ||
					left.stream().noneMatch(leftItem ->
						Objects.equals(leftAccessor.apply(leftItem), rightAccessor.apply(rightItem))
					))
				.collect(toList());

			results.joinedPairs = left.stream()
				.map(leftItem -> {
					Optional<R> rightMatch = right.stream()
						.filter(rightItem ->
							Objects.equals(leftAccessor.apply(leftItem), rightAccessor.apply(rightItem))
						)
						.findFirst();

					if (rightMatch.isPresent()) {
						return new JoinedPair<>(leftItem, rightMatch.get());
					} else {
						return null;
					}
				})
				.filter(Objects::nonNull)
				.collect(toList());

			return results;
		}
	}

}
