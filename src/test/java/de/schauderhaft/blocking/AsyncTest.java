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
import java.util.function.Function;

import org.junit.Test;
import org.reactivestreams.Publisher;

import de.schauderhaft.PrimeFactors;
import lombok.Data;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @author Jens Schauder
 */
public class AsyncTest {

	private Function<Object, String> threadName = i -> i + " " + Thread.currentThread().getName();
	private String currentThread = Thread.currentThread().getName();
	@Test
	public void runOnAdifferentScheduler() {

		Flux.just(1, 2, 3, 4, 5).subscribeOn(Schedulers.newSingle("whatever"))
				.map(new ControledComputation<>(
						i -> i * 2,
						"whatever"
				))
				.map(new ControledComputation<>(
						i -> i * i,
						"whatever"
				))
				.doOnNext(System.out::println)
				.blockLast(Duration.ofSeconds(5))
		;
	}

	@Test
	public void publishOn() {

		Flux.just(1, 2, 3, 4, 5)
				.map(new ControledComputation<>(
						i -> i * 2,
						currentThread
				))
				.publishOn(Schedulers.newSingle("whatever"))
				.map(new ControledComputation<>(
						i -> i * i,
						"whatever"
				))
				.publishOn(Schedulers.newSingle("second"))
				.map(new ControledComputation<>(
						i -> i - 10,
						"second"
				))
				.doOnNext(System.out::println)
				.blockLast(Duration.ofSeconds(5))
		;
	}

	@Test
	public void withFlatMapAndSuscribeOn() {

		Flux.just(1, 2, 3, 4, 5)
				.flatMap(i -> PrimeFactors.factors(i)
						.subscribeOn(Schedulers.newSingle("other"))
						.map(new ControledComputation<>(
								j -> j + 3,
								"other"))

				).map(new ControledComputation<>(
				j -> j * 8,
				"other"
		)).blockLast(Duration.ofSeconds(5));
	}

	@Test
	public void flatMapWrappedInPublishOn() {

		Flux.just(1, 2, 3, 4, 5)
				.flatMap(i -> PrimeFactors.factors(i)
						.publishOn(Schedulers.newSingle("other"))
						.map(new ControledComputation<>(
								j -> j + 3,
								"other"))

				).publishOn(Schedulers.newSingle("yet other")).map(new ControledComputation<>(
				j -> j * 8,
				"yet other"
		)).blockLast(Duration.ofSeconds(5));
	}


	@Test
	public void mixedThreads(){
		Flux<Integer> ints = Flux.range(1, 100);

		ints.flatMap(i -> i % 2 == 0 ? fromThread("one") : fromThread("two"))
				.map(threadName)
				.doOnNext(System.out::println)
				.blockLast(Duration.ofSeconds(5));

	}

	private Mono<String> fromThread(String name) {
		return Mono.just(name).publishOn(Schedulers.newSingle(name));
	}

	@Test
	public void withFlatMap() {
		Flux<Integer> ints = Flux.just(1, 2, 3, 4, 5);

		ints
				.flatMap(i -> PrimeFactors.factors(i)
						.map(new ControledComputation<>(
								j -> j + 3,
								currentThread))
				).blockLast(Duration.ofSeconds(5));
	}

	@Test
	public void mergeWith() { // first flux wins
		Flux<Integer> odd = Flux.range(1, 100).publishOn(Schedulers.newSingle("customOdds")).map(i -> i * 2 + 1);

		Flux<Integer> even = Flux.range(1, 100).publishOn(Schedulers.newSingle("customEven")).map(i -> i * 2);

		odd.mergeWith(even)
				.map(i -> i + Thread.currentThread().getName())
				.doOnNext(System.out::println)
				.blockLast(Duration.ofSeconds(5));
	}

	@Test
	public void merge() { // first flux wins
		Flux<String> odd = Flux.range(1, 100).publishOn(Schedulers.newSingle("customOdds")).map(i -> i * 2 + 1).map(threadName);

		Flux<String> even = Flux.range(1, 100).publishOn(Schedulers.newSingle("customEven")).map(i -> i * 2).map(threadName);

		Flux.merge(odd, even)
				.map(threadName)
				.doOnNext(System.out::println)
				.blockLast(Duration.ofSeconds(5));
	}

	@Test
	public void mergeAndSubscribeOn() { // first flux wins
		Flux<Integer> odd = Flux.range(1, 100).publishOn(Schedulers.newSingle("customOdds")).map(i -> i * 2 + 1);

		Flux<Integer> even = Flux.range(1, 100).publishOn(Schedulers.newSingle("customEven")).map(i -> i * 2);

		Flux.merge(odd, even)
				.map(i -> i + Thread.currentThread().getName())
				.doOnNext(System.out::println)
				.blockLast(Duration.ofSeconds(5));
	}


	@Test
	public void thereAndBackAgain() {

		Flux.just(1, 2, 3, 4, 5)
				.map(new ControledComputation<>(
						i -> i * 2,
						"other"
				))
				.subscribeOn(Schedulers.newSingle("other"))
				.map(new ControledComputation<>(
						i -> i * i,
						"other" // still ???
				))
				.doOnNext(System.out::println)
				.blockLast(Duration.ofSeconds(5))
		;
	}


	@Data
	static class ControledComputation<T, R> implements Function<T, R> {

		final Function<T, R> delegate;
		final String expectedThreadNamePrefix;

		@Override
		public R apply(T t) {
			String threadName = Thread.currentThread().getName();
			if (!threadName.startsWith(expectedThreadNamePrefix))
				throw new RuntimeException(String.format("wrong thread. Wanted %s, but got %s ", expectedThreadNamePrefix, threadName));
			return delegate.apply(t);
		}
	}
}
