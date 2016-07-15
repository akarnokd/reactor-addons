/*
 * Copyright (c) 2011-2016 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reactor.bus;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.bus.registry.Registration;
import reactor.bus.selector.Selector;
import reactor.core.Producer;
import reactor.core.Receiver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Operators;

/**
 * Emit signals whenever an Event arrives from the {@link reactor.bus.selector.Selector} topic from the {@link
 * reactor.bus.Bus}.
 * This stream will never emit a {@link org.reactivestreams.Subscriber#onComplete()}.
 * <p>
 * Create such stream with the provided factory, E.g.:
 * <pre>
 * {@code
 * eventBus.on($("topic")).subscribe(System.out::println)
 * }
 * </pre>
 *
 * @author Stephane Maldini
 */
final class BusFlux<T> extends Flux<T> {

	private final Selector  selector;
	private final Bus<?, T> observable;
	private final boolean   ordering;


	BusFlux(final @Nonnull Bus<?, T> observable,
	                    final @Nonnull Selector selector) {

		this.selector = selector;
		this.observable = observable;
		if (EventBus.class.isAssignableFrom(observable.getClass())) {
			this.ordering = 1 == ((EventBus) observable).getConcurrency();
		} else {
			this.ordering = true;
		}

	}

	@Override
	public void subscribe(Subscriber<? super T> s) {
		final Subscriber<? super T> subscriber;
		if (!ordering) {
			subscriber = Operators.serialize(s);
		} else {
			subscriber = s;
		}



		subscriber.onSubscribe(new BusToSubscription(subscriber));
	}

	@Override
	public String toString() {
		return "BusStream{" +
		  "selector=" + selector +
		  ", bus=" + observable +
		  '}';
	}

	private class BusToSubscriber implements Consumer<T>, Producer {

		private final Subscriber<? super T> subscriber;

		public BusToSubscriber(Subscriber<? super T> subscriber) {
			this.subscriber = subscriber;
		}

		@Override
		public Object downstream() {
			return subscriber;
		}

		@Override
		public void accept(T event) {
			subscriber.onNext(event);
		}
	}

	private class BusToSubscription implements Subscription, Receiver {

		final         Registration<?, ? extends BiConsumer<?, ? extends T>> registration;

		public BusToSubscription(Subscriber<? super T> subscriber) {
			registration = observable.on(selector, new BusToSubscriber(subscriber));
		}

		@Override
		public void request(long n) {
			//IGNORE
		}

		@Override
		public Object upstream() {
			return observable;
		}

		@Override
		public void cancel() {
			registration.cancel();
		}

	}
}
