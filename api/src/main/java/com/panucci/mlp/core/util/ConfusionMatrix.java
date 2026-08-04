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
