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

import java.time.Duration;

import org.junit.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @author Jens Schauder
 */
public class WorkSheddingTest {

	private reactor.core.scheduler.Scheduler other = Schedulers.newSingle("other");

	@Test
	public void publishOnScheduler() {
		int periodInMillis = 100;
		long offset = System.currentTimeMillis();
		Flux<Long> numbers = Flux.interval(Duration.ofMillis(periodInMillis));

		numbers
				.take(30)
				.flatMap(l -> Flux.just(l)
						.publishOn(other)
						.map(i -> {
							try {
								Thread.sleep(300);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							return l;
						})
				)
				.doOnNext(l -> {
					long timestamp = System.currentTimeMillis() - offset;
					System.out.println(timestamp + " " + l + " " + (timestamp - l * periodInMillis));
				})
				.blockLast();
	}

	@Test
	public void workShedding() {
		int periodInMillis = 10;
		long offset = System.currentTimeMillis();
		Flux<Long> numbers = Flux.interval(Duration.ofMillis(periodInMillis));

		numbers
				.take(3000)
				.onBackpressureError()
				.flatMap(l -> Flux.just(l)
						.publishOn(other)
						.map(i -> {
							try {
								Thread.sleep(300);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							return l;
						})
				)
				.onErrorResumeWith(e -> {
					e.printStackTrace();
					return Mono.just(-1L);
				})
				.doOnNext(l -> {
					long timestamp = System.currentTimeMillis() - offset;
					System.out.printf("%d\t%d\t%d%n", timestamp, l, timestamp - l * periodInMillis);
				})
				.blockLast();
	}
}
