/*
 * Copyright (c) 2011-2014 Pivotal Software, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package reactor.bus.selector;

import java.util.function.Predicate;

/**
 * Implementation of {@link Selector} that delegates the work of matching an object to the given {@link Predicate}.
 *
 * @author Jon Brisbin
 */
public class PredicateSelector<T> extends ObjectSelector<T, Predicate<? super T>> {

	public PredicateSelector(Predicate<? super T> object) {
		super(object);
	}

	/**
	 * Creates a {@link Selector} based on the given {@link Predicate}.
	 *
	 * @param predicate The {@link Predicate} to delegate to when matching objects.
	 * @return PredicateSelector
	 */
	public static <T> PredicateSelector<T> predicateSelector(Predicate<? super T> predicate) {
		return new PredicateSelector<>(predicate);
	}

	@Override
	public boolean matches(T key) {
		return getObject().test(key);
	}

}
