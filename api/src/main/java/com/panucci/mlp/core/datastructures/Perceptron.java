package com.panucci.mlp.core.datastructures;

import com.panucci.mlp.core.util.ActivationFunction;

import java.util.List;

public class Perceptron {
    // precisamos de um conjunto de entradas, um valor net e um valor de saída
    private double net;
    private double saida;
    private double erro, gradienteErro;
    private List<EntradaPerceptron> entradas;
    private ActivationFunction activationFunction;

    public Perceptron(List<EntradaPerceptron> entradas, ActivationFunction activationFunction) {
        this.entradas = entradas;
        this.activationFunction = activationFunction;

        this.net = Double.MAX_VALUE;
        this.saida = Double.MAX_VALUE;
        this.erro = Double.MAX_VALUE;
        this.gradienteErro = Double.MAX_VALUE;
    }

    public void geraErrosSaida(int saidaEsperada) {
        this.erro = saidaEsperada - this.getSaida();
        this.gradienteErro = this.erro * activationFunction.applyDeriv(this.net);
    }

    private void geraSaida() {
        this.net = 0;

        for (EntradaPerceptron entrada:entradas)
            this.net += entrada.geraResultado();

        this.saida = this.activationFunction.apply(this.net);
    }

    public double getSaida() {
        if (this.saida == Double.MAX_VALUE) {
            this.geraSaida();
        }

        return this.saida;
    }

    public void resetaNetAndSaida() {
        this.net = Double.MAX_VALUE;
        this.saida = Double.MAX_VALUE;
    }

    public double getGradienteErro() {
        return gradienteErro;
    }

    public void setGradienteErro(double gradienteErro) {
        this.gradienteErro = gradienteErro;
    }

    public List<EntradaPerceptron> getEntradas() {
        return entradas;
    }

    public double getNet() {
        return net;
    }

    public double getErro() {
        return erro;
    }
}
