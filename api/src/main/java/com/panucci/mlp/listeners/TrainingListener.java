package com.panucci.mlp.listeners;

import com.panucci.mlp.dto.TrainingEvent;

public interface TrainingListener {
    default void onWeightsUpdateEvent(TrainingEvent event){};
    default void onTrainingStartEvent(TrainingEvent event){};
    default void onTrainingEndEvent(TrainingEvent event){};
    default void onForwardPassEvent(TrainingEvent event){};
}
