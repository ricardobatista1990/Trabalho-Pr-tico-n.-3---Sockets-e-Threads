package batalhanaval.server;

import batalhanaval.protocol.Protocolo;

import java.io.*;
import java.net.*;

/**
 * Thread no servidor que gere a comunicação com um cliente específico.
 */
public class ClienteHandler implements Runnable {

    private final Socket socket;
    private final int numJogador;
    private final GestorJogo gestor;

    private PrintWriter out;
    private BufferedReader in;
    private String nome;
    private boolean conectado = true;

    public ClienteHandler(Socket socket, int numJogador, GestorJogo gestor) throws IOException {
        this.socket = socket;
        this.numJogador = numJogador;
        this.gestor = gestor;
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        this.nome = "Jogador" + numJogador;
    }

    @Override
    public void run() {
        try {
            String mensagem;
            while (conectado && (mensagem = in.readLine()) != null) {
                processar(mensagem.trim());
            }
        } catch (IOException e) {
            if (conectado) {
                System.out.println("[Handler-J" + numJogador + "] Ligação perdida: " + e.getMessage());
            }
        } finally {
            desligar();
        }
    }

    private void processar(String mensagem) {
        if (mensagem.isEmpty()) return;

        String[] partes = Protocolo.desmontar(mensagem);
        String comando = partes[0];

        System.out.println("[J" + numJogador + " → Servidor] " + mensagem);

        switch (comando) {

            case Protocolo.NOVO_JOGO -> {
                if (partes.length > 1) gestor.registarNome(numJogador, partes[1]);
            }

            case Protocolo.COLOCAR_BARCOS -> {
                if (partes.length > 1) gestor.processarBarcos(numJogador, partes[1]);
                else enviar(Protocolo.montar(Protocolo.BARCOS_INVALIDOS, "Sem dados de barcos."));
            }

            case Protocolo.TIRO -> {
                if (partes.length >= 3) {
                    try {
                        int linha = Integer.parseInt(partes[1]);
                        int coluna = Integer.parseInt(partes[2]);
                        gestor.processarTiro(numJogador, linha, coluna);
                    } catch (NumberFormatException e) {
                        enviar(Protocolo.montar(Protocolo.ERRO, "Coordenadas inválidas."));
                    }
                }
            }

            case Protocolo.GUARDAR_JOGO -> gestor.guardarJogo(numJogador);

            case Protocolo.SAIR -> {
                conectado = false;
                gestor.tratarDesconexao(numJogador);
            }

            default -> System.out.println("[Handler-J" + numJogador + "] Comando desconhecido: " + comando);
        }
    }

    /** Envia uma mensagem ao cliente. Thread-safe. */
    public synchronized void enviar(String mensagem) {
        if (out != null && !socket.isClosed()) {
            System.out.println("[Servidor → J" + numJogador + "] " + mensagem);
            out.println(mensagem);
        }
    }

    private void desligar() {
        conectado = false;
        try {
            if (!socket.isClosed()) socket.close();
        } catch (IOException ignored) {}

        // Notificar gestor da desconexão se o jogo ainda estava a decorrer
        gestor.tratarDesconexao(numJogador);
    }

    public void setNome(String nome) { this.nome = nome; }
    public String getNome() { return nome; }
    public int getNumJogador() { return numJogador; }
}
