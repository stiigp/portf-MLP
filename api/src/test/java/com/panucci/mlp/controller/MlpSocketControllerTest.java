package com.panucci.mlp.controller;

import static org.mockito.Mockito.verify;

import com.panucci.mlp.services.MlpTestingService;
import com.panucci.mlp.services.MlpTrainingService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MlpSocketControllerTest {

    @Test
    void startsTestingForTheRequestedTestSession() {
        MlpTrainingService trainingService = Mockito.mock(MlpTrainingService.class);
        MlpTestingService testingService = Mockito.mock(MlpTestingService.class);
        MlpSocketController controller = new MlpSocketController(trainingService, testingService);

        controller.startTest("test-session-1");

        verify(testingService).startTesting("test-session-1");
    }
}