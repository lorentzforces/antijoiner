// Copyright 2022 Lorentz Aberg subject to license details in {project root}/LICENSE.txt
package antijoiner;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;


// TODO: rename this
// TODO: javadoc the hell out of this, including explanations for type parameters
public class Library {

	/**
	 * Convenience method for the special case where items on both sides of the antijoin are the
	 * same type, and that type's {@code equals(..)} method is suitable for checking for the same
	 * object. This may not always be the case. For example, if you are comparing objects
	 * representing database entities, you may only want to check for equality on the value
	 * representing the primary key.
	 */
	public static <T> AntijoinResult<T, T> antijoin(Collection<T> left, Collection<T> right) {
		return antijoin(
			left,
			right,
			Function.identity(),
			Function.identity(),
			NullValuePolicy.RETAIN_NONE
		);
	}

	/**
	 * Convenience method for the special case where items on both sides of the antijoin are the
	 * same type.
	 */
	public static <T, X> AntijoinResult<T, T> antijoin(
		Collection<T> left,
		Collection<T> right,
		Function<T, X> accessor
	) {
		return antijoin(left, right, accessor, accessor, NullValuePolicy.RETAIN_NONE);
	}

	public static <L, R, X> AntijoinResult<L, R> antijoin(
		Collection<L> left,
		Collection<R> right,
		Function<L, X> leftAccessor,
		Function<R, X> rightAccessor,
		NullValuePolicy nullPolicy
	) {
		Map<X, R> rightMapping = new HashMap<>();
		List<R> rightNulls = new ArrayList<>();
		for (R rightEntry : right) {
			if (right == null) {
				throw new NullPointerException("null object on the right side of the join");
			} else if (rightAccessor.apply(rightEntry) == null) {
				rightNulls.add(rightEntry);
			} else {
				rightMapping.put(rightAccessor.apply(rightEntry), rightEntry);
			}
		}

		List<JoinedPair<L, R>> joinedPairs = new ArrayList<>();
		List<L> leftNulls = new ArrayList<>();
		List<L> leftComplement = new ArrayList<>();

		for (L leftEntry : left) {
			if (leftEntry == null) {
				throw new NullPointerException("null object on the left side of the join");
			} else if (leftAccessor.apply(leftEntry) == null) {
				leftNulls.add(leftEntry);
			} else {
				R rightEntry = rightMapping.remove(leftAccessor.apply(leftEntry));

				if (rightEntry == null) {
					leftComplement.add(leftEntry);
				} else {
					joinedPairs.add(new JoinedPair<>(leftEntry, rightEntry));
				}
			}
		}

		AntijoinResult<L, R> result = new AntijoinResult<>();
		result.leftComplement = leftComplement;
		result.joinedPairs = joinedPairs;
		result.rightComplement =
			rightMapping.entrySet()
			.stream()
			.map(Entry::getValue)
			.collect(toList());

		if (
			nullPolicy == NullValuePolicy.RETAIN_LEFT
			|| nullPolicy == NullValuePolicy.RETAIN_FULL
		) {
			result.leftComplement.addAll(leftNulls);
		}

		if (
			nullPolicy == NullValuePolicy.RETAIN_RIGHT
			|| nullPolicy == NullValuePolicy.RETAIN_FULL
		) {
			result.rightComplement.addAll(rightNulls);
		}

		return result;
	}

	// TODO: probably move these both out into their own source files
	public static class AntijoinResult <L, R> {
		public List<L> leftComplement;
		public List<JoinedPair<L, R>> joinedPairs;
		public List<R> rightComplement;
	}

	public static class JoinedPair <L, R> {
		public L left;
		public R right;

		public JoinedPair(L left, R right) {
			this.left = left;
			this.right = right;
		}
	}

	public static enum NullValuePolicy {
		RETAIN_NONE,
		RETAIN_LEFT,
		RETAIN_RIGHT,
		RETAIN_FULL
	}

}
