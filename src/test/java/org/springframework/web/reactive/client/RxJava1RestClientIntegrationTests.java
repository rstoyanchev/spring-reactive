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

package org.springframework.web.reactive.client;

import static org.junit.Assert.*;

import java.util.Arrays;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import rx.Single;
import rx.observers.TestSubscriber;

import org.springframework.core.codec.support.Pojo;
import org.springframework.http.MediaType;
import org.springframework.web.client.RequestBuilders;
import org.springframework.web.client.RxJava1HttpClient;

/**
 * @author Brian Clozel
 */
public class RxJava1RestClientIntegrationTests {

	private MockWebServer server;

	private RxJava1HttpClient restClient;


	@Before
	public void setup() {
		this.server = new MockWebServer();
		this.restClient = new RxJava1HttpClient();
	}

	@Test
	@Ignore // See https://github.com/ReactiveX/RxNetty/issues/119
	public void getTextPlain() throws Exception {
		HttpUrl baseUrl = server.url("/greeting?name=Spring");

		Single<String> result = this.restClient
				.perform(RequestBuilders.get(baseUrl.toString())
						.accept(MediaType.TEXT_PLAIN))
				.asSingle(String.class);


		TestSubscriber<String> ts = new TestSubscriber<String>();
		result.subscribe(ts);
		ts.awaitTerminalEvent();
		ts.assertReceivedOnNext(Arrays.asList("Hello Spring!"));
		ts.assertCompleted();
	}

	@Test
	@Ignore // See https://github.com/ReactiveX/RxNetty/issues/119
	public void getPerson() throws Exception {
		HttpUrl baseUrl = server.url("/pojo");
		this.server.enqueue(new MockResponse().setBody("{\"bar\":\"barbar\",\"foo\":\"foofoo\"}"));

		Single<Pojo> result = this.restClient
				.perform(RequestBuilders.get(baseUrl.toString())
						.accept(MediaType.APPLICATION_JSON))
				.asSingle(Pojo.class);

		TestSubscriber<Pojo> ts = new TestSubscriber<Pojo>();
		result.subscribe(ts);

		ts.awaitTerminalEvent();
		ts.assertValueCount(1);
		Assert.assertEquals("barbar", ts.getOnNextEvents().get(0).getBar());
		ts.assertCompleted();
	}

	@Test
	public void postPerson() throws Exception {
		HttpUrl baseUrl = server.url("/pojo/capitalize");
		this.server.enqueue(new MockResponse().setBody("{\"bar\":\"REACTIVE\",\"foo\":\"SPRING\"}"));

		Pojo spring = new Pojo("spring", "reactive");

		Single<Pojo> response = this.restClient
				.perform(RequestBuilders.post(baseUrl.toString())
						.content(spring)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.asSingle(Pojo.class);

		TestSubscriber<Pojo> ts = new TestSubscriber<Pojo>();
		response.subscribe(ts);

		ts.awaitTerminalEvent();
		ts.assertValueCount(1);
		Assert.assertEquals("SPRING", ts.getOnNextEvents().get(0).getFoo());
		ts.assertCompleted();

		RecordedRequest request = server.takeRequest();
		assertEquals("/pojo/capitalize", request.getPath());
		Assert.assertEquals("{\"foo\":\"spring\",\"bar\":\"reactive\"}", request.getBody().readUtf8());
	}

	@After
	public void tearDown() throws Exception {
		this.server.shutdown();
	}

}
