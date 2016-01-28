/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.client;

import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMessage;
import org.springframework.http.HttpStatus;

/**
 * Represents an HTTP request or response entity, consisting of headers and body.
 * This entity is itself publishing the body as a reactive stream, as it extends
 * a {@code Flux<T>}.
 *
 * <p>This is typically used in combination with a {@link HttpClient}.
 *
 * @author Brian Clozel
 */
public class FluxResponseEntity<T> extends Flux<T> implements HttpMessage {

	private final HttpStatus httpStatus;

	private final HttpHeaders httpHeaders;

	private final Flux<T> body;

	public FluxResponseEntity(HttpStatus httpStatus, HttpHeaders httpHeaders, Flux<T> body) {
		this.httpStatus = httpStatus;
		this.httpHeaders = httpHeaders;
		this.body = body;
	}

	/**
	 * @return the HTTP status as an {@link HttpStatus} enum value
	 */
	public HttpStatus getStatusCode() {
		return this.httpStatus;
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.httpHeaders;
	}

	@Override
	public void subscribe(Subscriber<? super T> s) {
		this.body.subscribe(s);
	}
}
