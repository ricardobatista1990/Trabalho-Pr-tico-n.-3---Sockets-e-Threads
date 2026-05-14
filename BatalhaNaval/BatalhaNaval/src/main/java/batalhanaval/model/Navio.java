package batalhanaval.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Representa um navio no tabuleiro.
 */
public class Navio implements Serializable {

    private static final long serialVersionUID = 1L;

    private final TipoNavio tipo;
    private final int linhaInicio;
    private final int colunaInicio;
    private final boolean horizontal; // true = horizontal, false = vertical
    private final Set<String> casasAcertadas;

    public Navio(TipoNavio tipo, int linhaInicio, int colunaInicio, boolean horizontal) {
        this.tipo = tipo;
        this.linhaInicio = linhaInicio;
        this.colunaInicio = colunaInicio;
        this.horizontal = horizontal;
        this.casasAcertadas = new HashSet<>();
    }

    /** Verifica se o navio ocupa a posição (linha, coluna). */
    public boolean ocupa(int linha, int coluna) {
        for (int i = 0; i < tipo.getTamanho(); i++) {
            int l = linhaInicio + (horizontal ? 0 : i);
            int c = colunaInicio + (horizontal ? i : 0);
            if (l == linha && c == coluna) return true;
        }
        return false;
    }

    /** Regista um acerto nesta posição. Devolve true se for nova posição. */
    public boolean registarAcerto(int linha, int coluna) {
        return casasAcertadas.add(linha + "," + coluna);
    }

    /** Verifica se o navio está afundado. */
    public boolean estaAfundado() {
        return casasAcertadas.size() >= tipo.getTamanho();
    }

    /** Devolve todas as posições (linha,coluna) que o navio ocupa. */
    public int[][] getPosicoes() {
        int[][] posicoes = new int[tipo.getTamanho()][2];
        for (int i = 0; i < tipo.getTamanho(); i++) {
            posicoes[i][0] = linhaInicio + (horizontal ? 0 : i);
            posicoes[i][1] = colunaInicio + (horizontal ? i : 0);
        }
        return posicoes;
    }

    public TipoNavio getTipo() { return tipo; }
    public int getLinhaInicio() { return linhaInicio; }
    public int getColunaInicio() { return colunaInicio; }
    public boolean isHorizontal() { return horizontal; }

    /** Serialização simples: TipoNavio,linha,coluna,horizontal (vírgula para não colidir com o separador do protocolo) */
    public String serializar() {
        return tipo.name() + "," + linhaInicio + "," + colunaInicio + "," + horizontal;
    }

    public static Navio desserializar(String dados) {
        String[] partes = dados.split(",");
        TipoNavio tipo = TipoNavio.valueOf(partes[0]);
        int linha = Integer.parseInt(partes[1]);
        int coluna = Integer.parseInt(partes[2]);
        boolean horizontal = Boolean.parseBoolean(partes[3]);
        return new Navio(tipo, linha, coluna, horizontal);
    }
}
