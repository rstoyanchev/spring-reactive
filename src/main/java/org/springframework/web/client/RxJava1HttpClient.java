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

import java.util.List;

import io.netty.buffer.UnpooledByteBufAllocator;
import rx.Observable;
import rx.Single;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.convert.support.ReactiveStreamsToRxJava1Converter;
import org.springframework.core.io.buffer.NettyDataBufferAllocator;
import org.springframework.http.client.reactive.ClientHttpRequestFactory;
import org.springframework.http.client.reactive.RxNettyHttpClientRequestFactory;

/**
 * HTTP client implementation for the RxJava1 composition API
 *
 * <p>This client is exposing RxJava's {@link Observable} and {@link Single} as
 * return types for composition. Any {@code ClientHttpRequestFactory} can be used
 * with it, but by default this class will use the {@link RxNettyHttpClientRequestFactory}
 * that requires the RxNetty module.
 *
 * @author Brian Clozel
 * @see rx.Observable
 * @see rx.Single
 */
public class RxJava1HttpClient {

	private final HttpClient reactiveClient;

	private final GenericConversionService conversionService;

	/**
	 * Create a {@code RxJava1RestClient}, using the RxNetty client
	 * implementation with {@link RxNettyHttpClientRequestFactory}.
	 */
	public RxJava1HttpClient() {
		this(new RxNettyHttpClientRequestFactory(new NettyDataBufferAllocator(UnpooledByteBufAllocator.DEFAULT)));
	}

	/**
	 * Create a {@code RxJava1RestClient}, using any {@link ClientHttpRequestFactory}
	 * implementation given as an argument.
	 *
	 * @param requestFactory the request factory to use
	 */
	public RxJava1HttpClient(ClientHttpRequestFactory requestFactory) {
		this.reactiveClient = new HttpClient(requestFactory);
		this.conversionService = new GenericConversionService();
		this.conversionService.addConverter(new ReactiveStreamsToRxJava1Converter());
	}

	public void setMessageEncoders(List<Encoder<?>> messageEncoders) {
		this.reactiveClient.setMessageEncoders(messageEncoders);
	}

	public void setMessageDecoders(List<Decoder<?>> messageDecoders) {
		this.reactiveClient.setMessageDecoders(messageDecoders);
	}

	/**
	 * Create a {@link RxJava1RestClientExecution} that will provide
	 * methods to consume the HTTP response
	 */
	public RxJava1RestClientExecution perform(RequestBuilder builder) {
		return new RxJava1RestClientExecution(builder);
	}

	/**
	 * Execution class that performs the actual HTTP request/response exchange
	 *
	 * This class exposes RxJava specific types for composing the response,
	 * such as {@link Observable} and {@link Single}.
	 *
	 * Pulling demand from the exposed types will result in:
	 * <ul>
	 *     <li>building the actual HTTP request using the provided {@code RequestBuilder}</li>
	 *     <li>encoding the HTTP request body with the configured {@code Encoder}s</li>
	 *     <li>consuming the response and decoding its body using the configured {@code Encoder}s</li>
	 *     <li>returning the proper types requested by the user</li>
	 * </ul>
	 */
	public class RxJava1RestClientExecution {

		private final RequestBuilder builder;

		public RxJava1RestClientExecution(RequestBuilder builder) {
			this.builder = builder;
		}

		/**
		 * Return a {@link Single} of the requested type
		 * @param resolvableType instance that expresses the return types requested by the user
		 */
		protected <T> Single<T> asSingle(ResolvableType resolvableType) {
			return conversionService.convert(reactiveClient.perform(builder).asMonoOf(resolvableType), Single.class);
		}

		/**
		 * Return a {@link Single} of the requested type
		 * @param sourceClass the main type requested for the return value
		 * @param generics the generics requested for the return value
		 */
		public <T> Single<T> asSingle(Class<T> sourceClass, Class<?>... generics) {
			return asSingle(ResolvableType.forClassWithGenerics(sourceClass, generics));
		}

		/**
		 *Return a {@link Observable} of the requested type
		 * @param resolvableType instance that expresses the return types requested by the user
		 */
		protected <T> Observable<T> asObservable(ResolvableType resolvableType) {
			return conversionService.convert(reactiveClient.perform(builder).asFluxOf(resolvableType), Observable.class);
		}

		/**
		 *Return a {@link Observable} of the requested type
		 * @param sourceClass the main type requested for the return value
		 * @param generics the generics requested for the return value
		 */
		public <T> Observable<T> asObservable(Class<T> sourceClass, Class<?>... generics) {
			return asObservable(ResolvableType.forClassWithGenerics(sourceClass, generics));
		}
	}
}
