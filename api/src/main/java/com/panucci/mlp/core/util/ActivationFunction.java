package com.panucci.mlp.core.util;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ActivationFunction {
    private Function<Double, Double> activ, derivActiv;

    public ActivationFunction(Function<Double, Double> activ, Function<Double, Double> derivActiv) {
        this.derivActiv = derivActiv;
        this.activ = activ;
    }

    public double apply(double x) {
        return this.activ.apply(x);
    }

    public double applyDeriv(double x) {
        return this.derivActiv.apply(x);
    }

    public static ActivationFunction logistica = new ActivationFunction(
            x -> 1.0 / (1.0 + Math.exp(-x)),
            x -> {
                double fx = 1.0 / (1.0 + Math.exp(-x));
                return fx * (1 - fx);
            }
    );

    public static final ActivationFunction tangenteHiperbolica = new ActivationFunction(
            x -> Math.tanh(x),
            y -> 1 - Math.pow(Math.tanh(y), 2)
    );

    public static final ActivationFunction netSobreDois = new ActivationFunction(
        x -> x/2,
        y -> 0.5
    );

    public static final ActivationFunction linear = new ActivationFunction(
            x -> x/10,
            y -> 0.1
    );

    public static final HashMap<String, ActivationFunction> nameToActivationFunctionMap = new HashMap(Map.of(
        "logistica", logistica,
        "logistic", logistica,

        "tangenteHiperbolica", tangenteHiperbolica,
        "hiperbolicTan", tangenteHiperbolica,

        "netSobreDois", netSobreDois,
        "netOverTwo", netSobreDois,

        "linear", linear
    ));
}
