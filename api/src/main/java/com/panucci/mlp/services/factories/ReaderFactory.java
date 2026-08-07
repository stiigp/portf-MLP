package com.panucci.mlp.services.factories;

import com.panucci.mlp.core.dataprocessing.Reader;

public interface ReaderFactory {
    public Reader create(String nomeTabela, String nomeAtributoTarget);
}
