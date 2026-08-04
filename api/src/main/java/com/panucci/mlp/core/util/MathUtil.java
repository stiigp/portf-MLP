package com.panucci.mlp.core.util;

import java.util.List;

public class MathUtil {
    public static double mean(List<Double> conjunto) {
        double soma = 0;
        for (double ele:conjunto)
            soma += ele;

        return soma / conjunto.size();
    }

    public static double desvioPadrao(List<Double> conjunto) {
        double media = mean(conjunto);
        double soma = 0;

        for (double ele: conjunto) {
            soma += Math.pow((ele - media), 2);
        }

        return Math.sqrt(soma / conjunto.size());
    }
}
