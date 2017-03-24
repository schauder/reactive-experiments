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
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import de.schauderhaft.PrimeFactors;
import de.schauderhaft.blocking.Request.Type;
import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * @author Jens Schauder
 */
public class BlockingSimulator {


	public static void main(String[] args) {
		Random random = new Random(0);
		Flux<Request> events = Flux.<Integer>generate(s -> s.next(random.nextInt()))
				.take(Duration.ofSeconds(10))
				.map(Request::new);

		Flux<Result> results = events
				.flatMap(
						r -> r.type() == Type.DB
								? simpleDbCall(r)
								: simpleComputation(r))
				.filter(Result::isLast);

		Flux<GroupedFlux<Result, Result>> groupedByTimeSlot = groupOnSwitch(
				results,
				r -> r.timeSlot()).filter(gf -> gf.key() != null);

//		Flux<GroupedFlux<Tuple2<Long, Type>, Result>> groupedByTimeSlotAndType = groupedByTimeSlot
//				.flatMap(gf -> gf.groupBy(r -> Tuples.of(gf.key().timeSlot(), r.getRequest().type())));
//
//		groupedByTimeSlotAndType
//				.flatMap(gf -> gf.count().map(c -> gf.key().toString() + c))
//				.doOnNext(System.out::println)
//				.blockLast(Duration.ofSeconds(11));
	}

	private static Publisher<Result> simpleDbCall(Request r) {
		// doesn't consume resources, but takes some time, emitting a single result
		Random random = new Random();
		return Mono.just(Result.finalResult(r, String.format("dbResult<%s>", r.id))).delayElementMillis((int) (random.nextGaussian() * 50.0 + 300));
	}

	private static Flux<Result> simpleComputation(Request r) {
		return PrimeFactors.factors(r.id).map(f -> new Result(r, String.format("result<%s>", f))).concatWith(Mono.just(Result.finalResult(r)));
	}


	private static <T> Flux<GroupedFlux<T, T>> groupOnSwitch(Flux<T> values, Function<T, ?> keyFunction) {
		ChangeTrigger changeTrigger = new ChangeTrigger(0);
		return values.windowUntil(l -> changeTrigger.test(keyFunction.apply(l)));
	}

	private static class ChangeTrigger<T> {

		T last = null;

		ChangeTrigger(T initialValue) {
			last = initialValue;
		}

		boolean test(T value) {
			boolean result = !Objects.equals(last, value);
			last = value;
			return result;
		}
	}
}
