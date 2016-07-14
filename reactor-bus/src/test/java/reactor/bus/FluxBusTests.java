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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.Test;
import reactor.bus.flux.FluxCoordinator;
import reactor.bus.selector.Selector;
import reactor.bus.selector.Selectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.TopicProcessor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static reactor.bus.selector.Selectors.$;

/**
 * @author Stephane Maldini
 */
public class FluxBusTests {

	@Test
	public void barrierStreamWaitsForAllDelegatesToBeInvoked() throws Exception {
		CountDownLatch latch1 = new CountDownLatch(1);
		CountDownLatch latch2 = new CountDownLatch(1);
		CountDownLatch latch3 = new CountDownLatch(1);

		FluxCoordinator streamCoordinator = new FluxCoordinator();

		EventBus bus = EventBus.create(TopicProcessor.create());
		bus.on($("hello"), streamCoordinator.wrap((Event<String> ev) -> {
			try {
				Thread.sleep(500);
			}
			catch (InterruptedException e) {
			}
			latch1.countDown();
		}));

		Flux.just("Hello World!")
		       .map(streamCoordinator.wrap((Function<String, String>) String::toUpperCase))
		       .subscribe(s -> {
			       latch2.countDown();
		       });

		streamCoordinator.subscribe(vals -> {
			try {
				Thread.sleep(500);
			}
			catch (InterruptedException e) {
			}
			latch3.countDown();
		});

		bus.notify("hello", Event.wrap("Hello World!"));
		bus.getProcessor()
		   .onComplete();

		assertThat("EventBus Consumer has been invoked", latch1.await(1, TimeUnit.SECONDS), is(true));
		assertThat("Stream map Function has been invoked", latch2.getCount(), is(0L));
		assertThat("BarrierStream has published downstream", latch3.await(1, TimeUnit.SECONDS), is(true));
	}

	@Test
	public void testRelaysEventsToReactor() throws InterruptedException {
		EventBus r = EventBus.config()
		                     .get();
		Selector key = Selectors.$();

		final CountDownLatch latch = new CountDownLatch(5);

		Flux<? extends Event<?>> tap = r
				.on(key)
				.doOnNext(d -> latch.countDown());

		Event[] data = new Event[1];
		tap.subscribe(d -> data[0] = d);

		r.notify(Flux.just("1", "2", "3", "4", "5")
		             .map(Integer::parseInt), key.getObject());

		//await(s, is(5));
		assertThat("latch was counted down", latch.getCount(), is(0l));
		assertThat("value is 5",
				data[0] .getData(),
				is(5));
	}
}
