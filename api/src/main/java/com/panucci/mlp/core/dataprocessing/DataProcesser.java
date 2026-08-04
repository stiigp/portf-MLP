package com.panucci.mlp.core.dataprocessing;

import org.springframework.web.multipart.MultipartFile;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class DataProcesser {
    private Table tabela, trainTable, testTable;
    private String nomeAtributoTarget;

    public DataProcesser(MultipartFile tabela, String nomeAtributoTarget) throws IOException {
        if (tabela != null)
            this.tabela = Table.read().csv(tabela.getInputStream());
        trainTable = null;
        testTable = null;
        this.nomeAtributoTarget = nomeAtributoTarget;
    }

    public void exibeTabela() {
        System.out.println(tabela.first(5));
    }

    public void oneHotEncode() {
        for (String nomeColuna:tabela.columnNames()) {
            if (tabela.column(nomeColuna) instanceof StringColumn)
                oneHotEncode(nomeColuna);
            else if (tabela.column(nomeColuna) instanceof BooleanColumn)
                booleanEncode(nomeColuna);
        }
    }

    public Table getTrainTable() {
        if (this.trainTable == null)
            trainTestSplit();

        return this.trainTable;
    }

    public Table getTestTable() {
        if (this.testTable == null)
            trainTestSplit();

        return this.testTable;
    }

    public void setTrainAndTestTable(Table train, Table test) {
        this.trainTable = train;
        this.testTable = test;
    }

    private void trainTestSplit() {
        trainTestSplit(0.7);
    }

    private void trainTestSplit(double percTrain) {
        List<Table> tabelasPorClasse = this.separaTabelaPorClasseTarget();
        this.trainTable = Table.create("train");
        this.testTable = Table.create("test");
        for (Column col : tabela.columns()) {
            this.trainTable.addColumns(col.emptyCopy());
            this.testTable.addColumns(col.emptyCopy());
        }

        for (Table subTabela:tabelasPorClasse) {
            shuffleTable(subTabela);

            int nRegistrosTreinoDaSubTabela = (int)(percTrain * subTabela.rowCount());
            this.trainTable.append(subTabela.inRange(0, nRegistrosTreinoDaSubTabela));
            this.testTable.append(subTabela.inRange(nRegistrosTreinoDaSubTabela, subTabela.rowCount()));
        }

        shuffleTable(this.trainTable);
        shuffleTable(this.testTable);
    }

    private void shuffleTable(Table tabelaEntrada) {
        // embaralha "in-place", usando memoização
        Random r = new Random();
        for (int i = 0; i < tabelaEntrada.rowCount(); i ++) {
            int novaPos = r.nextInt(tabelaEntrada.rowCount() - 1);
            Row aux = tabelaEntrada.row(i);

            insertRow(tabelaEntrada, i, tabelaEntrada.row(novaPos));
            insertRow(tabelaEntrada, novaPos, aux);
        }
    }

    private void insertRow(Table tabela, int pos, Row linhaNova) {
        Row linhaAlterar = tabela.row(pos);

        for (String nomeColuna:linhaAlterar.columnNames()) {
            Column coluna = tabela.column(nomeColuna);
            if (coluna.type() == ColumnType.FLOAT) {
                linhaAlterar.setFloat(nomeColuna, linhaNova.getFloat(nomeColuna));
            } else if (coluna.type() == ColumnType.DOUBLE) {
                linhaAlterar.setDouble(nomeColuna, linhaNova.getDouble(nomeColuna));
            } else if (coluna.type() == ColumnType.INTEGER) {
                linhaAlterar.setInt(nomeColuna, linhaNova.getInt(nomeColuna));
            }
        }
    }

    private void oneHotEncode(String nomeColunaACodificar) {
        // instanciando listas de zeros para cada nova coluna que será gerada
        HashMap<String, int[]> novasColunas = new HashMap<>();
        for (String nomeNovaColuna: retornaNomesColunasCodificadas(nomeColunaACodificar)) {
            int[] novaColuna = new int[tabela.rowCount()];

            novasColunas.put(nomeNovaColuna, novaColuna);
        }

        // após isso vamos iterar pela coluna que será codificada setando para 1 a respectiva posição
        // na respectiva nova coluna
        StringColumn coluna = tabela.stringColumn(nomeColunaACodificar);
        for (int indiceLinha = 0; indiceLinha < coluna.size(); indiceLinha++) {
            String valorColuna = coluna.get(indiceLinha);

            novasColunas.get(nomeColunaACodificar+"_"+valorColuna)[indiceLinha] = 1;
        }

        for (String nomeNovaColuna: novasColunas.keySet()) {
            IntColumn novaColuna = IntColumn.create(nomeNovaColuna, novasColunas.get(nomeNovaColuna));

            tabela.addColumns(novaColuna);
        }

        tabela.removeColumns(nomeColunaACodificar);
    }

    private void booleanEncode(String nomeColunaACodificar) {
        BooleanColumn colunaACodificar = tabela.booleanColumn(nomeColunaACodificar);

        int[] novaColuna = new int[colunaACodificar.size()];
        for (int i = 0; i < colunaACodificar.size(); i ++) {
            if (colunaACodificar.get(i) == true)
                novaColuna[i] = 1;
            else
                novaColuna[i] = 0;
        }

        tabela.removeColumns(nomeColunaACodificar);
        tabela.addColumns(IntColumn.create(nomeColunaACodificar, novaColuna));
    }

    private List<Table> separaTabelaPorClasseTarget() {
        List<Table> ret = new ArrayList<>();
        List<String> colunasTargetCodificadas = retornaNomesColunasTargetAposOneHotEncoding();

        for (String colunaTarget : colunasTargetCodificadas) {
            Table subTabela = tabela.where(tabela.intColumn(colunaTarget).isEqualTo(1));
            ret.add(subTabela);
        }

        return ret;
    }

    private List<String> retornaNomesColunasCodificadas(String nomeColuna) {
        StringColumn stringColumn = (StringColumn) tabela.column(nomeColuna);
        List<String> ret = new ArrayList<>();

        for (int i = 0; i < numeroDeClassesDaColuna(nomeColuna); i ++) {
            String nomeClasse = (String)stringColumn.countByCategory().get(i, 0);
            ret.add(nomeColuna+"_"+nomeClasse);
        }

        return ret;
    }

    private List<String> retornaNomesColunasTargetAposOneHotEncoding() {
        List<String> retorno = new ArrayList<>();

        for (String col:this.tabela.columnNames()) {
            if (col.split("_")[0].equals(this.nomeAtributoTarget))
                retorno.add(col);
        }

        return retorno;
    }

    private int numeroDeClassesDaColuna(String nomeColuna) {
        StringColumn stringColumn = (StringColumn) tabela.column(nomeColuna);

        return stringColumn.countByCategory().rowCount();
    }

    public Table getTabela() {
        return tabela;
    }
}
