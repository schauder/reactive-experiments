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

import static de.schauderhaft.blocking.Request.Type.*;

import java.time.Duration;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import de.schauderhaft.PrimeFactors;
import de.schauderhaft.blocking.Request.Type;
import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

/**
 * @author Jens Schauder
 */
public class BlockingSimulator {


	Scheduler dbScheduler = Schedulers.fromExecutorService(Executors.newFixedThreadPool(5));
	Scheduler mainScheduler = Schedulers.fromExecutorService(Executors.newFixedThreadPool(4));
	Random random = new Random(0);


	public static void main(String[] args) {
		new BlockingSimulator().
				runExperiment(new Configuration() {
					{
						durationInSeconds = 10;
						percentageDbCalls = 2;
					}
				});


	}

	private void runExperiment(Configuration configuration) {
		dbScheduler.start();
		try {
			Flux<Request> events = generateEvents(configuration);

			Flux<Result> results = processRequests(events);
			results.publishOn(mainScheduler);
			Flux<GroupedFlux<Result, Result>> groupedByTimeSlot = groupOnSwitch(
					results,
					r -> r.timeSlot()).filter(gf -> gf.key() != null);

			gatherStats(groupedByTimeSlot)
					.doOnNext(System.out::println)
					.blockLast(Duration.ofSeconds(11));
		} finally {
			dbScheduler.dispose();
		}
	}

	private Flux<Tuple3<Long, Type, Long>> gatherStats(Flux<GroupedFlux<Result, Result>> groupedByTimeSlot) {
		Flux<GroupedFlux<Tuple2<Long, Type>, Result>> groupedByTimeSlotAndType = groupedByTimeSlot
				.flatMap(gf -> gf.groupBy(r -> Tuples.of(gf.key().timeSlot(), r.getRequest().getType())));

		return groupedByTimeSlotAndType
				.flatMap(gf -> gf.count().map(c -> Tuples.of(gf.key().getT1(), gf.key().getT2(), c)));
	}

	private Flux<Result> processRequests(Flux<Request> events) {
		return (Flux<Result>) events
						.flatMap(
								r -> r.getType() == DB
										? simpleDbCall(r)
										: simpleComputation(r))
						.doOnNext(result -> System.out.println(Thread.currentThread().getName() + " " + result.getRequest().getType()))
						.filter(Result::isLast);
	}

	private Flux<Request> generateEvents(Configuration configuration) {
		return Flux.<Integer>generate(s -> s.next(random.nextInt()))
						.take(Duration.ofSeconds(configuration.durationInSeconds))
						.publishOn(mainScheduler)
						.map(id -> new Request(id, type(id, configuration.percentageDbCalls)));
	}

	private Type type(Integer id, int percentageDbCalls) {
		return Math.abs(id % 100) < percentageDbCalls ? DB : COMPUTATIONAL;
	}

	/**
	 * doesn't consume resources, but takes some time, emitting a single result
	 */
	private Publisher<Result> simpleDbCall(Request r) {
		Random random = new Random(r.getId());//make the behavior reproducable
		return Mono.just("").publishOn(dbScheduler).map(s -> {
			sleep();
			return Result.finalResult(r, String.format("db result<%s>", r.id));
		});
	}

	private void sleep() {
		try{
			int delay = (int) (random.nextGaussian() * 50.0 + 300);
			Thread.sleep(delay);
		} catch (Exception e) {}
	}

	private Flux<Result> simpleComputation(Request r) {

		return PrimeFactors
				.factors(r.id)
				.map(f -> new Result(r, String.format("non db result<%s>", f)))
				.concatWith(Mono.just(Result.finalResult(r)));
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
