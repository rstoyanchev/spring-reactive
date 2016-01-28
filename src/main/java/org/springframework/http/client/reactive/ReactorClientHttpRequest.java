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

package org.springframework.http.client.reactive;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.io.buffer.Buffer;
import reactor.io.net.http.HttpClient;
import reactor.io.net.http.model.Method;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;

/**
 * {@link ClientHttpRequest} implementation for the Reactor Net HTTP client
 *
 * @author Brian Clozel
 * @see HttpClient
 */
public class ReactorClientHttpRequest implements ClientHttpRequest {

	private final DataBufferAllocator allocator;

	private final HttpMethod httpMethod;

	private final URI uri;

	private final HttpHeaders headers;

	private final HttpClient<Buffer, Buffer> httpClient;

	private Flux<Buffer> body;

	private SuccessCallback<Void> successCallback;

	private FailureCallback failureCallback;

	private final AtomicBoolean requestSent = new AtomicBoolean(false);

	public ReactorClientHttpRequest(HttpMethod httpMethod, URI uri, HttpClient httpClient, HttpHeaders headers,
			DataBufferAllocator allocator) {
		this.allocator = allocator;
		this.httpMethod = httpMethod;
		this.uri = uri;
		this.httpClient = httpClient;
		if (headers != null) {
			this.headers = headers;
		}
		else {
			this.headers = new HttpHeaders();
		}
		this.successCallback = v -> {
		};
		this.failureCallback = v -> {
		};
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.requestSent.get()) {
			return HttpHeaders.readOnlyHttpHeaders(this.headers);
		}
		else {
			return this.headers;
		}
	}

	@Override
	public HttpMethod getMethod() {
		return this.httpMethod;
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	@Override
	public void setBody(Publisher<DataBuffer> body) {

		this.body = Flux.from(body)
				.map(b -> new Buffer(b.asByteBuffer()))
				.doOnComplete(() -> this.successCallback.onSuccess(null))
				.doOnError(t -> this.failureCallback.onFailure(t));
	}

	@Override
	public void onSent(SuccessCallback<Void> successCallback, FailureCallback failureCallback) {
		this.successCallback = successCallback;
		this.failureCallback = failureCallback;
	}

	@Override
	public Mono<ClientHttpResponse> execute() {

		return this.httpClient.request(new Method(httpMethod.toString()), uri.toString(),
				channel -> {
					// see https://github.com/reactor/reactor-io/pull/8
					if (body == null) {
						channel.headers().removeTransferEncodingChunked();
					}
					headers.entrySet().stream().forEach(e -> channel.headers().set(e.getKey(), e.getValue()));
					if (!requestSent.compareAndSet(false, true)) {
						return Mono.error(new IllegalStateException("Could not flush request headers for request: "
						+ uri.toString()));
					}
					if (body != null) {
						return channel.writeBufferWith(body);
					}
					else {
						return channel.writeHeaders();
					}
				})
				.map(httpChannel -> new ReactorClientHttpResponse(httpChannel, allocator));
	}

}

