package batalhanaval.protocol;

/**
 * Protocolo de comunicação entre Servidor e Cliente.
 * Mensagens trocadas via Sockets (texto simples, separadas por "|").
 *
 * Formato geral: COMANDO|param1|param2|...
 */
public class Protocolo {

    // ── Mensagens do Cliente → Servidor ──────────────────────────────────────

    /** Cliente quer criar novo jogo: NOVO_JOGO|nomeJogador */
    public static final String NOVO_JOGO = "NOVO_JOGO";

    /** Cliente quer carregar jogo guardado: CARREGAR_JOGO|ficheiroEstado */
    public static final String CARREGAR_JOGO = "CARREGAR_JOGO";

    /** Cliente envia disposição dos barcos: COLOCAR_BARCOS|JSON_BARCOS */
    public static final String COLOCAR_BARCOS = "COLOCAR_BARCOS";

    /** Cliente dispara: TIRO|linha|coluna  (ex: TIRO|3|5) */
    public static final String TIRO = "TIRO";

    /** Cliente pede para guardar: GUARDAR_JOGO */
    public static final String GUARDAR_JOGO = "GUARDAR_JOGO";

    /** Cliente pede para sair: SAIR */
    public static final String SAIR = "SAIR";

    // ── Mensagens do Servidor → Cliente ──────────────────────────────────────

    /** Aguarda segundo jogador: AGUARDAR */
    public static final String AGUARDAR = "AGUARDAR";

    /** Jogo pronto a começar: JOGO_INICIADO|nomeAdversario|primeiroJogador */
    public static final String JOGO_INICIADO = "JOGO_INICIADO";

    /** Pede colocação de barcos: COLOCAR_BARCOS_REQ */
    public static final String COLOCAR_BARCOS_REQ = "COLOCAR_BARCOS_REQ";

    /** Barcos inválidos: BARCOS_INVALIDOS|motivo */
    public static final String BARCOS_INVALIDOS = "BARCOS_INVALIDOS";

    /** Barcos aceites: BARCOS_OK */
    public static final String BARCOS_OK = "BARCOS_OK";

    /** É o teu turno: TEU_TURNO|tirosRestantes */
    public static final String TEU_TURNO = "TEU_TURNO";

    /** Aguarda turno do adversário: AGUARDAR_TURNO */
    public static final String AGUARDAR_TURNO = "AGUARDAR_TURNO";

    /** Resultado de tiro próprio: RESULTADO_TIRO|linha|coluna|resultado */
    public static final String RESULTADO_TIRO = "RESULTADO_TIRO";

    /** Tiro recebido do adversário: TIRO_RECEBIDO|linha|coluna|resultado */
    public static final String TIRO_RECEBIDO = "TIRO_RECEBIDO";

    /** Resultados possíveis de tiro */
    public static final String AGUA = "Água";
    public static final String ACERTOU = "Acertou";  // Acertou|TipoNavio
    public static final String AFUNDOU = "Afundou";  // Afundou|TipoNavio

    /** Jogo terminado: FIM_JOGO|resultado  (VITORIA, DERROTA, EMPATE) */
    public static final String FIM_JOGO = "FIM_JOGO";
    public static final String VITORIA = "VITORIA";
    public static final String DERROTA = "DERROTA";
    public static final String EMPATE = "EMPATE";

    /** Jogo guardado com sucesso: JOGO_GUARDADO|ficheiro */
    public static final String JOGO_GUARDADO = "JOGO_GUARDADO";

    /** Erro genérico: ERRO|mensagem */
    public static final String ERRO = "ERRO";

    /** Adversário desligou-se: ADVERSARIO_DESCONECTADO */
    public static final String ADVERSARIO_DESCONECTADO = "ADVERSARIO_DESCONECTADO";

    // ── Utilitários ──────────────────────────────────────────────────────────

    public static String montar(String... partes) {
        return String.join("|", partes);
    }

    public static String[] desmontar(String mensagem) {
        return mensagem.split("\\|");
    }
}
