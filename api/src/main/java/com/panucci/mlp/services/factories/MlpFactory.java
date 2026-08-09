package com.panucci.mlp.services.factories;

import com.panucci.mlp.core.datastructures.MLP;
import com.panucci.mlp.core.util.ActivationFunction;
import com.panucci.mlp.dto.TrainingEventOptions;
import com.panucci.mlp.listeners.TrainingListener;

public interface MlpFactory {
    public MLP create(int nCamadasOcultas, ActivationFunction activationFunction, double taxaDeAprendizado, TrainingListener listener, String sessionId, TrainingEventOptions eventOptions);
}
