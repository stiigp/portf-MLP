package com.panucci.mlp.core.datastructures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.panucci.mlp.core.dataprocessing.Reader;
import com.panucci.mlp.core.util.ActivationFunction;
import com.panucci.mlp.dto.TestFinishedEvent;
import com.panucci.mlp.dto.TestMessage;
import com.panucci.mlp.dto.TestProgressEvent;
import com.panucci.mlp.dto.TestStartedEvent;
import com.panucci.mlp.dto.TrainingEventOptions;
import com.panucci.mlp.listeners.TestListener;
import com.panucci.mlp.listeners.TrainingListener;

class MLPTest {

    @Test
    void emitsTestLifecycleEventsForEveryTestSample() {
        Reader reader = new Reader("iris", "Species");
        reader.normaliza();
        reader.oneHotEncode();

        List<TestMessage> events = new ArrayList<>();
        MLP mlp = new MLP(
            1,
            ActivationFunction.logistica,
            0.001,
            new TrainingListener() { },
            "training-session-1",
            TrainingEventOptions.defaults(),
            new TestListener() {
                @Override
                public void onTestStartEvent(TestMessage event) {
                    events.add(event);
                }

                @Override
                public void onTestProgressEvent(TestMessage event) {
                    events.add(event);
                }

                @Override
                public void onTestEndEvent(TestMessage event) {
                    events.add(event);
                }
            }
        );
        mlp.train(reader.getTrainTable(), "Species", 0.001, 1);

        mlp.test(reader.getTestTable(), "Species", "test-session-1");

        int totalSamples = reader.getTestTable().rowCount();
        assertEquals(totalSamples + 2, events.size());

        TestStartedEvent startedEvent = assertInstanceOf(
            TestStartedEvent.class,
            events.get(0)
        );
        assertEquals("TEST_STARTED", startedEvent.type());
        assertEquals("test-session-1", startedEvent.testSessionId());
        assertEquals("training-session-1", startedEvent.trainingSessionId());
        assertEquals(totalSamples, startedEvent.totalSamples());

        for (int sampleIndex = 0; sampleIndex < totalSamples; sampleIndex++) {
            TestProgressEvent progressEvent = assertInstanceOf(
                TestProgressEvent.class,
                events.get(sampleIndex + 1)
            );
            assertEquals("TEST_PROGRESS", progressEvent.type());
            assertEquals(sampleIndex, progressEvent.sampleIndex());
            assertTrue(progressEvent.accuracy() >= 0.0 && progressEvent.accuracy() <= 1.0);
            assertTrue(progressEvent.precision() >= 0.0 && progressEvent.precision() <= 1.0);
            assertTrue(progressEvent.f1Score() >= 0.0 && progressEvent.f1Score() <= 1.0);
        }

        TestFinishedEvent finishedEvent = assertInstanceOf(
            TestFinishedEvent.class,
            events.get(events.size() - 1)
        );
        assertEquals("TEST_FINISHED", finishedEvent.type());
        assertEquals(totalSamples, finishedEvent.processedSamples());
    }
}