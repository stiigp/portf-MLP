package com.panucci.mlp.services.publishing;

import com.panucci.mlp.dto.TrainingMessage;

public interface TrainingEventPublisher {
    public void publish(TrainingMessage event);
}
