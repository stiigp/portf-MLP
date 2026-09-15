package com.panucci.mlp.core.dataprocessing;

import com.panucci.mlp.core.datastructures.MLP;
import com.panucci.mlp.core.util.ActivationFunction;
import com.panucci.mlp.dto.TrainingEventOptions;
import com.panucci.mlp.dto.TrainingMessage;
import com.panucci.mlp.listeners.TrainingListener;
import org.junit.jupiter.api.Test;
import tech.tablesaw.api.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReaderTest {

    @Test
    void loadsIrisWithExpectedOneHotTargetAndNoIdColumn() {
        Reader reader = new Reader("iris", "Species");

        reader.normaliza();
        reader.oneHotEncode();

        Table trainTable = reader.getTrainTable();

        assertFalse(trainTable.columnNames().contains("Id"));
        assertEquals(7, trainTable.columnCount());
        assertTrue(trainTable.columnNames().contains("Species__Iris-setosa"));
        assertTrue(trainTable.columnNames().contains("Species__Iris-versicolor"));
        assertTrue(trainTable.columnNames().contains("Species__Iris-virginica"));
    }

    @Test
    void loadsRaisinsWithDecimalCommaColumnsAsNumericInputs() {
        Reader reader = new Reader("raisins", "Class");

        reader.normaliza();
        reader.oneHotEncode();

        Table trainTable = reader.getTrainTable();

        assertEquals(9, trainTable.columnCount());
        assertTrue(trainTable.columnNames().contains("Class__Kecimen"));
        assertTrue(trainTable.columnNames().contains("Class__Besni"));
    }

    @Test
    void raisinsTrainTableCanBeUsedByMlp() {
        Reader reader = new Reader("raisins", "Class");
        reader.normaliza();
        reader.oneHotEncode();

        MLP mlp = new MLP(
            1,
            ActivationFunction.logistica,
            0.001,
            noOpTrainingListener(),
            "test-session",
            TrainingEventOptions.defaults()
        );

        mlp.train(reader.getTrainTable(), "Class", 0.001, 1);
    }

    @Test
    void loadsBankNotesWithNumericTargetEncodedAsTwoOutputColumns() {
        Reader reader = new Reader("bank_notes", "class");

        reader.normaliza();
        reader.oneHotEncode();

        Table trainTable = reader.getTrainTable();
        long targetColumnCount = trainTable.columnNames().stream()
            .filter(columnName -> columnName.startsWith("class__"))
            .count();

        assertEquals(6, trainTable.columnCount());
        assertEquals(2, targetColumnCount);
    }

    private TrainingListener noOpTrainingListener() {
        return new TrainingListener() {
            @Override
            public void onTrainingStartEvent(TrainingMessage event) {
            }

            @Override
            public void onForwardPassEvent(TrainingMessage event) {
            }

            @Override
            public void onWeightsUpdateEvent(TrainingMessage event) {
            }

            @Override
            public void onTrainingEndEvent(TrainingMessage event) {
            }
        };
    }
}