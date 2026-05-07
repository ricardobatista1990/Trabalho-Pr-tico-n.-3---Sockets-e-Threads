package batalhanaval.model;

/**
 * Resultado de um tiro no tabuleiro.
 */
public class ResultadoTiro {

    private final TipoResultado tipo;
    private final TipoNavio navioAfetado;

    public ResultadoTiro(TipoResultado tipo, TipoNavio navioAfetado) {
        this.tipo = tipo;
        this.navioAfetado = navioAfetado;
    }

    public TipoResultado getTipo() { return tipo; }
    public TipoNavio getNavioAfetado() { return navioAfetado; }

    /** Mensagem legível para o protocolo. */
    public String getMensagem() {
        return switch (tipo) {
            case AGUA -> "Água";
            case ACERTOU -> "Acertou em " + navioAfetado.getNome();
            case AFUNDOU -> "Afundou " + navioAfetado.getNome();
            case JA_DISPARADO -> "Posição já disparada";
            case INVALIDO -> "Posição inválida";
        };
    }

    /** Serialização para protocolo: AGUA | ACERTOU|TipoNavio | AFUNDOU|TipoNavio */
    public String serializar() {
        return switch (tipo) {
            case AGUA -> "AGUA";
            case ACERTOU -> "ACERTOU|" + navioAfetado.name();
            case AFUNDOU -> "AFUNDOU|" + navioAfetado.name();
            default -> "INVALIDO";
        };
    }
}
