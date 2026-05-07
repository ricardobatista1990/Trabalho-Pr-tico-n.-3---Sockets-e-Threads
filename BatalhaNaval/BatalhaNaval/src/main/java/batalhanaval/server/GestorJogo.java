package batalhanaval.server;

import batalhanaval.model.*;
import batalhanaval.protocol.Protocolo;

import java.io.*;
import java.net.*;
import java.util.Random;

/**
 * Gestor central do jogo.
 * Coordena os dois handlers de clientes e arbitra as regras.
 */
public class GestorJogo {

    private final EstadoJogo estado;
    private final ServerSocket serverSocket;

    private ClienteHandler handler1;
    private ClienteHandler handler2;

    public GestorJogo(EstadoJogo estado, ServerSocket serverSocket) {
        this.estado = estado;
        this.serverSocket = serverSocket;
    }

    /** Aceita os dois jogadores e arranca o jogo. */
    public void iniciar() throws IOException {
        // Aceitar jogador 1
        System.out.println("Aguardando jogador 1...");
        Socket socket1 = serverSocket.accept();
        handler1 = new ClienteHandler(socket1, 1, this);
        System.out.println("Jogador 1 ligado: " + socket1.getInetAddress());

        // Notificar J1 que aguarda J2
        handler1.enviar(Protocolo.AGUARDAR);

        // Aceitar jogador 2
        System.out.println("Aguardando jogador 2...");
        Socket socket2 = serverSocket.accept();
        handler2 = new ClienteHandler(socket2, 2, this);
        System.out.println("Jogador 2 ligado: " + socket2.getInetAddress());

        // Iniciar threads de leitura
        Thread t1 = new Thread(handler1, "Handler-J1");
        Thread t2 = new Thread(handler2, "Handler-J2");
        t1.setDaemon(false);
        t2.setDaemon(false);
        t1.start();
        t2.start();

        // Fase de colocação de barcos
        estado.setFase(FaseJogo.COLOCAR_BARCOS);
        String nome1 = estado.getNomeJogador1() != null ? estado.getNomeJogador1() : "Jogador1";
        String nome2 = estado.getNomeJogador2() != null ? estado.getNomeJogador2() : "Jogador2";

        // Se jogo carregado, restaurar nomes dos handlers
        if (estado.getNomeJogador1() != null) {
            handler1.setNome(estado.getNomeJogador1());
            handler2.setNome(estado.getNomeJogador2());
        }

        handler1.enviar(Protocolo.montar(Protocolo.JOGO_INICIADO, nome2, ""));
        handler2.enviar(Protocolo.montar(Protocolo.JOGO_INICIADO, nome1, ""));

        // Pedir colocação de barcos a ambos
        handler1.enviar(Protocolo.COLOCAR_BARCOS_REQ);
        handler2.enviar(Protocolo.COLOCAR_BARCOS_REQ);

        System.out.println("Aguardando colocação de barcos...");

        try { t1.join(); t2.join(); } catch (InterruptedException ignored) {}
    }

    /** Chamado quando um jogador envia a disposição dos barcos. */
    public synchronized void processarBarcos(int numJogador, String dadosBarcos) {
        Tabuleiro tab = estado.getTabuleiro(numJogador);
        ClienteHandler handler = getHandler(numJogador);

        // Parsear barcos: cada linha = TipoNavio|linha|coluna|horizontal
        String[] linhas = dadosBarcos.split(";");
        for (String linha : linhas) {
            if (linha.isBlank()) continue;
            try {
                Navio n = Navio.desserializar(linha.trim());
                if (!tab.colocarNavio(n)) {
                    handler.enviar(Protocolo.montar(Protocolo.BARCOS_INVALIDOS,
                            "Posição inválida ou sobreposição para " + n.getTipo().getNome()));
                    return;
                }
            } catch (Exception e) {
                handler.enviar(Protocolo.montar(Protocolo.BARCOS_INVALIDOS, "Formato inválido: " + e.getMessage()));
                return;
            }
        }

        // Validar quantidades
        String erroValidacao = tab.validarDisposicao();
        if (erroValidacao != null) {
            tab.getNavios().clear();
            // Reset grelha
            char[][] g = tab.getGrelha();
            for (int i = 0; i < Tabuleiro.TAMANHO; i++)
                for (int j = 0; j < Tabuleiro.TAMANHO; j++) g[i][j] = ' ';
            handler.enviar(Protocolo.montar(Protocolo.BARCOS_INVALIDOS, erroValidacao));
            return;
        }

        handler.enviar(Protocolo.BARCOS_OK);

        if (numJogador == 1) estado.setBarcos1Prontos(true);
        else estado.setBarcos2Prontos(true);

        System.out.println("Barcos do jogador " + numJogador + " prontos.");

        // Quando ambos prontos, sortear e iniciar
        if (estado.todosBarcosColocados()) {
            iniciarFaseJogo();
        }
    }

    /** Sorteia o jogador inicial e começa a fase de jogo. */
    private synchronized void iniciarFaseJogo() {
        estado.setFase(FaseJogo.EM_JOGO);

        int primeiro = new Random().nextInt(2) + 1;
        estado.setJogadorAtual(primeiro);

        String nomePrimeiro = primeiro == 1
                ? estado.getNomeJogador1() : estado.getNomeJogador2();

        System.out.println("Jogo iniciado! Primeiro jogador: J" + primeiro + " (" + nomePrimeiro + ")");

        // Notificar ambos
        handler1.enviar(Protocolo.montar("INFO", "Primeiro a jogar: " + nomePrimeiro));
        handler2.enviar(Protocolo.montar("INFO", "Primeiro a jogar: " + nomePrimeiro));

        // Notificar quem joga
        notificarTurno();
    }

    /** Notifica qual jogador deve jogar agora. */
    public synchronized void notificarTurno() {
        int atual = estado.getJogadorAtual();
        int adversario = estado.getAdversario(atual);

        getHandler(atual).enviar(Protocolo.montar(Protocolo.TEU_TURNO,
                String.valueOf(estado.getTirosRestantes())));
        getHandler(adversario).enviar(Protocolo.AGUARDAR_TURNO);
    }

    /** Processa um tiro enviado por um jogador. */
    public synchronized void processarTiro(int numJogador, int linha, int coluna) {
        if (estado.getFase() != FaseJogo.EM_JOGO) return;
        if (estado.getJogadorAtual() != numJogador) {
            getHandler(numJogador).enviar(Protocolo.montar(Protocolo.ERRO, "Não é o seu turno."));
            return;
        }

        int adversario = estado.getAdversario(numJogador);
        Tabuleiro tabAdversario = estado.getTabuleiro(adversario);

        ResultadoTiro resultado = tabAdversario.processarTiro(linha, coluna);

        if (resultado.getTipo() == TipoResultado.INVALIDO ||
            resultado.getTipo() == TipoResultado.JA_DISPARADO) {
            getHandler(numJogador).enviar(Protocolo.montar(Protocolo.ERRO, resultado.getMensagem()));
            return;
        }

        estado.incrementarTiros();

        // Notificar ambos do resultado
        String resultadoSerial = resultado.serializar();
        getHandler(numJogador).enviar(Protocolo.montar(Protocolo.RESULTADO_TIRO,
                String.valueOf(linha), String.valueOf(coluna), resultadoSerial));
        getHandler(adversario).enviar(Protocolo.montar(Protocolo.TIRO_RECEBIDO,
                String.valueOf(linha), String.valueOf(coluna), resultadoSerial));

        System.out.printf("[J%d] Tiro (%d,%d): %s%n", numJogador, linha, coluna, resultado.getMensagem());

        // Verificar fim do jogo
        if (tabAdversario.todosAfundados()) {
            terminarJogo(numJogador);
            return;
        }

        // Trocar turno se completou os 3 tiros
        if (estado.turnoCompleto()) {
            estado.trocarTurno();
            notificarTurno();
        } else {
            // Ainda tem tiros no turno
            getHandler(numJogador).enviar(Protocolo.montar(Protocolo.TEU_TURNO,
                    String.valueOf(estado.getTirosRestantes())));
        }
    }

    /** Termina o jogo com um vencedor. */
    private void terminarJogo(int vencedor) {
        estado.setFase(FaseJogo.TERMINADO);
        int perdedor = estado.getAdversario(vencedor);

        System.out.println("Jogo terminado! Vencedor: J" + vencedor);

        getHandler(vencedor).enviar(Protocolo.montar(Protocolo.FIM_JOGO, Protocolo.VITORIA));
        getHandler(perdedor).enviar(Protocolo.montar(Protocolo.FIM_JOGO, Protocolo.DERROTA));
    }

    /** Guarda o estado do jogo (a pedido ou em caso de desconexão). */
    public synchronized void guardarJogo(int solicitante) {
        try {
            String ficheiro = estado.guardar();
            getHandler(solicitante).enviar(Protocolo.montar(Protocolo.JOGO_GUARDADO, ficheiro));
            int outro = estado.getAdversario(solicitante);
            if (getHandler(outro) != null)
                getHandler(outro).enviar(Protocolo.montar("INFO", "Jogo guardado: " + ficheiro));
        } catch (IOException e) {
            getHandler(solicitante).enviar(Protocolo.montar(Protocolo.ERRO, "Erro ao guardar: " + e.getMessage()));
        }
    }

    /** Chamado quando um jogador se desconecta inesperadamente. */
    public synchronized void tratarDesconexao(int numJogador) {
        System.out.println("[Servidor] Jogador " + numJogador + " desconectou-se.");

        // Guardar estado automaticamente
        if (estado.getFase() == FaseJogo.EM_JOGO || estado.getFase() == FaseJogo.COLOCAR_BARCOS) {
            try {
                String ficheiro = estado.guardar();
                System.out.println("[Servidor] Estado guardado automaticamente: " + ficheiro);
            } catch (IOException e) {
                System.out.println("[Servidor] Erro ao guardar automaticamente: " + e.getMessage());
            }
        }

        // Notificar o outro jogador
        int outro = estado.getAdversario(numJogador);
        ClienteHandler outroHandler = getHandler(outro);
        if (outroHandler != null) {
            outroHandler.enviar(Protocolo.ADVERSARIO_DESCONECTADO);
        }

        estado.setFase(FaseJogo.TERMINADO);
    }

    /** Define o nome de um jogador. */
    public synchronized void registarNome(int numJogador, String nome) {
        if (numJogador == 1) estado.setNomeJogador1(nome);
        else estado.setNomeJogador2(nome);
        getHandler(numJogador).setNome(nome);
        System.out.println("Jogador " + numJogador + " identificado como: " + nome);
    }

    private ClienteHandler getHandler(int num) {
        return num == 1 ? handler1 : handler2;
    }

    public EstadoJogo getEstado() { return estado; }
}
