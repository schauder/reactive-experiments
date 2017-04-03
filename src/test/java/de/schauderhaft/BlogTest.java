/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schauderhaft;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.time.Duration;

import org.junit.Test;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * @author Jens Schauder
 */
public class BlogTest {

	static Flux<String> addThread(Flux<?> flux) {
		return flux.map(e -> e + " " + Thread.currentThread());
	}

	static <T> Flux<T> assertThread(Flux<T> flux, String name) {
		return flux.doOnNext(
				e -> assertThat(Thread.currentThread().getName(),
						startsWith(name))
		);
	}

	private final String threadName = Thread.currentThread().getName();

	@Test
	public void reactorIsSingleThreadedByDefault() {

		Flux<Integer> flux = Flux.range(0, 1000);

		assertThread(flux, threadName)
				.blockLast(Duration.ofSeconds(1));
	}

	@Test
	public void delayingElementsIntroducesThreads() {

		Flux<Integer> flux = Flux.range(0, 1000)
				.delayElements(Duration.ofMillis(1));

		assertThread(flux, "timer")
				.blockLast(Duration.ofSeconds(3));
	}

	@Test
	public void publishOn() {

		Flux<Integer> flux = Flux.range(0, 1000)
				.publishOn(Schedulers.newSingle("newThread"));

		assertThread(flux, "newThread")
				.blockLast(Duration.ofSeconds(1));
	}

	@Test
	public void subscribeOn() {

		Flux<Integer> flux = Flux.range(0, 1000)
				.subscribeOn(Schedulers.newSingle("newThread"));

		assertThread(flux, "newThread")
				.blockLast(Duration.ofSeconds(1));
	}


}
