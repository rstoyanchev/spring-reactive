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


import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpRequestFactory;
import org.springframework.http.client.reactive.HttpRequestBuilder;

/**
 * Builds a {@link ClientHttpRequest}
 *
 * <p>See static factory methods in {@link RequestBuilders}
 *
 * @author Brian Clozel
 * @see RequestBuilders
 */
public class RequestBuilder implements HttpRequestBuilder {

	protected HttpMethod httpMethod;

	protected HttpHeaders httpHeaders;

	protected URI url;

	protected Object content;

	protected final List<HttpCookie> cookies = new ArrayList<HttpCookie>();

	protected RequestBuilder() {
	}

	public RequestBuilder(HttpMethod httpMethod, String urlTemplate, Object... urlVariables) throws RestClientException {
		this.httpMethod = httpMethod;
		this.httpHeaders = new HttpHeaders();
		this.url = parseURI(urlTemplate);
	}

	public RequestBuilder(HttpMethod httpMethod, URI url) {
		this.httpMethod = httpMethod;
		this.httpHeaders = new HttpHeaders();
		this.url = url;
	}

	private URI parseURI(String uri) throws RestClientException {
		try {
			return new URI(uri);
		}
		catch (URISyntaxException e) {
			throw new RestClientException("could not parse URL template", e);
		}
	}

	public RequestBuilder param(String name, String... values) {
		return this;
	}

	public RequestBuilder header(String name, String... values) {
		Arrays.stream(values).forEach(value -> this.httpHeaders.add(name, value));
		return this;
	}

	public RequestBuilder headers(HttpHeaders httpHeaders) {
		this.httpHeaders = httpHeaders;
		return this;
	}

	public RequestBuilder contentType(MediaType contentType) {
		this.httpHeaders.setContentType(contentType);
		return this;
	}

	public RequestBuilder contentType(String contentType) {
		this.httpHeaders.setContentType(MediaType.parseMediaType(contentType));
		return this;
	}

	public RequestBuilder accept(MediaType... mediaTypes) {
		this.httpHeaders.setAccept(Arrays.asList(mediaTypes));
		return this;
	}

	public RequestBuilder accept(String... mediaTypes) {
		this.httpHeaders.setAccept(Arrays.stream(mediaTypes)
				.map(type -> MediaType.parseMediaType(type))
				.collect(Collectors.toList()));
		return this;
	}

	public RequestBuilder content(Object content) {
		this.content = content;
		return this;
	}

	public ClientHttpRequest build(ClientHttpRequestFactory factory) {
		ClientHttpRequest request = factory.createRequest(this.httpMethod, this.url, this.httpHeaders);
		request.getHeaders().putAll(this.httpHeaders);
		return request;
	}

	public Object getContent() {
		return this.content;
	}

}