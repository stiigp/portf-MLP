package com.panucci.mlp.listeners;

import com.panucci.mlp.dto.TrainingMessage;

public interface TrainingListener {
    default void onWeightsUpdateEvent(TrainingMessage event){};
    default void onTrainingStartEvent(TrainingMessage event){};
    default void onTrainingEndEvent(TrainingMessage event){};
    default void onForwardPassEvent(TrainingMessage event){};
}
