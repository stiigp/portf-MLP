package com.panucci.mlp.services.publishing;

import com.panucci.mlp.dto.TrainingEvent;

public interface TrainingEventPublisher {
    public void publish(TrainingEvent event);
}
