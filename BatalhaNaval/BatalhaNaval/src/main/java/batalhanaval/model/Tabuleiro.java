package batalhanaval.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Tabuleiro 10x10 da Batalha Naval.
 *
 * Células:
 *   ' ' = água/vazio
 *   'N' = navio (próprio tabuleiro)
 *   'X' = acerto
 *   'O' = água (tiro falhado)
 *   'A' = navio afundado
 */
public class Tabuleiro implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final int TAMANHO = 10;

    private final char[][] grelha;
    private final List<Navio> navios;

    public Tabuleiro() {
        grelha = new char[TAMANHO][TAMANHO];
        navios = new ArrayList<>();
        for (int i = 0; i < TAMANHO; i++)
            for (int j = 0; j < TAMANHO; j++)
                grelha[i][j] = ' ';
    }

    // ── Colocação de navios ───────────────────────────────────────────────────

    /**
     * Tenta colocar um navio. Devolve true se bem-sucedido.
     */
    public boolean colocarNavio(Navio navio) {
        // Verificar limites e sobreposição
        for (int[] pos : navio.getPosicoes()) {
            int l = pos[0], c = pos[1];
            if (l < 0 || l >= TAMANHO || c < 0 || c >= TAMANHO) return false;
            if (grelha[l][c] != ' ') return false;
        }
        // Colocar
        for (int[] pos : navio.getPosicoes()) {
            grelha[pos[0]][pos[1]] = 'N';
        }
        navios.add(navio);
        return true;
    }

    // ── Disparo ───────────────────────────────────────────────────────────────

    /**
     * Processa um tiro. Devolve o resultado como string.
     */
    public ResultadoTiro processarTiro(int linha, int coluna) {
        if (linha < 0 || linha >= TAMANHO || coluna < 0 || coluna >= TAMANHO)
            return new ResultadoTiro(TipoResultado.INVALIDO, null);

        char celula = grelha[linha][coluna];
        if (celula == 'X' || celula == 'O' || celula == 'A')
            return new ResultadoTiro(TipoResultado.JA_DISPARADO, null);

        for (Navio n : navios) {
            if (n.ocupa(linha, coluna)) {
                n.registarAcerto(linha, coluna);
                if (n.estaAfundado()) {
                    // Marcar todas as posições do navio como afundado
                    for (int[] pos : n.getPosicoes())
                        grelha[pos[0]][pos[1]] = 'A';
                    return new ResultadoTiro(TipoResultado.AFUNDOU, n.getTipo());
                } else {
                    grelha[linha][coluna] = 'X';
                    return new ResultadoTiro(TipoResultado.ACERTOU, n.getTipo());
                }
            }
        }
        grelha[linha][coluna] = 'O';
        return new ResultadoTiro(TipoResultado.AGUA, null);
    }

    /** Verifica se todos os navios estão afundados. */
    public boolean todosAfundados() {
        return navios.stream().allMatch(Navio::estaAfundado);
    }

    /** Número total de navios colocados. */
    public int getNumNavios() { return navios.size(); }

    public char[][] getGrelha() { return grelha; }
    public List<Navio> getNavios() { return navios; }

    // ── Validação da disposição ───────────────────────────────────────────────

    /**
     * Valida se os navios colocados respeitam as regras:
     * 4×Submarino(1), 3×Contratorpedeiro(2), 2×NavioGuerra(3), 1×Couraçado(4), 1×PortaAvioes(5)
     */
    public String validarDisposicao() {
        int[] contagem = new int[TipoNavio.values().length];
        for (Navio n : navios) contagem[n.getTipo().ordinal()]++;

        for (TipoNavio tipo : TipoNavio.values()) {
            if (contagem[tipo.ordinal()] != tipo.getQuantidade()) {
                return "Quantidade incorreta de " + tipo.getNome()
                        + ": esperado " + tipo.getQuantidade()
                        + ", tem " + contagem[tipo.ordinal()];
            }
        }
        return null; // OK
    }

    // ── Representação visual ──────────────────────────────────────────────────

    /**
     * Renderiza o tabuleiro do JOGADOR (navios visíveis).
     */
    public String renderizarProprio() {
        return renderizar(false);
    }

    /**
     * Renderiza o tabuleiro do ADVERSÁRIO (navios ocultos, só tiros).
     */
    public String renderizarAdversario() {
        return renderizar(true);
    }

    private String renderizar(boolean ocultarNavios) {
        StringBuilder sb = new StringBuilder();
        sb.append("   A B C D E F G H I J\n");
        sb.append("  +-------------------+\n");
        for (int i = 0; i < TAMANHO; i++) {
            sb.append(String.format("%2d|", i + 1));
            for (int j = 0; j < TAMANHO; j++) {
                char c = grelha[i][j];
                if (ocultarNavios && c == 'N') c = ' ';
                sb.append(c).append(' ');
            }
            sb.append("|\n");
        }
        sb.append("  +-------------------+\n");
        return sb.toString();
    }

    // ── Serialização simples (estado do jogo) ─────────────────────────────────

    public String serializar() {
        StringBuilder sb = new StringBuilder();
        // Grelha
        for (int i = 0; i < TAMANHO; i++) {
            for (int j = 0; j < TAMANHO; j++) sb.append(grelha[i][j]);
            sb.append('\n');
        }
        // Navios
        sb.append("NAVIOS:").append(navios.size()).append('\n');
        for (Navio n : navios) sb.append(n.serializar()).append('\n');
        return sb.toString();
    }

    public static Tabuleiro desserializar(String dados) {
        Tabuleiro t = new Tabuleiro();
        String[] linhas = dados.split("\n");
        for (int i = 0; i < TAMANHO; i++) {
            for (int j = 0; j < TAMANHO; j++) {
                t.grelha[i][j] = linhas[i].charAt(j);
            }
        }
        int numNavios = Integer.parseInt(linhas[TAMANHO].split(":")[1]);
        for (int k = 0; k < numNavios; k++) {
            Navio n = Navio.desserializar(linhas[TAMANHO + 1 + k]);
            t.navios.add(n);
        }
        return t;
    }
}
