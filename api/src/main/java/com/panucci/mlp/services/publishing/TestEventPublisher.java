package com.panucci.mlp.services.publishing;

import com.panucci.mlp.dto.TestMessage;

public interface TestEventPublisher {
    void publish(TestMessage event);
}