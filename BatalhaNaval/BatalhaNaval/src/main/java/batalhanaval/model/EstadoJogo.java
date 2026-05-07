package batalhanaval.model;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Estado completo de um jogo em curso.
 * Pode ser guardado/carregado de ficheiro.
 */
public class EstadoJogo implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String PASTA_SAVES = "saves/";
    private static final String DELIMITADOR = "===";

    // Jogadores
    private String nomeJogador1;
    private String nomeJogador2;

    // Tabuleiros
    private Tabuleiro tabuleiro1; // tabuleiro do jogador 1
    private Tabuleiro tabuleiro2; // tabuleiro do jogador 2

    // Tiros do adversário (o que o jogador vê do adversário)
    private Tabuleiro tirosSobreJogador1; // tiros do J2 sobre J1
    private Tabuleiro tirosSobreJogador2; // tiros do J1 sobre J2

    // Estado do turno
    private int jogadorAtual; // 1 ou 2
    private int tirosNoTurnoAtual;
    public static final int TIROS_POR_TURNO = 3;

    // Fase do jogo
    private FaseJogo fase;

    // Controlo de barcos prontos
    private boolean barcos1Prontos;
    private boolean barcos2Prontos;

    public EstadoJogo() {
        tabuleiro1 = new Tabuleiro();
        tabuleiro2 = new Tabuleiro();
        tirosSobreJogador1 = new Tabuleiro();
        tirosSobreJogador2 = new Tabuleiro();
        fase = FaseJogo.AGUARDAR_JOGADORES;
        barcos1Prontos = false;
        barcos2Prontos = false;
        tirosNoTurnoAtual = 0;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getNomeJogador1() { return nomeJogador1; }
    public void setNomeJogador1(String nome) { this.nomeJogador1 = nome; }

    public String getNomeJogador2() { return nomeJogador2; }
    public void setNomeJogador2(String nome) { this.nomeJogador2 = nome; }

    public Tabuleiro getTabuleiro(int jogador) {
        return jogador == 1 ? tabuleiro1 : tabuleiro2;
    }

    public Tabuleiro getTirosSobre(int jogador) {
        return jogador == 1 ? tirosSobreJogador1 : tirosSobreJogador2;
    }

    public int getJogadorAtual() { return jogadorAtual; }
    public void setJogadorAtual(int j) { this.jogadorAtual = j; }

    public int getTirosNoTurnoAtual() { return tirosNoTurnoAtual; }
    public void incrementarTiros() { tirosNoTurnoAtual++; }
    public void resetTiros() { tirosNoTurnoAtual = 0; }
    public boolean turnoCompleto() { return tirosNoTurnoAtual >= TIROS_POR_TURNO; }
    public int getTirosRestantes() { return TIROS_POR_TURNO - tirosNoTurnoAtual; }

    public FaseJogo getFase() { return fase; }
    public void setFase(FaseJogo fase) { this.fase = fase; }

    public boolean isBarcos1Prontos() { return barcos1Prontos; }
    public void setBarcos1Prontos(boolean b) { this.barcos1Prontos = b; }

    public boolean isBarcos2Prontos() { return barcos2Prontos; }
    public void setBarcos2Prontos(boolean b) { this.barcos2Prontos = b; }

    public boolean todosBarcosColocados() { return barcos1Prontos && barcos2Prontos; }

    /** Troca o turno para o outro jogador. */
    public void trocarTurno() {
        jogadorAtual = (jogadorAtual == 1) ? 2 : 1;
        tirosNoTurnoAtual = 0;
    }

    /** Número do jogador adversário. */
    public int getAdversario(int jogador) { return jogador == 1 ? 2 : 1; }

    // ── Persistência ──────────────────────────────────────────────────────────

    /**
     * Guarda o estado em ficheiro. Devolve o nome do ficheiro gerado.
     */
    public String guardar() throws IOException {
        Files.createDirectories(Paths.get(PASTA_SAVES));
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String ficheiro = PASTA_SAVES + "jogo_" + timestamp + ".bns";

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(ficheiro))) {
            oos.writeObject(this);
        }
        System.out.println("[Servidor] Jogo guardado em: " + ficheiro);
        return ficheiro;
    }

    /**
     * Carrega o estado de um ficheiro.
     */
    public static EstadoJogo carregar(String ficheiro) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(ficheiro))) {
            return (EstadoJogo) ois.readObject();
        }
    }

    /**
     * Lista todos os jogos guardados disponíveis.
     */
    public static String[] listarGuardados() {
        File pasta = new File(PASTA_SAVES);
        if (!pasta.exists()) return new String[0];
        File[] ficheiros = pasta.listFiles((d, n) -> n.endsWith(".bns"));
        if (ficheiros == null) return new String[0];
        String[] nomes = new String[ficheiros.length];
        for (int i = 0; i < ficheiros.length; i++) nomes[i] = ficheiros[i].getPath();
        return nomes;
    }
}
