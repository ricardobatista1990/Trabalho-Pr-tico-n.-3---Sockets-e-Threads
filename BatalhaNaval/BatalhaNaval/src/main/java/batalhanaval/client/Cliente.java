package batalhanaval.client;

import batalhanaval.model.*;
import batalhanaval.protocol.Protocolo;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Cliente da Batalha Naval (CLI).
 *
 * Uso: java -cp BatalhaNaval-client.jar batalhanaval.client.Cliente [host] [porta]
 *
 * Usa duas threads:
 *  - Thread principal: lê input do utilizador
 *  - Thread de receção: lê mensagens do servidor
 */
public class Cliente {

    private static final String HOST_PADRAO = "localhost";
    private static final int PORTA_PADRAO = 12345;

    // Tabuleiros locais para visualização
    private final Tabuleiro meuTabuleiro;
    private final Tabuleiro tabuleirOAdversario;

    private PrintWriter out;
    private BufferedReader in;
    private final Scanner scanner;

    private boolean emJogo = true;
    private boolean meuTurno = false;
    private int tirosRestantes = 0;
    private String meuNome = "Jogador";
    private String nomeAdversario = "Adversário";

    public Cliente() {
        meuTabuleiro = new Tabuleiro();
        tabuleirOAdversario = new Tabuleiro();
        scanner = new Scanner(System.in);
    }

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : HOST_PADRAO;
        int porta = args.length > 1 ? Integer.parseInt(args[1]) : PORTA_PADRAO;

        new Cliente().ligar(host, porta);
    }

    public void ligar(String host, int porta) throws IOException {
        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║    BATALHA NAVAL - CLIENTE         ║");
        System.out.println("╚════════════════════════════════════╝");

        System.out.print("O teu nome: ");
        meuNome = scanner.nextLine().trim();
        if (meuNome.isEmpty()) meuNome = "Jogador";

        System.out.println("A ligar a " + host + ":" + porta + "...");

        try (Socket socket = new Socket(host, porta)) {
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            System.out.println("Ligado ao servidor!\n");

            // Enviar nome ao servidor
            out.println(Protocolo.montar(Protocolo.NOVO_JOGO, meuNome));

            // Thread de receção de mensagens do servidor
            Thread rececao = new Thread(this::receberMensagens, "Recetor");
            rececao.setDaemon(true);
            rececao.start();

            // Thread principal aguarda (o input é gerido pela thread de receção
            // que chama os métodos de input quando necessário)
            try { rececao.join(); } catch (InterruptedException ignored) {}
        } catch (ConnectException e) {
            System.out.println("Não foi possível ligar ao servidor. Verifique o host e porta.");
        }
    }

    /** Loop de receção de mensagens do servidor. */
    private void receberMensagens() {
        try {
            String mensagem;
            while (emJogo && (mensagem = in.readLine()) != null) {
                processar(mensagem.trim());
            }
        } catch (IOException e) {
            if (emJogo) System.out.println("\nLigação ao servidor perdida.");
        }
        emJogo = false;
        System.out.println("\n[Jogo terminado]");
    }

    private void processar(String mensagem) {
        String[] partes = Protocolo.desmontar(mensagem);
        String comando = partes[0];

        switch (comando) {

            case Protocolo.AGUARDAR ->
                System.out.println("A aguardar o segundo jogador...");

            case Protocolo.JOGO_INICIADO -> {
                nomeAdversario = partes.length > 1 ? partes[1] : "Adversário";
                System.out.println("Jogo iniciado! Adversário: " + nomeAdversario);
            }

            case Protocolo.COLOCAR_BARCOS_REQ -> pedirColocacaoBarcos();

            case Protocolo.BARCOS_INVALIDOS -> {
                String motivo = partes.length > 1 ? partes[1] : "Disposição inválida";
                System.out.println("\n❌ Barcos inválidos: " + motivo);
                pedirColocacaoBarcos();
            }

            case Protocolo.BARCOS_OK ->
                System.out.println("✓ Barcos aceites! A aguardar o adversário...");

            case "INFO" ->
                System.out.println("[Info] " + (partes.length > 1 ? partes[1] : ""));

            case Protocolo.TEU_TURNO -> {
                tirosRestantes = partes.length > 1 ? Integer.parseInt(partes[1]) : 3;
                meuTurno = true;
                mostrarTabuleiros();
                pedirTiro();
            }

            case Protocolo.AGUARDAR_TURNO -> {
                meuTurno = false;
                System.out.println("\n⏳ Turno de " + nomeAdversario + ". Aguarda...");
            }

            case Protocolo.RESULTADO_TIRO -> {
                // RESULTADO_TIRO|linha|coluna|AGUA | ACERTOU|TipoNavio | AFUNDOU|TipoNavio
                int linha = Integer.parseInt(partes[1]);
                int coluna = Integer.parseInt(partes[2]);
                String resultStr = partes[3];

                aplicarResultadoNoAdversario(linha, coluna, resultStr, partes);
                String descricao = descreverResultado(resultStr, partes);
                System.out.println("  → " + descricao);

                tirosRestantes--;
                if (tirosRestantes > 0) {
                    System.out.println("  Tiros restantes: " + tirosRestantes);
                }
            }

            case Protocolo.TIRO_RECEBIDO -> {
                int linha = Integer.parseInt(partes[1]);
                int coluna = Integer.parseInt(partes[2]);
                String resultStr = partes[3];

                aplicarResultadoNoProprioTabuleiro(linha, coluna, resultStr, partes);
                String descricao = descreverResultado(resultStr, partes);
                System.out.println("\n💥 " + nomeAdversario + " disparou em "
                        + coordParaStr(linha, coluna) + ": " + descricao);
            }

            case Protocolo.FIM_JOGO -> {
                String resultado = partes.length > 1 ? partes[1] : "";
                mostrarTabuleiros();
                switch (resultado) {
                    case Protocolo.VITORIA -> System.out.println("\n🏆 VITÓRIA! Parabéns, " + meuNome + "!");
                    case Protocolo.DERROTA -> System.out.println("\n💀 DERROTA. Boa sorte da próxima vez!");
                    case Protocolo.EMPATE  -> System.out.println("\n🤝 EMPATE!");
                }
                emJogo = false;
            }

            case Protocolo.JOGO_GUARDADO -> {
                String ficheiro = partes.length > 1 ? partes[1] : "desconhecido";
                System.out.println("\n💾 Jogo guardado em: " + ficheiro);
            }

            case Protocolo.ADVERSARIO_DESCONECTADO -> {
                System.out.println("\n⚠️  O adversário desligou-se. O jogo foi guardado.");
                emJogo = false;
            }

            case Protocolo.ERRO -> {
                String erro = partes.length > 1 ? partes[1] : "Erro desconhecido";
                System.out.println("\n⚠️  Erro: " + erro);
            }

            default -> System.out.println("[Servidor] " + mensagem);
        }
    }

    // ── Colocação de barcos ───────────────────────────────────────────────────

    private void pedirColocacaoBarcos() {
        // Reset tabuleiro local
        Tabuleiro tabTemp = new Tabuleiro();
        StringBuilder dadosBarcos = new StringBuilder();

        System.out.println("\n════════ COLOCAÇÃO DE BARCOS ════════");
        System.out.println("Navios a colocar:");
        for (TipoNavio tipo : TipoNavio.values()) {
            System.out.printf("  %dx %s (%d casa%s)%n",
                    tipo.getQuantidade(), tipo.getNome(),
                    tipo.getTamanho(), tipo.getTamanho() > 1 ? "s" : "");
        }
        System.out.println();
        System.out.println("Formato: linha coluna H/V  (ex: 3 5 H = linha 3, coluna 5, horizontal)");
        System.out.println("Colunas: A=0 B=1 C=2 D=3 E=4 F=5 G=6 H=7 I=8 J=9");
        System.out.println();

        for (TipoNavio tipo : TipoNavio.values()) {
            for (int k = 0; k < tipo.getQuantidade(); k++) {
                boolean colocado = false;
                while (!colocado) {
                    System.out.print(tipo.getNome() + " #" + (k + 1) + ": ");
                    System.out.println(tabTemp.renderizarProprio());
                    System.out.print("  > ");
                    String linha = scanner.nextLine().trim();
                    String[] tok = linha.split("\\s+");
                    if (tok.length < 3) {
                        System.out.println("Formato inválido. Use: linha coluna H/V");
                        continue;
                    }
                    try {
                        int l = Integer.parseInt(tok[0]) - 1; // 1-based para 0-based
                        int c = parseColuna(tok[1]);
                        boolean horiz = tok[2].equalsIgnoreCase("H");
                        Navio n = new Navio(tipo, l, c, horiz);
                        if (tabTemp.colocarNavio(n)) {
                            colocado = true;
                            if (dadosBarcos.length() > 0) dadosBarcos.append(";");
                            dadosBarcos.append(n.serializar());
                        } else {
                            System.out.println("Posição inválida ou sobreposição. Tenta novamente.");
                        }
                    } catch (Exception e) {
                        System.out.println("Entrada inválida: " + e.getMessage());
                    }
                }
            }
        }

        // Mostrar tabuleiro final
        System.out.println("\nTeu tabuleiro com barcos:");
        System.out.println(tabTemp.renderizarProprio());

        // Copiar navios para o tabuleiro local
        for (Navio n : tabTemp.getNavios()) meuTabuleiro.colocarNavio(n);

        // Enviar ao servidor
        out.println(Protocolo.montar(Protocolo.COLOCAR_BARCOS, dadosBarcos.toString()));
    }

    private int parseColuna(String s) {
        if (s.length() == 1 && Character.isLetter(s.charAt(0))) {
            return Character.toUpperCase(s.charAt(0)) - 'A';
        }
        return Integer.parseInt(s);
    }

    // ── Disparo ───────────────────────────────────────────────────────────────

    private void pedirTiro() {
        System.out.println("\n🎯 É o teu turno! Tiros restantes: " + tirosRestantes);
        System.out.println("Comandos: linha coluna  |  guardar  |  sair");
        System.out.print("  > ");

        String entrada = scanner.nextLine().trim();

        if (entrada.equalsIgnoreCase("guardar")) {
            out.println(Protocolo.GUARDAR_JOGO);
            return;
        }
        if (entrada.equalsIgnoreCase("sair")) {
            out.println(Protocolo.SAIR);
            emJogo = false;
            return;
        }

        String[] tok = entrada.split("\\s+");
        if (tok.length < 2) {
            System.out.println("Formato: linha coluna  (ex: 3 E  ou  3 4)");
            pedirTiro();
            return;
        }
        try {
            int linha = Integer.parseInt(tok[0]) - 1;
            int coluna = parseColuna(tok[1]);
            out.println(Protocolo.montar(Protocolo.TIRO, String.valueOf(linha), String.valueOf(coluna)));
        } catch (Exception e) {
            System.out.println("Entrada inválida. Use: linha coluna  (ex: 3 E)");
            pedirTiro();
        }
    }

    // ── Atualização visual ────────────────────────────────────────────────────

    private void aplicarResultadoNoAdversario(int linha, int coluna, String res, String[] partes) {
        char marca = res.startsWith("AGUA") ? 'O' : (res.startsWith("AFUNDOU") ? 'A' : 'X');
        tabuleirOAdversario.getGrelha()[linha][coluna] = marca;
    }

    private void aplicarResultadoNoProprioTabuleiro(int linha, int coluna, String res, String[] partes) {
        char marca = res.startsWith("AGUA") ? 'O' : (res.startsWith("AFUNDOU") ? 'A' : 'X');
        meuTabuleiro.getGrelha()[linha][coluna] = marca;
    }

    private String descreverResultado(String res, String[] partes) {
        if (res.equals("AGUA")) return "Água!";
        if (res.startsWith("ACERTOU")) {
            String tipoNavio = partes.length > 4 ? TipoNavio.valueOf(partes[4]).getNome() : "navio";
            return "Acertou em " + tipoNavio + "!";
        }
        if (res.startsWith("AFUNDOU")) {
            String tipoNavio = partes.length > 4 ? TipoNavio.valueOf(partes[4]).getNome() : "navio";
            return "Afundou " + tipoNavio + "! 🚢";
        }
        return res;
    }

    private void mostrarTabuleiros() {
        System.out.println("\n════════════════════════════════════════════");
        System.out.println("  MEU TABULEIRO (" + meuNome + ")");
        System.out.println(meuTabuleiro.renderizarProprio());
        System.out.println("  TABULEIRO DO ADVERSÁRIO (" + nomeAdversario + ")");
        System.out.println(tabuleirOAdversario.renderizarAdversario());
        System.out.println("  Legenda: N=Navio  X=Acerto  O=Água  A=Afundado");
        System.out.println("════════════════════════════════════════════\n");
    }

    private String coordParaStr(int linha, int coluna) {
        return (linha + 1) + "" + (char)('A' + coluna);
    }
}
