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
package de.schauderhaft.blocking;

import java.time.Duration;

import org.junit.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * @author Jens Schauder
 */
public class BackpressureWithSchedulers {

	private Scheduler 		parallel = Schedulers.newParallel("parallel", 4);


	@Test
	public void isThisBackpressureOrJustBlocking() {
		Flux.<Long>generate(s -> s.next(System.currentTimeMillis()))
				.map(this::sleep)
				.doOnNext(System.out::println)
				.take(Duration.ofSeconds(1))
				.blockLast(Duration.ofSeconds(1));
	}

	@Test
	public void subscribeOnDifferentThreadStillSingleThreaded() {
		int seconds = 10;
		Flux.<Long>generate(s -> s.next(System.currentTimeMillis()))
				.map(this::sleep)
				.subscribeOn(parallel)
				.doOnNext(m -> System.out.println(m / 100 + " " + Thread.currentThread().getName()))
				.take(Duration.ofSeconds(seconds))
				.blockLast(Duration.ofSeconds(seconds));
	}

	@Test
	public void flatMapWithSubscribeOn() {
		int seconds = 10;
		Flux.<Long>generate(s -> s.next(System.currentTimeMillis()))
				.flatMap(l -> Mono.just(l).subscribeOn(parallel).map(this::sleep))
				.subscribeOn(parallel)
				.doOnNext(m -> System.out.println(m / 100 + " " + Thread.currentThread().getName()))
				.take(Duration.ofSeconds(seconds))
				.blockLast(Duration.ofSeconds(seconds));
	}


	private <T> T sleep(T s) {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return s;
	}
}
