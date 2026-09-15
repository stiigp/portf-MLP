package com.panucci.mlp.core.dataprocessing;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.io.IOException;
import java.net.URL;

public class Reader {
    private Table tabela, trainTable, testTable;
    private String nomeAtributoTarget;
    public static final HashMap<String, String> tableNameToTargetClass = new HashMap<>(Map.of(
        "mushrooms", "class",
        "fruits", "fruit_name",
        "iris", "Species",
        "raisins", "Class",
        "bank_notes", "class"
    ));;

    public Reader(String nomeTabela, String nomeAtributoTarget) {
        this.nomeAtributoTarget = nomeAtributoTarget;
        String resourcePath = "data/" + nomeTabela + ".csv";

        URL resource = Thread.currentThread()
            .getContextClassLoader()
            .getResource(resourcePath);

        if (resource == null) {
            throw new IllegalArgumentException("Dataset not found: " + resourcePath);
        }

        try {
            this.tabela = Table.read().usingOptions(csvReadOptionsBuilder(resource, nomeTabela));
        } catch (IOException error) {
            System.out.println("IOException: " + error.getMessage());
        }

        removeColunasIdentificadoras();
        converteColunasTextoNumericas();
        
        trainTable = null;
        testTable = null;
    }


    private CsvReadOptions.Builder csvReadOptionsBuilder(URL resource, String nomeTabela) throws IOException {
        CsvReadOptions.Builder builder = CsvReadOptions.builder(resource);

        if (nomeTabela.equals("raisins"))
            builder.locale(Locale.forLanguageTag("pt-BR"));

        return builder;
    }
    public void exibeTabela() {
        System.out.println(tabela.first(5));
    }

    public void normaliza() {
        for (Column<?> c: new ArrayList<>(tabela.columns())) {
            if (c.name().equals(nomeAtributoTarget))
                continue;

            if (c instanceof IntColumn) {
                normaliza((IntColumn) c);
            } else if (c instanceof DoubleColumn) {
                normaliza((DoubleColumn) c);
            } else if (c instanceof FloatColumn) {
                normaliza((FloatColumn) c);
            } else if (c instanceof LongColumn) {
                normaliza((LongColumn) c);
            }
        }
    }

    private void normaliza(LongColumn c) {
        double max = c.max();
        double min = c.min();

        double range = max - min;

        double[] colunaNovaArr = new double[c.size()];

        for (int i = 0; i < c.size(); i++) {
            double valor = c.getLong(i);
            double normalizado = (valor - min) / range;

            colunaNovaArr[i] = normalizado;
        }

        DoubleColumn colunaNova = DoubleColumn.create(c.name(), colunaNovaArr);

        tabela.removeColumns(c.name());
        tabela.addColumns(colunaNova);
    }
    private void normaliza(IntColumn c) {
        double max = c.max();
        double min = c.min();

        double range = max - min;

        double[] colunaNovaArr = new double[c.size()];

        for (int i = 0; i < c.size(); i++) {
            double valor = c.getInt(i);
            double normalizado = (valor - min) / range;

            colunaNovaArr[i] = normalizado;
        }

        DoubleColumn colunaNova = DoubleColumn.create(c.name(), colunaNovaArr);

        tabela.removeColumns(c.name());
        tabela.addColumns(colunaNova);
    }

    private void normaliza(DoubleColumn c) {
        double max = c.max();
        double min = c.min();

        double range = max - min;

        for (int i = 0; i < c.size(); i++) {
            double valor = c.getDouble(i);
            double normalizado = (valor - min) / range;

            c.set(i, (float)normalizado);
        }
    }

    private void normaliza(FloatColumn c) {
        double max = c.max();
        double min = c.min();

        double range = max - min;

        for (int i = 0; i < c.size(); i++) {
            double valor = c.getFloat(i);
            double normalizado = (valor - min) / range;

            c.set(i, (float)normalizado);
        }
    }

    public void oneHotEncode() {
        encodeNumericTargetAsString();

        for (String nomeColuna:new ArrayList<>(tabela.columnNames())) {
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

    private void removeColunasIdentificadoras() {
        if (tabela.columnNames().contains("Id") && !nomeAtributoTarget.equals("Id"))
            tabela.removeColumns("Id");
    }

    private void converteColunasTextoNumericas() {
        for (Column<?> coluna:new ArrayList<>(tabela.columns())) {
            if (!(coluna instanceof StringColumn) || coluna.name().equals(this.nomeAtributoTarget))
                continue;

            StringColumn colunaTexto = (StringColumn) coluna;
            double[] valores = new double[colunaTexto.size()];

            boolean colunaNumerica = true;
            for (int i = 0; i < colunaTexto.size(); i++) {
                try {
                    valores[i] = parseDouble(colunaTexto.get(i));
                } catch (NumberFormatException error) {
                    colunaNumerica = false;
                    break;
                }
            }

            if (colunaNumerica) {
                tabela.removeColumns(coluna.name());
                tabela.addColumns(DoubleColumn.create(coluna.name(), valores));
            }
        }
    }

    private double parseDouble(String valor) {
        return Double.parseDouble(valor.trim().replace(",", "."));
    }

    private void encodeNumericTargetAsString() {
        if (tabela.column(nomeAtributoTarget) instanceof StringColumn)
            return;

        Column<?> colunaTarget = tabela.column(nomeAtributoTarget);
        StringColumn colunaTargetTexto = StringColumn.create(nomeAtributoTarget);

        for (int i = 0; i < colunaTarget.size(); i++) {
            colunaTargetTexto.append(colunaTarget.getString(i));
        }

        tabela.removeColumns(nomeAtributoTarget);
        tabela.addColumns(colunaTargetTexto);
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
            } else if (coluna.type() == ColumnType.LONG) {
                linhaAlterar.setLong(nomeColuna, linhaNova.getLong(nomeColuna));
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

            novasColunas.get(nomeColunaACodificar+"__"+valorColuna)[indiceLinha] = 1;
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
            ret.add(nomeColuna+"__"+nomeClasse);
        }

        return ret;
    }

    private List<String> retornaNomesColunasTargetAposOneHotEncoding() {
        List<String> retorno = new ArrayList<>();

        for (String col:this.tabela.columnNames()) {
            if (col.split("__")[0].equals(this.nomeAtributoTarget))
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
