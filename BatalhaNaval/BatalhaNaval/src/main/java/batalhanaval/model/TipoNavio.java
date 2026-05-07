package batalhanaval.model;

/**
 * Tipos de navios na Batalha Naval.
 * Regra: 4x1 casa, 3x2 casas, 2x3 casas, 1x4 casas, 1x5 casas.
 */
public enum TipoNavio {
    SUBMARINO(1, 4, "Submarino"),      // 4 navios de 1 casa
    CONTRATORPEDEIRO(2, 3, "Contratorpedeiro"), // 3 navios de 2 casas
    NAVIO_GUERRA(3, 2, "Navio de Guerra"),      // 2 navios de 3 casas
    COURAÇADO(4, 1, "Couraçado"),     // 1 navio de 4 casas
    PORTA_AVIOES(5, 1, "Porta-Aviões"); // 1 navio de 5 casas

    private final int tamanho;
    private final int quantidade;
    private final String nome;

    TipoNavio(int tamanho, int quantidade, String nome) {
        this.tamanho = tamanho;
        this.quantidade = quantidade;
        this.nome = nome;
    }

    public int getTamanho() { return tamanho; }
    public int getQuantidade() { return quantidade; }
    public String getNome() { return nome; }

    @Override
    public String toString() { return nome; }
}
