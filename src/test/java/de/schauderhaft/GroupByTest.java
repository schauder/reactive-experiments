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
import java.util.Objects;
import java.util.function.Function;

import org.junit.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.test.StepVerifier;

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
				.expectNext("one", "WINDOW CLOSED")
				.expectNext("two", "twenty", "tissue", "WINDOW CLOSED")
				.expectNext("berta", "blot", "WINDOW CLOSED")
				.expectNext("thousand", "WINDOW CLOSED")
				.verifyComplete();
	}

	@Test
	public void groupOnSwitchKeys() {

		Flux<GroupedFlux<String, String>> fluxOfGroupedFluxes = groupOnSwitch(
				Flux.just("one", "two", "twenty", "tissue", "berta", "blot", "thousand"),
				s -> s.substring(0, 1));
		StepVerifier.create(
				fluxOfGroupedFluxes.map(gf -> gf.key())
		)
				.expectNext("o", "t", "b", "t")
				.verifyComplete();
	}

	private static <T, X> Flux<GroupedFlux<X, T>> groupOnSwitch(Flux<T> flux, Function<T, X> keyFunction) {
		ChangeTrigger changeTrigger = new ChangeTrigger();
		Flux<GroupedFlux<T, T>> fluxOfGroupedFluxes = flux.windowUntil(l -> changeTrigger.test(keyFunction.apply(l)), true);
		return fluxOfGroupedFluxes.flatMap(gf -> gf.groupBy(t -> keyFunction.apply(gf.key())));
	}

	private static class ChangeTrigger<T> {

		T last = null;

		boolean test(T value) {
			boolean startNew = !Objects.equals(last, value);
			last = value;
			System.out.println(String.format("%s, %s", value, startNew));
			return startNew;
		}
	}
}
