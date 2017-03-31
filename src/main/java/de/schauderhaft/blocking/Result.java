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

import java.time.LocalDateTime;
import java.time.ZoneId;

import lombok.Data;

/**
 * @author Jens Schauder
 */
@Data
public class Result {

	private final Request request;
	private final String value;
	private final boolean last;
	private final LocalDateTime end;


	static Result finalResult(Request request) {
		return new Result(request, "N.A.", true);
	}

	static Result finalResult(Request request, String value) {
		return new Result(request, value, true);
	}

	Result(Request request, String value, boolean last) {

		//System.out.println(value + " on " + Thread.currentThread().getName());

		this.request = request;
		this.value = value;
		this.last = last;
		end = LocalDateTime.now();
	}

	Result(Request request, String value) {
		this(request, value, false);
	}

	long timeSlot() {
		return end.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli() / 500;
	}
}
