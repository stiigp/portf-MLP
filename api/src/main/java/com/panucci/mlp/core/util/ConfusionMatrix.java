package com.panucci.mlp.core.util;

import java.util.List;

public class ConfusionMatrix {
    private List<String> atributosTarget;
    private int[][] matrix;

    public ConfusionMatrix(List<String> atributosTarget) {
        this.atributosTarget = atributosTarget;
        this.matrix = new int[atributosTarget.size()][atributosTarget.size()];
    }

    public void addResult(int got, int expected) {
        this.matrix[got][expected] ++;
    }

    public double accuracy() {
        int total = 0;
        int correct = 0;

        for (int predictedClassIndex = 0; predictedClassIndex < this.matrix.length; predictedClassIndex++) {
            for (int expectedClassIndex = 0; expectedClassIndex < this.matrix.length; expectedClassIndex++) {
                int value = this.matrix[predictedClassIndex][expectedClassIndex];
                total += value;
                if (predictedClassIndex == expectedClassIndex) {
                    correct += value;
                }
            }
        }

        return total == 0 ? 0.0 : (double) correct / total;
    }

    public double macroPrecision() {
        return macroAverage(this::precisionForClass);
    }

    public double macroF1Score() {
        return macroAverage(this::f1ScoreForClass);
    }

    private double macroAverage(java.util.function.IntToDoubleFunction metric) {
        if (this.matrix.length == 0) {
            return 0.0;
        }

        double sum = 0.0;
        for (int classIndex = 0; classIndex < this.matrix.length; classIndex++) {
            sum += metric.applyAsDouble(classIndex);
        }

        return sum / this.matrix.length;
    }

    private double precisionForClass(int classIndex) {
        int predictedAsClass = 0;
        for (int expectedClassIndex = 0; expectedClassIndex < this.matrix.length; expectedClassIndex++) {
            predictedAsClass += this.matrix[classIndex][expectedClassIndex];
        }

        return predictedAsClass == 0
            ? 0.0
            : (double) this.matrix[classIndex][classIndex] / predictedAsClass;
    }

    private double f1ScoreForClass(int classIndex) {
        double precision = precisionForClass(classIndex);
        int expectedAsClass = 0;
        for (int predictedClassIndex = 0; predictedClassIndex < this.matrix.length; predictedClassIndex++) {
            expectedAsClass += this.matrix[predictedClassIndex][classIndex];
        }

        double recall = expectedAsClass == 0
            ? 0.0
            : (double) this.matrix[classIndex][classIndex] / expectedAsClass;
        return precision + recall == 0.0
            ? 0.0
            : 2.0 * precision * recall / (precision + recall);
    }
    public void print() {
        int n = atributosTarget.size();

        // calcula a largura máxima de uma coluna (para alinhar)
        int maxLabelLength = atributosTarget.stream()
                .mapToInt(String::length)
                .max()
                .orElse(5);

        int cellWidth = Math.max(6, maxLabelLength + 2);

        // cabeçalho
        System.out.printf("%" + cellWidth + "s", ""); // canto superior esquerdo
        for (String label : atributosTarget)
            System.out.printf("%" + cellWidth + "s", label);
        System.out.println();

        for (int i = 0; i < n; i++) {
            // nome da classe da linha
            System.out.printf("%" + cellWidth + "s", atributosTarget.get(i));
            for (int j = 0; j < n; j++) {
                System.out.printf("%" + cellWidth + "d", matrix[i][j]);
            }
            System.out.println();
        }
    }
}
