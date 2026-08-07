package com.panucci.mlp.services.factories;

import org.springframework.stereotype.Component;

import com.panucci.mlp.core.datastructures.MLP;
import com.panucci.mlp.core.util.ActivationFunction;
import com.panucci.mlp.listeners.TrainingListener;

@Component("defaultMlpFactory")
public class DefaultMlpFactory implements MlpFactory {
    
    @Override
    public MLP create(int nCamadasOcultas, ActivationFunction activationFunction, double taxaDeAprendizado, TrainingListener listener, String sessionId) {
        return new MLP(
            nCamadasOcultas,
            activationFunction,
            taxaDeAprendizado,
            listener,
            sessionId
        );
    }
}
