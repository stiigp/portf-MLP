package com.panucci.mlp.core.datastructures;

public class EntradaMLP extends Perceptron {
    private double valor;

    public EntradaMLP(double valor) {
        super(null, null);
        this.valor = valor;
    }

    @Override
    public double getSaida() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }
}
