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
import java.util.function.Function;

import org.junit.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuples;

/**
 * @author Jens Schauder
 */
public class GroupByTest {

	@Test
	public void groupByDemo() {
		Flux<Long> longs = Flux
				.<Long>generate(s -> s.next(System.currentTimeMillis()))
				.delayElements(Duration.ofMillis(5))
				.take(Duration.ofMillis(500));
//		longs.doOnNext(System.out::println).blockLast();
		longs
				.groupBy(l -> l / 100)
				.flatMap(gf -> gf.count())
				.doOnNext(System.out::println)
				.blockLast();
	}


	@Test
	public void windowWhile() {

	}


	@Test
	public void groupOnSwitchDemo() {
		Flux<Long> longs = Flux
				.<Long>generate(s -> s.next(System.currentTimeMillis()))
				.delayElements(Duration.ofMillis(10))
				.take(Duration.ofSeconds(5));

		groupOnSwitch(longs, l -> l / 100)
				.flatMap(gf -> gf.count())
				.doOnNext(System.out::println)
				.blockLast();
	}

	@Test
	public void groupOnSwitch() {
		StepVerifier
				.create(
						groupOnSwitch(
								Flux.just("one", "two", "twenty", "tissue", "berta", "blot", "thousand"),
								s -> s.substring(0, 1))
								.flatMap(Flux::materialize)
								.map(s -> s.isOnComplete() ? "WINDOW CLOSED" : s.get())
				)
//				.expectNext("one", "WINDOW CLOSED")// the first element gets swollowed, and I don't know how to fix that.
				.expectNext("two", "twenty", "tissue", "WINDOW CLOSED")
				.expectNext("berta", "blot", "WINDOW CLOSED")
				.expectNext("thousand", "WINDOW CLOSED")
				.verifyComplete();
	}

	@Test
	public void rollingBuffer() {
		Flux<Integer> source = Flux.just(1, 2, 3, 4, 5, 6, 7, 8, 9);
		Flux<Integer> doubledFirst = source.compose(s -> source.take(1).concatWith(source));

		doubledFirst.buffer(2, 1).doOnNext(System.out::println).blockLast();
	}


	@Test
	public void groupOnSwitchKeys() {

		Flux<GroupedFlux<String, String>> fluxOfGroupedFluxes = groupOnSwitch(
				Flux.just("one", "two", "twenty", "tissue", "berta", "blot", "thousand"),
				s -> s.substring(0, 1));
		StepVerifier.create(
				fluxOfGroupedFluxes.map(gf -> gf.key())
		)
//				.expectNext("o")// the first element gets swollowed, and I don't know how to fix that.
				.expectNext("t", "b", "t")
				.verifyComplete();
	}

	private static <T, X> Flux<GroupedFlux<X, T>> groupOnSwitch(Flux<T> source, Function<T, X> keyFunction) {

		//Flux<T> doubledFirst = source.compose(s -> source.take(1).flatMap(v -> Flux.just(v, v)).concatWith(source));
		Flux<GroupedFlux<X, T>> value = source
				.buffer(2, 1)
				.filter(l -> l.size() == 2)
				.map(l -> Tuples.of(
						!keyFunction.apply(l.get(1)).equals(keyFunction.apply(l.get(0))),
						l.get(1)
				))
				.windowUntil(tup -> tup.getT1())
				.flatMap(gf -> gf.map(tup -> tup.getT2()).groupBy(t -> keyFunction.apply(t)));

		return value;
	}
}
