package com.harman.ignite.orchestrator.worker.http.handler;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.builder.ServiceTaskBuilder;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.extension.testcontainer.ZeebeProcessTest;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@WireMockTest(httpPort = 8089)
@SpringBootTest
@ZeebeProcessTest
public class HttpJobHandlerTests {
	
	@Autowired
	private ZeebeTestEngine engine;
	
	@Autowired
	private ZeebeClient client;
	
	@BeforeEach
	public void configureApiMock(WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(get(urlEqualTo("/config"))
				.willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("[]")));

	}
	
	@Test
	  public void testGetRequest(WireMockRuntimeInfo wmRuntimeInfo) {

	    stubFor(
	        get(urlEqualTo("/api"))
	            .willReturn(
	                aResponse().withHeader("Content-Type", "application/json").withBody("{\"x\":1}")));

	    final var processInstance =
	        createInstance(
	            serviceTask ->
	                serviceTask
	                    .zeebeTaskHeader("url", wmRuntimeInfo.getHttpBaseUrl() + "/api")
	                    .zeebeTaskHeader("method", "GET"),
	            Collections.emptyMap());

	    //ZeebeTestThreadSupport.waitForProcessInstanceCompleted(processInstance);

	    BpmnAssert.assertThat(processInstance)
	        .isCompleted()
	        .hasVariableWithValue("statusCode", 200)
	        .hasVariableWithValue("body", Map.of("x", 1));

	    verify(getRequestedFor(urlEqualTo("/api")));
	  }
	
	
	private ProcessInstanceEvent createInstance(final Consumer<ServiceTaskBuilder> taskCustomizer) {
		return createInstance(taskCustomizer, new HashMap<>());
	}

	private ProcessInstanceEvent createInstance(final Consumer<ServiceTaskBuilder> taskCustomizer,
			Map<String, Object> variables) {

		ServiceTaskBuilder processBuilder = Bpmn.createExecutableProcess("process").startEvent().serviceTask("task",
				t -> t.zeebeJobType("http"));

		taskCustomizer.accept(processBuilder);
		processBuilder.endEvent();

		client.newDeployResourceCommand().addProcessModel(processBuilder.done(), "process.bpmn")
				.requestTimeout(Duration.ofSeconds(10)).send().join();

		return client.newCreateInstanceCommand().bpmnProcessId("process").latestVersion().variables(variables)
				.requestTimeout(Duration.ofSeconds(10)).send().join();
	}
	
	
	 
	

}
