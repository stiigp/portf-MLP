package com.panucci.mlp.listeners;

import com.panucci.mlp.dto.TestMessage;

public interface TestListener {
    default void onTestStartEvent(TestMessage event) {
    }

    default void onTestProgressEvent(TestMessage event) {
    }

    default void onTestEndEvent(TestMessage event) {
    }
}