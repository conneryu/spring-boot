/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.logging.logback;

import java.util.List;
import java.util.function.Supplier;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.WarnStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SystemStatusListener}.
 *
 * @author Dmytro Nosan
 * @author Phillip Webb
 */
@ExtendWith(OutputCaptureExtension.class)
class SystemStatusListenerTests {

	private static final String TEST_MESSAGE = "testtesttest";

	private final StatusManager statusManager = mock(StatusManager.class);

	private final LoggerContext loggerContext = mock(LoggerContext.class);

	SystemStatusListenerTests() {
		given(this.loggerContext.getStatusManager()).willReturn(this.statusManager);
		given(this.statusManager.add(any(StatusListener.class))).willReturn(true);
	}

	@Test
	void addStatusWithInfoLevelWhenNoDebugDoesNotPrint(CapturedOutput output) {
		addStatus(false, () -> new InfoStatus(TEST_MESSAGE, null));
		assertThat(output.getOut()).doesNotContain(TEST_MESSAGE);
		assertThat(output.getErr()).doesNotContain(TEST_MESSAGE);
	}

	@Test
	void addStatusWithWarningLevelWhenNoDebugPrintsToSystemErr(CapturedOutput output) {
		addStatus(false, () -> new WarnStatus(TEST_MESSAGE, null));
		assertThat(output.getOut()).doesNotContain(TEST_MESSAGE);
		assertThat(output.getErr()).contains(TEST_MESSAGE);
	}

	@Test
	void addStatusWithErrorLevelWhenNoDebugPrintsToSystemErr(CapturedOutput output) {
		addStatus(false, () -> new ErrorStatus(TEST_MESSAGE, null));
		assertThat(output.getOut()).doesNotContain(TEST_MESSAGE);
		assertThat(output.getErr()).contains(TEST_MESSAGE);
	}

	@Test
	void addStatusWithInfoLevelWhenDebugPrintsToSystemOut(CapturedOutput output) {
		addStatus(true, () -> new InfoStatus(TEST_MESSAGE, null));
		assertThat(output.getOut()).contains(TEST_MESSAGE);
		assertThat(output.getErr()).doesNotContain(TEST_MESSAGE);
	}

	@Test
	void addStatusWithWarningLevelWhenDebugPrintsToSystemOut(CapturedOutput output) {
		addStatus(true, () -> new WarnStatus(TEST_MESSAGE, null));
		assertThat(output.getOut()).contains(TEST_MESSAGE);
		assertThat(output.getErr()).doesNotContain(TEST_MESSAGE);
	}

	@Test
	void addStatusWithErrorLevelWhenDebugPrintsToSystemOut(CapturedOutput output) {
		addStatus(true, () -> new ErrorStatus(TEST_MESSAGE, null));
		assertThat(output.getOut()).contains(TEST_MESSAGE);
		assertThat(output.getErr()).doesNotContain(TEST_MESSAGE);
	}

	@Test
	void shouldRetrospectivePrintStatusOnStartAndDebugIsDisabled(CapturedOutput output) {
		given(this.statusManager.getCopyOfStatusList()).willReturn(List.of(new ErrorStatus(TEST_MESSAGE, null),
				new WarnStatus(TEST_MESSAGE, null), new InfoStatus(TEST_MESSAGE, null)));
		addStatus(false, () -> new InfoStatus(TEST_MESSAGE, null));
		assertThat(output.getErr()).contains("WARN " + TEST_MESSAGE);
		assertThat(output.getErr()).contains("ERROR " + TEST_MESSAGE);
		assertThat(output.getErr()).doesNotContain("INFO");
		assertThat(output.getOut()).isEmpty();
	}

	@Test
	void shouldRetrospectivePrintStatusOnStartAndDebugIsEnabled(CapturedOutput output) {
		given(this.statusManager.getCopyOfStatusList()).willReturn(List.of(new ErrorStatus(TEST_MESSAGE, null),
				new WarnStatus(TEST_MESSAGE, null), new InfoStatus(TEST_MESSAGE, null)));
		addStatus(true, () -> new InfoStatus(TEST_MESSAGE, null));
		assertThat(output.getErr()).isEmpty();
		assertThat(output.getOut()).contains("WARN " + TEST_MESSAGE);
		assertThat(output.getOut()).contains("ERROR " + TEST_MESSAGE);
		assertThat(output.getOut()).contains("INFO " + TEST_MESSAGE);
	}

	@Test
	void shouldNotRetrospectivePrintWhenStatusIsOutdated(CapturedOutput output) {
		ErrorStatus outdatedStatus = new ErrorStatus(TEST_MESSAGE, null);
		ReflectionTestUtils.setField(outdatedStatus, "timestamp", System.currentTimeMillis() - 300);
		given(this.statusManager.getCopyOfStatusList()).willReturn(List.of(outdatedStatus));
		addStatus(false, () -> new InfoStatus(TEST_MESSAGE, null));
		assertThat(output.getOut()).isEmpty();
		assertThat(output.getErr()).isEmpty();
	}

	private void addStatus(boolean debug, Supplier<Status> statusFactory) {
		SystemStatusListener.addTo(this.loggerContext, debug);
		ArgumentCaptor<StatusListener> listener = ArgumentCaptor.forClass(StatusListener.class);
		then(this.statusManager).should().add(listener.capture());
		assertThat(listener.getValue()).extracting("context").isSameAs(this.loggerContext);
		listener.getValue().addStatusEvent(statusFactory.get());
	}

}
