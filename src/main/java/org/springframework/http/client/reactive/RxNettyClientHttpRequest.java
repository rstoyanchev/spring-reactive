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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import org.reactivestreams.Publisher;
import reactor.core.converter.RxJava1ObservableConverter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferAllocator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;
import org.springframework.web.client.RxJava1HttpClient;

/**
 * {@link ClientHttpRequest} implementation for the RxNetty HTTP client
 *
 * @author Brian Clozel
 * @see RxJava1HttpClient
 */
public class RxNettyClientHttpRequest implements ClientHttpRequest {

	private final NettyDataBufferAllocator allocator;

	private final HttpMethod httpMethod;

	private final URI uri;

	private final HttpHeaders headers;

	private Observable<ByteBuf> body;

	private SuccessCallback<Void> successCallback;

	private FailureCallback failureCallback;

	private final AtomicBoolean requestSent = new AtomicBoolean(false);


	public RxNettyClientHttpRequest(HttpMethod httpMethod, URI uri, HttpHeaders headers, NettyDataBufferAllocator allocator) {
		this.httpMethod = httpMethod;
		this.uri = uri;
		this.allocator = allocator;
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
	public void setBody(Publisher<DataBuffer> body) {

		this.body = RxJava1ObservableConverter.from(Flux.from(body)
				.map(b -> allocator.wrap(b.asByteBuffer()).getNativeBuffer())
				.doOnComplete(() -> this.successCallback.onSuccess(null))
				.doOnError(t -> this.failureCallback.onFailure(t)));
	}

	@Override
	public void onSent(SuccessCallback<Void> successCallback, FailureCallback failureCallback) {
		this.successCallback = successCallback;
		this.failureCallback = failureCallback;
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
	public Mono<ClientHttpResponse> execute() {
		try {
			HttpClientRequest<ByteBuf, ByteBuf> request = HttpClient
					.newClient(this.uri.getHost(), this.uri.getPort())
					.createRequest(io.netty.handler.codec.http.HttpMethod.valueOf(this.httpMethod.name()), uri.getRawPath());

			for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
				request = request.addHeader(entry.getKey(), entry.getValue().get(0));
			}
			if (this.body == null) {
				request = request.removeHeader("Transfer-Encoding");
			}
			if (!this.requestSent.compareAndSet(false, true)) {
				return Mono.error(new IllegalStateException("Could not flush request headers for request: "
						+ uri.toString()));
			}
			if (this.body != null) {

				return Mono.from(RxJava1ObservableConverter.from(request
						.writeContent(this.body)
						.map(response -> new RxNettyClientHttpResponse(response, this.allocator))));
			}
			else {
				return Mono.from(RxJava1ObservableConverter
						.from(request.map(response -> new RxNettyClientHttpResponse(response, this.allocator))));
			}

		}
		catch (IllegalArgumentException exc) {
			return Mono.error(exc);
		}
	}

}
