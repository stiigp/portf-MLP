package com.panucci.mlp.services;

import com.panucci.mlp.core.dataprocessing.Reader;
import com.panucci.mlp.core.datastructures.MLP;
import com.panucci.mlp.core.util.ActivationFunction;
import com.panucci.mlp.dto.StartTrainingPayload;
import com.panucci.mlp.dto.TrainingEventOptions;
import com.panucci.mlp.dto.TrainingMessage;
import com.panucci.mlp.dto.TrainingProgressEvent;
import com.panucci.mlp.listeners.TrainingListener;
import com.panucci.mlp.services.factories.MlpFactory;
import com.panucci.mlp.services.factories.ReaderFactory;
import com.panucci.mlp.services.publishing.TrainingEventPublisher;
import com.panucci.mlp.services.sessions.SessionIdGenerator;
import com.panucci.mlp.services.sessions.TrainingSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.core.task.AsyncTaskExecutor;
import tech.tablesaw.api.Table;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertSame;

class MlpTrainingServiceTest {

    private final AsyncTaskExecutor sameThreadExecutor = new AsyncTaskExecutor() {
        @Override
        public void execute(Runnable task) {
            task.run();
        }

        @Override
        public Future<?> submit(Runnable task) {
            task.run();
            return CompletableFuture.completedFuture(null);
        }
    };

    private TrainingEventPublisher eventPublisher;
    private ReaderFactory readerFactory;
    private MlpFactory mlpFactory;
    private Reader reader;
    private MLP mlp;
    private Table trainTable;
    private MlpTrainingService service;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(TrainingEventPublisher.class);
        readerFactory = mock(ReaderFactory.class);
        mlpFactory = mock(MlpFactory.class);
        reader = mock(Reader.class);
        mlp = mock(MLP.class);
        trainTable = Table.create("train");

        service = new MlpTrainingService(
            eventPublisher,
            sameThreadExecutor,
            readerFactory,
            mlpFactory,
            new TrainingSessionService(new StaticSessionIdGenerator(), eventPublisher, 600000, 600000)
        );
    }

    @Test
    void startTrainingBuildsReaderAndMlpFromPayload() {
        StartTrainingPayload payload = validPayload();

        when(readerFactory.create("fruits", "fruit_name")).thenReturn(reader);
        when(reader.getTrainTable()).thenReturn(trainTable);
        when(mlpFactory.create(
            eq(2),
            eq(ActivationFunction.logistica),
            eq(0.1),
            any(TrainingListener.class),
            eq("session-1"),
            eq(TrainingEventOptions.defaults())
        )).thenReturn(mlp);

        service.startTraining(payload);

        verify(readerFactory).create("fruits", "fruit_name");
        verify(mlpFactory).create(
            eq(2),
            eq(ActivationFunction.logistica),
            eq(0.1),
            any(TrainingListener.class),
            eq("session-1"),
            eq(TrainingEventOptions.defaults())
        );
    }

    @Test
    void startTrainingPreparesReaderBeforeTrainingMlp() {
        StartTrainingPayload payload = validPayload();

        when(readerFactory.create("fruits", "fruit_name")).thenReturn(reader);
        when(reader.getTrainTable()).thenReturn(trainTable);
        when(mlpFactory.create(anyInt(), any(), anyDouble(), any(), anyString(), any())).thenReturn(mlp);

        service.startTraining(payload);

        InOrder inOrder = inOrder(reader, mlp);
        inOrder.verify(reader).normaliza();
        inOrder.verify(reader).oneHotEncode();
        inOrder.verify(reader).getTrainTable();
        inOrder.verify(mlp).train(trainTable, "fruit_name", 0.01, 5);
    }

    @Test
    void startTrainingDoesNothingWhenActivationFunctionIsInvalid() {
        StartTrainingPayload payload = new StartTrainingPayload(
            "session-1",
            "fruits",
            2,
            "invalid",
            0.1,
            0.01,
            5,
            null
        );

        service.startTraining(payload);

        verifyNoInteractions(readerFactory, mlpFactory);
    }

    @Test
    void startTrainingDoesNothingWhenDatabaseNameIsInvalid() {
        StartTrainingPayload payload = new StartTrainingPayload(
            "session-1",
            "invalid",
            2,
            "logistica",
            0.1,
            0.01,
            5,
            null
        );

        service.startTraining(payload);

        verifyNoInteractions(readerFactory, mlpFactory);
    }

    @Test
    void trainingListenerPublishesTrainingEvents() {
        StartTrainingPayload payload = validPayload();
        TrainingMessage event = new TrainingProgressEvent(
            "TRAINING_PROGRESS",
            "session-1",
            0,
            0,
            0.0
        );
        ArgumentCaptor<TrainingListener> listenerCaptor = ArgumentCaptor.forClass(TrainingListener.class);

        when(readerFactory.create("fruits", "fruit_name")).thenReturn(reader);
        when(reader.getTrainTable()).thenReturn(trainTable);
        when(mlpFactory.create(anyInt(), any(), anyDouble(), listenerCaptor.capture(), anyString(), any())).thenReturn(mlp);

        service.startTraining(payload);

        TrainingListener listener = listenerCaptor.getValue();
        listener.onTrainingStartEvent(event);
        listener.onForwardPassEvent(event);
        listener.onWeightsUpdateEvent(event);
        listener.onTrainingEndEvent(event);

        verify(eventPublisher, times(7)).publish(any());
        verify(eventPublisher, times(4)).publish(event);
    }

    @Test
    void trainingUsesSameTableReturnedByReader() {
        StartTrainingPayload payload = validPayload();

        when(readerFactory.create("fruits", "fruit_name")).thenReturn(reader);
        when(reader.getTrainTable()).thenReturn(trainTable);
        when(mlpFactory.create(anyInt(), any(), anyDouble(), any(), anyString(), any())).thenReturn(mlp);

        service.startTraining(payload);

        ArgumentCaptor<Table> tableCaptor = ArgumentCaptor.forClass(Table.class);
        verify(mlp).train(tableCaptor.capture(), eq("fruit_name"), eq(0.01), eq(5));
        assertSame(trainTable, tableCaptor.getValue());
    }

    private StartTrainingPayload validPayload() {
        return new StartTrainingPayload(
            "session-1",
            "fruits",
            2,
            "logistica",
            0.1,
            0.01,
            5,
            null
        );
    }

    private static class StaticSessionIdGenerator implements SessionIdGenerator {
        @Override
        public String generate() {
            return "session-1";
        }
    }
}
