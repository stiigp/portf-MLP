package com.panucci.mlp.services.sessions;

import tech.tablesaw.api.Table;

public record TestDataset(
    String databaseName,
    String targetClassName,
    Table testTable
) {
}