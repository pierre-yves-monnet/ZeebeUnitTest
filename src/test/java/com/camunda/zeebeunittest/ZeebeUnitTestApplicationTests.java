package com.camunda.zeebeunittest;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.assertions.DeploymentAssert;
import io.camunda.zeebe.process.test.filters.RecordStream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import io.camunda.zeebe.process.test.extension.testcontainer.ZeebeProcessTest;

@SpringBootTest
@ZeebeProcessTest
class ZeebeUnitTestApplicationTests {
  private ZeebeTestEngine engine;
  private ZeebeClient client;
  private RecordStream recordStream;

  @Test
  void deployment() {
    DeploymentEvent event = client.newDeployResourceCommand()
        .addResourceFromClasspath("ZeebeUnitTest.bpmn")
        .send()
        .join();
    DeploymentAssert assertions = BpmnAssert.assertThat(event);
  }

}
