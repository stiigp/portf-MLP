package com.panucci.mlp.core.datastructures;

public class EntradaPerceptron {
    // representa a entrada de um perceptron, diferente da entrada da main.java.com.panucci.mlp.datastructures.MLP
    // é o conjunto de um perceptron com o seu peso
    private Perceptron perceptron;
    private double peso;

    public EntradaPerceptron(Perceptron perceptron, double peso) {
        this.perceptron = perceptron;
        this.peso = peso;
    }

    public double geraResultado() {
        return this.perceptron.getSaida() * this.peso;
    }

    public void atualizaPeso(double peso) {
        this.peso = peso;
    }

    public double getPeso() {
        return peso;
    }

    public double getSaida() {
        return this.perceptron.getSaida();
    }
}
