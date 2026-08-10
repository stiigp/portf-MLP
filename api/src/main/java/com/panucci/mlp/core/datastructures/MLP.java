package com.panucci.mlp.core.datastructures;

import com.panucci.mlp.core.util.ActivationFunction;
import com.panucci.mlp.core.util.ConfusionMatrix;
import com.panucci.mlp.core.util.MathUtil;
import com.panucci.mlp.dto.ConnectionSnapshot;
import com.panucci.mlp.dto.LayerTopology;
import com.panucci.mlp.dto.OutputValueSnapshot;
import com.panucci.mlp.dto.OutputValuesEvent;
import com.panucci.mlp.dto.TrainingEventOptions;
import com.panucci.mlp.dto.TrainingFinishedEvent;
import com.panucci.mlp.dto.TrainingProgressEvent;
import com.panucci.mlp.dto.TrainingStartedEvent;
import com.panucci.mlp.dto.WeightsUpdateEvent;
import com.panucci.mlp.listeners.TrainingListener;

import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MLP {
    protected List<EntradaMLP> entradas;
    protected List<Integer> saidasEsperadas;
    protected List<List<Perceptron>> camadasOcultas;
    protected List<Perceptron> camadaSaida;
    protected ActivationFunction activationFunction;
    protected ConfusionMatrix confusionMatrix;
    protected double taxaDeAprendizado;
    protected double erroRede;

    protected int nCamadasOcultas, nPerceptronsPorCamadaOculta;
    protected TrainingListener listener;
    protected String sessionId;
    protected TrainingEventOptions eventOptions;
    private long lastProgressEventMillis;
    private long lastWeightsEventMillis;

    public MLP(int nCamadasOcultas, ActivationFunction activationFunction, double taxaDeAprendizado, TrainingListener listener, String sessionId, TrainingEventOptions eventOptions) {
        this.nCamadasOcultas = nCamadasOcultas;
        this.activationFunction = activationFunction;
        this.camadasOcultas = new ArrayList<>();
        this.taxaDeAprendizado = taxaDeAprendizado;
        this.erroRede = Double.MAX_VALUE;
        this.listener = listener;
        this.sessionId = sessionId;
        this.eventOptions = TrainingEventOptions.normalize(eventOptions);
    }

    public void train(Table trainTable, String nomeAtributoTarget, double erroParada, int maxEpochs) {
        this.nPerceptronsPorCamadaOculta = numeroDePerceptronsMediaAritmetica(trainTable);

        this.instanciaCamadasRede(trainTable, nomeAtributoTarget);

        List<String> colunasTarget = retornaNomesDasColunasTarget(nomeAtributoTarget, trainTable);

        Table trainTableEntradas = trainTable.copy().removeColumns(colunasTarget.toArray(new String[0]));
        Table trainTableSaidas = trainTable.selectColumns(colunasTarget.toArray(new String[0]));

        List<List<Double>> entradas = converteTableEmListaDeListasDeDouble(trainTableEntradas);
        List<List<Integer>> saidas = converteTableEmListaDeListasDeInteger(trainTableSaidas);

        train(entradas, saidas, erroParada, maxEpochs);
    }

    protected void instanciaCamadasRede(Table tabela, String nomeAtributoTarget) {
        int nEntradas = retornaNEntradas(tabela, nomeAtributoTarget);
        this.instanciaAndPreencheEntradas(nEntradas);

        this.preenchePrimeiraCamadaOculta(this.nPerceptronsPorCamadaOculta);
        this.preencheRestoDasCamadasOcultas(this.nCamadasOcultas, this.nPerceptronsPorCamadaOculta);

        int nSaidas = retornaNSaidas(tabela, nomeAtributoTarget);
        this.instanciaAndPreencheCamadaSaida(nSaidas, nPerceptronsPorCamadaOculta);

        // inicializa as saídas esperadas todas zeradas, não está pronto para teste logo após ser instanciado
        this.saidasEsperadas = new ArrayList<>();
        for (int i = 0; i < nSaidas; i ++) {
            this.saidasEsperadas.add(0);
        }
    }

    private void train(List<List<Double>> entradasTreino, List<List<Integer>> saidasEsperadas, double erroParada, int maxEpochs) {
        int contadorEpochs = 0;
        List<Double> ultimos10Erros = new ArrayList<>();

        emitTrainingStarted(contadorEpochs, 0);
        while (this.erroRede > erroParada && contadorEpochs < maxEpochs) {
            assertTrainingNotInterrupted();

            for (int i = 0; i < entradasTreino.size(); i ++) {
                assertTrainingNotInterrupted();

                this.atualizaEntradasAndSaidasEsperadas(entradasTreino.get(i), saidasEsperadas.get(i));
                emitOutputValues(contadorEpochs, i);

                backPropagation();
                emitTrainingProgress(contadorEpochs, i);
                emitWeightsUpdate(contadorEpochs, i);
            }

            if (ultimos10Erros.size() < 10) {
                ultimos10Erros.add(this.erroRede);
            } else {
                if (MathUtil.desvioPadrao(ultimos10Erros) < erroParada * Math.pow(10, -1)) {
                    System.out.println("Plateau!");
                }

                ultimos10Erros.remove(0);
                ultimos10Erros.add(this.erroRede);
            }

            contadorEpochs++;
        }

        if (contadorEpochs == maxEpochs)
            System.out.println("Treinamento parou pelo limite de épocas!");

        emitTrainingFinished(contadorEpochs, 0);
    }

    public double test(Table testTable, String nomeAtributoTarget) {
        List<String> colunasTarget = retornaNomesDasColunasTarget(nomeAtributoTarget, testTable);
        this.confusionMatrix = new ConfusionMatrix(colunasTarget);

        Table testTableEntradas = testTable.copy().removeColumns(colunasTarget.toArray(new String[0]));
        Table testTableSaidas = testTable.selectColumns(colunasTarget.toArray(new String[0]));

        List<List<Double>> entradas = converteTableEmListaDeListasDeDouble(testTableEntradas);
        List<List<Integer>> saidas = converteTableEmListaDeListasDeInteger(testTableSaidas);

        if (this.activationFunction == ActivationFunction.tangenteHiperbolica) {
            // troca os 0's da saída esperada por -1
            for (List<Integer> saida:saidas) {
                for (int i = 0; i < saida.size(); i++) {
                    int valorSaida = saida.get(i);

                    if (valorSaida == 0)
                        saida.set(i, -1);
                }
            }
        }

        return testeMultiplo(entradas, saidas);
    }

    public double testeMultiplo(List<List<Double>> entradasTeste, List<List<Integer>> saidasTeste) {
        int nAcertos = 0;
        for (int i = 0; i < entradasTeste.size(); i ++) {
            if (testeUnico(entradasTeste.get(i), saidasTeste.get(i)))
                nAcertos ++;
        }

        // retorna a taxa de acerto
        return (double) nAcertos/entradasTeste.size();
    }

    public boolean testeUnico(List<Double> entradaTeste, List<Integer> saidaTeste) {
        atualizaEntradasAndSaidasEsperadas(entradaTeste, saidaTeste);

        int vencedor = retornaNeuronioVencedor();
        int vencedorEsperado = retornaVencedorEsperado();

        System.out.printf("Neurônio vencedor: %d\nVencedor esperado: %d\n", vencedor, vencedorEsperado);
        this.confusionMatrix.addResult(vencedor, vencedorEsperado);

        if (saidasEsperadas.get(vencedor) == 1) {
            System.out.println("Acerto!");
            return true;
        }

        System.out.println("Erro!");
        return false;
    }

    protected void backPropagation() {
        // atualiza os pesos com base nos resultados
        atualizaErrosCamadaSaida();
        atualizaErrosCamadasOcultas();
        atualizaPesosCamadaSaida();
        atualizaPesosCamadasOcultas();
        resetaNetAndSaidaDeTodosOsPerceptrons();
    }

    protected void atualizaErrosCamadaSaida() {
        this.erroRede = 0;
        for (int i = 0; i < camadaSaida.size(); i ++) {
            camadaSaida.get(i).geraErrosSaida(this.saidasEsperadas.get(i));
            this.erroRede += Math.pow(camadaSaida.get(i).getErro(), 2);
        }

        this.erroRede *= 0.5;
    }

    protected void atualizaErrosCamadasOcultas() {
        atualizaGradientesErrosUltimaCamadaOculta();
        atualizaGradientesErrosRestoDasCamadasOcultas();
    }

    protected void atualizaGradientesErrosUltimaCamadaOculta() {
        List<Perceptron> ultimaCamadaOculta = camadasOcultas.get(camadasOcultas.size()-1);
        for (int i = 0; i < ultimaCamadaOculta.size(); i ++) {
            double somatorio = 0;
            Perceptron perceptronAtual = ultimaCamadaOculta.get(i);
            for (int j = 0; j < camadaSaida.size(); j ++) {
                Perceptron saidaAtual = camadaSaida.get(j);
                somatorio += saidaAtual.getGradienteErro() * saidaAtual.getEntradas().get(i).getPeso();
            }

            perceptronAtual.setGradienteErro(somatorio * activationFunction.applyDeriv(perceptronAtual.getNet()));
        }
    }

    protected void atualizaGradientesErrosRestoDasCamadasOcultas() {
        for (int i = camadasOcultas.size() - 2; i >= 0; i --) {
            List<Perceptron> camadaOcultaAtual = camadasOcultas.get(i);
            List<Perceptron> proxCamadaOculta = camadasOcultas.get(i+1);

            for (int j = 0; j < camadaOcultaAtual.size(); j ++) {
                Perceptron perceptronAtual = camadaOcultaAtual.get(j);
                double somatorio = 0;

                for (int k = 0; k < proxCamadaOculta.size(); k ++) {
                    Perceptron perceptronDaProxCamada = proxCamadaOculta.get(k);
                    double pesoCorrespondente = perceptronDaProxCamada.getEntradas().get(j).getPeso();

                    somatorio += perceptronDaProxCamada.getGradienteErro() * pesoCorrespondente;
                }

                perceptronAtual.setGradienteErro(somatorio * activationFunction.applyDeriv(perceptronAtual.getNet()));
            }
        }
    }

    protected void atualizaPesosCamadaSaida() {
        for (int i = 0; i < camadaSaida.size(); i ++) {
            Perceptron saidaAtual = camadaSaida.get(i);
            List<EntradaPerceptron> entradasSaidaAtual = saidaAtual.getEntradas();

            for (int j = 0; j < entradasSaidaAtual.size(); j ++) {
                EntradaPerceptron entradaAtual = entradasSaidaAtual.get(j);
                double novoPeso = entradaAtual.getPeso() + (saidaAtual.getGradienteErro() * entradaAtual.getSaida() * taxaDeAprendizado);

                entradaAtual.atualizaPeso(novoPeso);
            }
        }
    }

    protected void atualizaPesosCamadasOcultas() {
        for (int i = camadasOcultas.size()-1; i >= 0; i --) {
            List<Perceptron> camadaOcultaAtual = camadasOcultas.get(i);

            for (int j = 0; j < camadaOcultaAtual.size(); j ++) {
                Perceptron perceptronAtual = camadaOcultaAtual.get(j);
                List<EntradaPerceptron> entradasCamadaOcultaAtual = perceptronAtual.getEntradas();

                for (int k = 0; k < entradasCamadaOcultaAtual.size(); k ++) {
                    EntradaPerceptron entradaAtual = entradasCamadaOcultaAtual.get(k);
                    double novoPeso = entradaAtual.getPeso() + (perceptronAtual.getGradienteErro() * entradaAtual.getSaida() * taxaDeAprendizado);

                    entradaAtual.atualizaPeso(novoPeso);
                }
            }

        }
    }

    public void atualizaEntradasAndSaidasEsperadas(List<Double> valoresEntrada, List<Integer> valoresSaida) {
        // atualiza as entradas da main.java.com.panucci.mlp.datastructures.MLP e reseta os valores net, saída e erro de cada perceptron
        for (int i = 0; i < this.entradas.size(); i ++) {
            EntradaMLP entradaMLP = this.entradas.get(i);

            entradaMLP.setValor(valoresEntrada.get(i));
        }

        for (int i = 0; i < this.saidasEsperadas.size(); i ++) {
            int saidaAtual = valoresSaida.get(i);

            this.saidasEsperadas.set(i, saidaAtual);
        }

        resetaNetAndSaidaDeTodosOsPerceptrons();
    }

    protected void instanciaAndPreencheEntradas(int nEntradas) {
        this.entradas = new ArrayList<>();
        for (int i = 0; i < nEntradas; i ++) {
            this.entradas.add(
                    new EntradaMLP(0)
            );
        }
    }

    protected void preenchePrimeiraCamadaOculta(int nPerceptronsPorCamadaOculta) {
        Random r = new Random();
        this.camadasOcultas.add(new ArrayList<>());
        for (int i = 0; i < nPerceptronsPorCamadaOculta; i ++) {
            List<EntradaPerceptron> entradasPrimeiraCamada = new ArrayList<>();
            for (EntradaMLP entrada: this.entradas) {
                entradasPrimeiraCamada.add(
                        // inicializa com pesos aleatórios
                        new EntradaPerceptron(entrada, r.nextDouble() - 0.5)
                );
            }
            this.camadasOcultas.get(0).add(new Perceptron(entradasPrimeiraCamada, activationFunction));
        }
    }

    protected void preencheRestoDasCamadasOcultas(int nCamadasOcultas, int nPerceptronsPorCamadaOculta) {
        Random r = new Random();

        for (int i = 1; i < nCamadasOcultas; i ++) {
            this.camadasOcultas.add(new ArrayList<>());
            for (int j = 0; j < nPerceptronsPorCamadaOculta; j ++) {
                List<EntradaPerceptron> entradas = new ArrayList<>();
                for (int k = 0; k < nPerceptronsPorCamadaOculta; k ++) {
                    entradas.add(new EntradaPerceptron(this.camadasOcultas.get(i-1).get(k), r.nextDouble()-0.5));
                }

                this.camadasOcultas.get(i).add(new Perceptron(entradas, this.activationFunction));
            }
        }
    }

    protected void instanciaAndPreencheCamadaSaida(int nSaidas, int nPerceptronsPorCamadaOculta) {
        this.camadaSaida = new ArrayList<>();
        Random r = new Random();
        List<Perceptron> ultimaCamadaOculta = this.camadasOcultas.get(this.camadasOcultas.size()-1);

        for (int i = 0; i < nSaidas; i ++) {
            List<EntradaPerceptron> entradas = new ArrayList<>();
            for (int j = 0; j < nPerceptronsPorCamadaOculta; j ++) {
                entradas.add(
                  new EntradaPerceptron(ultimaCamadaOculta.get(j), r.nextDouble() - 0.5)
                );
            }

            this.camadaSaida.add(new Perceptron(entradas, this.activationFunction));
        }
    }

    public void exibeSaidas() {
        for (Perceptron saida:camadaSaida) {
            System.out.println(saida.getSaida());
        }
    }

    protected void resetaNetAndSaidaDeTodosOsPerceptrons() {
        for (List<Perceptron> camadaOculta: camadasOcultas) {
            for (Perceptron perceptron: camadaOculta) {
                perceptron.resetaNetAndSaida();
            }
        }

        for (Perceptron saida: camadaSaida) {
            saida.resetaNetAndSaida();
        }
    }

    protected int retornaNeuronioVencedor() {
        double maior = Double.MIN_VALUE;
        int indiceMaior = -1;

        for (Perceptron saida: camadaSaida) {
            if (saida.getSaida() > maior) {
                maior = saida.getSaida();
                indiceMaior = camadaSaida.indexOf(saida);
            }
        }

        return indiceMaior;
    }

    protected int retornaVencedorEsperado() {
        for (int saidaEsperada:this.saidasEsperadas) {
            if (saidaEsperada == 1)
                return this.saidasEsperadas.indexOf(saidaEsperada);
        }

        return -1;
    }

    protected List<String> retornaNomesDasColunasTarget(String nomeAtributoTarget, Table tabela) {
        List<String> colunasTarget = new ArrayList<>();
        for (String coluna: tabela.columnNames()) {
            if (coluna.split("__")[0].equals(nomeAtributoTarget))
                colunasTarget.add(coluna);
        }

        return colunasTarget;
    }

    protected List<List<Integer>> converteTableEmListaDeListasDeInteger(Table tabela) {
        List<List<Integer>> retorno = new ArrayList<>();

        for (int i = 0; i < tabela.rowCount(); i ++) {
            List<Integer> listaAtual = new ArrayList<>();
            Row linhaAtual = tabela.row(i);

            for (int j = 0; j < linhaAtual.columnCount(); j ++)
                listaAtual.add(linhaAtual.getInt(j));

            retorno.add(listaAtual);
        }

        return retorno;
    }

    protected List<List<Double>> converteTableEmListaDeListasDeDouble(Table tabela) {
        List<List<Double>> retorno = new ArrayList<>();

        for (int i = 0; i < tabela.rowCount(); i ++) {
            List<Double> listaAtual = new ArrayList<>();
            Row linhaAtual = tabela.row(i);

            for (int j = 0; j < linhaAtual.columnCount(); j ++) {
                if (tabela.column(j).type() == ColumnType.DOUBLE)
                    listaAtual.add(linhaAtual.getDouble(j));
                else if (tabela.column(j).type() == ColumnType.FLOAT)
                    listaAtual.add((double)linhaAtual.getFloat(j));
                else
                    listaAtual.add((double)linhaAtual.getInt(j));
            }

            retorno.add(listaAtual);
        }

        return retorno;
    }

    protected int retornaNEntradas(Table tabela, String nomeAtributoTarget) {
        int nEntradas = 0;
        for (String coluna: tabela.columnNames()) {
            if (!coluna.split("__")[0].equals(nomeAtributoTarget))
                nEntradas++;
        }

        return nEntradas;
    }

    protected int retornaNSaidas(Table tabela, String nomeAtributoTarget) {
        int nSaidas = 0;
        for (String coluna: tabela.columnNames()) {
            if (coluna.split("__")[0].equals(nomeAtributoTarget))
                nSaidas++;
        }

        return nSaidas;
    }

    private int numeroDePerceptronsMediaAritmetica(Table tabela) {
        // a MLP recebe a tabela pós onehotenconding, portanto o número de entradas + número de classes
        // é simplesmente o número de colunas da tabela
        return tabela.columns().size() / 2;
    }

    private int numeroDePerceptronsMediaGeometrica(Table tabela) {
        return (int)Math.sqrt(numeroDeColunasTarget(tabela) * numeroDeColunasEntrada(tabela));
    }

    private int numeroDeColunasTarget(Table tabela) {
        int cont = 0;
        for (String name:tabela.columnNames()) {
            if (name.contains("__"))
                cont ++;
        }

        return cont;
    }

    private int numeroDeColunasEntrada(Table tabela) {
        int cont = 0;
        for (String name:tabela.columnNames()) {
            if (!name.contains("__"))
                cont ++;
        }

        return cont;
    }

    public void exibeConfusionMatrix() {
        this.confusionMatrix.print();
    }

    // auxiliary snapshot functions
    private void emitTrainingStarted(int epoch, int sampleIndex) {
        this.listener.onTrainingStartEvent(
            new TrainingStartedEvent(
                "TRAINING_STARTED",
                this.sessionId,
                epoch,
                sampleIndex,
                snapshotTopology()
            )
        );
    }

    private void emitOutputValues(int epoch, int sampleIndex) {
        if (!shouldEmitBySample(sampleIndex, this.eventOptions.outputSampleInterval())) {
            return;
        }

        this.listener.onForwardPassEvent(
            new OutputValuesEvent(
                "OUTPUT_VALUES",
                this.sessionId,
                epoch,
                sampleIndex,
                snapshotOutputValues()
            )
        );
    }

    private void emitTrainingProgress(int epoch, int sampleIndex) {
        long now = System.currentTimeMillis();
        if (
            !shouldEmitBySample(sampleIndex, this.eventOptions.progressSampleInterval())
                || !shouldEmitByTime(now, this.lastProgressEventMillis, this.eventOptions.progressMinMillis())
        ) {
            return;
        }

        this.lastProgressEventMillis = now;
        this.listener.onForwardPassEvent(
            new TrainingProgressEvent(
                "TRAINING_PROGRESS",
                this.sessionId,
                epoch,
                sampleIndex,
                this.erroRede
            )
        );
    }

    private void emitWeightsUpdate(int epoch, int sampleIndex) {
        long now = System.currentTimeMillis();
        if (
            !shouldEmitBySample(sampleIndex, this.eventOptions.weightsSampleInterval())
                || !shouldEmitByTime(now, this.lastWeightsEventMillis, this.eventOptions.weightsMinMillis())
        ) {
            return;
        }

        this.lastWeightsEventMillis = now;
        this.listener.onWeightsUpdateEvent(
            new WeightsUpdateEvent(
                "WEIGHTS_UPDATE",
                this.sessionId,
                epoch,
                sampleIndex,
                snapshotWeights()
            )
        );
    }

    private void emitTrainingFinished(int epoch, int sampleIndex) {
        this.listener.onTrainingEndEvent(
            new TrainingFinishedEvent(
                "TRAINING_FINISHED",
                this.sessionId,
                epoch,
                sampleIndex,
                this.erroRede
            )
        );
    }

    private boolean shouldEmitBySample(int sampleIndex, int interval) {
        return sampleIndex % interval == 0;
    }

    private boolean shouldEmitByTime(long now, long lastEventMillis, long minMillis) {
        return lastEventMillis == 0 || now - lastEventMillis >= minMillis;
    }

    private void assertTrainingNotInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new IllegalStateException("Training was interrupted");
        }
    }

    private List<LayerTopology> snapshotTopology() {
        List<LayerTopology> layers = new ArrayList<>();
        
        List<String> inputIds = new ArrayList<>();

        for (int i = 0; i < this.entradas.size(); i++) {
            inputIds.add(inputPerceptronId(i));
        }
        layers.add(new LayerTopology("input", 0, inputIds));

        for (int layerIndex = 0; layerIndex < this.camadasOcultas.size(); layerIndex++) {
            List<String> hiddenIds = new ArrayList<>();
            List<Perceptron> camada = this.camadasOcultas.get(layerIndex);

            for (int perceptronIndex = 0; perceptronIndex < camada.size(); perceptronIndex++) {
                hiddenIds.add(hiddenPerceptronId(layerIndex, perceptronIndex));
            }
            layers.add(new LayerTopology("hidden", layerIndex, hiddenIds));
        }

        List<String> outputIds = new ArrayList<>();
        for (int i = 0; i < this.camadaSaida.size(); i++) {
            outputIds.add(outputPerceptronId(i));
        }
        layers.add(new LayerTopology("output", 0, outputIds));

        return layers;
    }

    private List<OutputValueSnapshot> snapshotOutputValues() {
        List<OutputValueSnapshot> outputs = new ArrayList<>();

        for (int perceptronIndex = 0; perceptronIndex < this.camadaSaida.size(); perceptronIndex++) {
            Perceptron perceptron = this.camadaSaida.get(perceptronIndex);
            double output = perceptron.getSaida();

            outputs.add(
                new OutputValueSnapshot(
                    outputPerceptronId(perceptronIndex),
                    safeValue(perceptron.getNet()),
                    output,
                    this.saidasEsperadas.get(perceptronIndex)
                )
            );
        }

        return outputs;
    }

    private List<ConnectionSnapshot> snapshotWeights() {
        List<ConnectionSnapshot> connections = new ArrayList<>();

        for (int layerIndex = 0; layerIndex < this.camadasOcultas.size(); layerIndex++) {
            List<Perceptron> camada = this.camadasOcultas.get(layerIndex);

            for (int perceptronIndex = 0; perceptronIndex < camada.size(); perceptronIndex++) {
                Perceptron perceptron = camada.get(perceptronIndex);
                String destinationId = hiddenPerceptronId(layerIndex, perceptronIndex);
                addInputConnections(connections, perceptron, layerIndex, destinationId);
            }
        }

        for (int perceptronIndex = 0; perceptronIndex < this.camadaSaida.size(); perceptronIndex++) {
            Perceptron perceptron = this.camadaSaida.get(perceptronIndex);
            addInputConnections(connections, perceptron, this.camadasOcultas.size(), outputPerceptronId(perceptronIndex));
        }

        return connections;
    }

    private void addInputConnections(List<ConnectionSnapshot> connections, Perceptron perceptron, int destinationLayerIndex, String destinationId) {
        List<EntradaPerceptron> entradasPerceptron = perceptron.getEntradas();

        for (int inputIndex = 0; inputIndex < entradasPerceptron.size(); inputIndex++) {
            EntradaPerceptron entrada = entradasPerceptron.get(inputIndex);
            String sourceId = destinationLayerIndex == 0
                    ? inputPerceptronId(inputIndex)
                    : hiddenPerceptronId(destinationLayerIndex - 1, inputIndex);

            connections.add(new ConnectionSnapshot(sourceId, destinationId, entrada.getPeso()));
        }
    }

    private String inputPerceptronId(int index) {
        return "i-" + index;
    }

    private String hiddenPerceptronId(int layerIndex, int perceptronIndex) {
        return "h-" + layerIndex + "-" + perceptronIndex;
    }

    private String outputPerceptronId(int index) {
        return "o-" + index;
    }

    private Double safeValue(double value) {
        return value == Double.MAX_VALUE ? null : value;
    }

}
