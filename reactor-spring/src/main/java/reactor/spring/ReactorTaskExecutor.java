/*
 * Copyright (c) 2011-2013 GoPivotal, Inc. All Rights Reserved.
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

package reactor.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import reactor.core.Reactor;
import reactor.function.Consumer;
import reactor.event.Event;
import reactor.event.selector.Selector;
import reactor.event.selector.Selectors;
import reactor.tuple.Tuple2;

import java.util.concurrent.Executor;

/**
 * A {@link TaskExecutor} implementation that uses a {@link Reactor} to dispatch and execute tasks.
 *
 * @author Jon Brisbin
 */
public class ReactorTaskExecutor implements TaskExecutor, Executor {

	private final Tuple2<Selector, Object> exec = Selectors.$();
	private final Reactor reactor;

	/**
	 * Creates a new ReactorTaskExceutor that will use the given {@link reactor} to execute
	 * tasks.
	 *
	 * @param reactor The reactor to use
	 */
	@Autowired
	public ReactorTaskExecutor(Reactor reactor) {
		this.reactor = reactor;

		this.reactor.on(exec.getT1(), new Consumer<Event<Runnable>>() {
			@Override
			public void accept(Event<Runnable> ev) {
				ev.getData().run();
			}
		});
	}

	@Override
	public void execute(Runnable task) {
		reactor.notify(exec.getT2(), Event.wrap(task));
	}

}
