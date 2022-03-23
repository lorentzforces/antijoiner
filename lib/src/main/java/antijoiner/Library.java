// Copyright 2022 Lorentz Aberg subject to license details in {project root}/LICENSE.txt
package antijoiner;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

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
		return antijoin(left, right, Function.identity(), Function.identity());
	}

	public static <L, R, X> AntijoinResult<L, R> antijoin(
		Collection<L> left,
		Collection<R> right,
		Function<L, X> leftAccessor,
		Function<R, X> rightAccessor
	) {
		Set<L> leftComplement = new HashSet<>();
		Map<X, R> rightMapping =
			right.stream()
			.filter(Objects::nonNull)
			.collect(toMap(rightAccessor, Function.identity()));
		// TODO: check sizes to see if we got passed a null, maybe error if we got one?
		Set<JoinedPair<L, R>> joinedPairs = new HashSet<>();

		for (L leftEntry : left) {
			R rightEntry = rightMapping.remove(leftAccessor.apply(leftEntry));

			if (rightEntry == null) {
				leftComplement.add(leftEntry);
			} else {
				joinedPairs.add(new JoinedPair<>(leftEntry, rightEntry));
			}
		}

		AntijoinResult<L, R> result = new AntijoinResult<>();
		result.leftComplement = leftComplement;
		result.joinedPairs = joinedPairs;
		result.rightComplement =
			rightMapping.entrySet()
			.stream()
			.map(Entry::getValue)
			.collect(toSet());

		return result;
	}

	// TODO: probably move these both out into their own source files
	public static class AntijoinResult <L, R> {
		public Set<L> leftComplement;
		public Set<JoinedPair<L, R>> joinedPairs;
		public Set<R> rightComplement;
	}

	public static class JoinedPair <L, R> {
		public L left;
		public R right;

		public JoinedPair(L left, R right) {
			this.left = left;
			this.right = right;
		}
	}

}
