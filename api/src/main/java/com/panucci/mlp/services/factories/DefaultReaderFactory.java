package com.panucci.mlp.services.factories;

import org.springframework.stereotype.Component;

import com.panucci.mlp.core.dataprocessing.Reader;

@Component("defaultReaderFactory")
public class DefaultReaderFactory implements ReaderFactory {

    @Override
    public Reader create(String nomeTabela, String nomeAtributoTarget) {
        return new Reader(nomeTabela, nomeAtributoTarget);
    }
}
