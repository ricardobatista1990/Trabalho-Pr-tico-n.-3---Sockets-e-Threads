package batalhanaval.client;

import batalhanaval.model.*;
import batalhanaval.protocol.Protocolo;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Cliente gráfico da Batalha Naval (Swing).
 * Mantém o mesmo protocolo de rede que o Cliente CLI.
 */
public class ClienteGUI extends JFrame {

    // ── Palette ───────────────────────────────────────────────────────────────
    static final Color BG       = new Color(10, 28, 58);
    static final Color BG_PANEL = new Color(15, 42, 82);
    static final Color BG_CARD  = new Color(20, 52, 98);
    static final Color ACCENT   = new Color(0, 155, 215);
    static final Color TEXT     = new Color(215, 230, 255);
    static final Color TEXT_DIM = new Color(130, 165, 205);
    static final Color SUCCESS  = new Color(55, 195, 95);
    static final Color DANGER   = new Color(215, 55, 55);
    static final Color WARN     = new Color(225, 175, 45);
    static final Color BORDER_C = new Color(35, 75, 135);

    // ── Network ───────────────────────────────────────────────────────────────
    private Socket activeSocket;
    private PrintWriter netOut;
    private volatile boolean emJogo = false;

    // ── Game state ────────────────────────────────────────────────────────────
    private final char[][] meuGrid  = freshGrid();
    private final char[][] advGrid  = freshGrid();
    private String meuNome          = "Jogador";
    private String nomeAdv          = "Adversário";
    private int tirosRestantes      = 0;
    private boolean meuTurno        = false;

    // ── Placement state ───────────────────────────────────────────────────────
    private final java.util.List<TipoNavio> filaNavios = new ArrayList<>();
    private int filaIdx = 0;
    private boolean placHoriz = true;
    private final char[][] placGrid = freshGrid();
    private final StringBuilder placDados = new StringBuilder();

    // ── UI ────────────────────────────────────────────────────────────────────
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private TabuleiroPanel tMeu, tAdv, tPlac;
    private JLabel turnoLbl, tirosLbl, navioAtualLbl, oriLbl;
    private JPanel naviosRestPanel, fimBotoesPanel;
    private JTextArea logArea;

    // ── Bootstrap ─────────────────────────────────────────────────────────────

    public ClienteGUI() {
        super("Batalha Naval");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(560, 500));
        getContentPane().setBackground(BG);

        root.setBackground(BG);
        add(root);

        root.add(panelConectar(), "CONECTAR");
        root.add(panelAguardar(), "AGUARDAR");
        root.add(panelColocacao(), "COLOCACAO");
        root.add(panelJogo(),     "JOGO");

        cards.show(root, "CONECTAR");
        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new ClienteGUI().setVisible(true);
        });
    }

    // =========================================================================
    // Panels
    // =========================================================================

    private JPanel panelConectar() {
        JPanel p = bgPanel(new GridBagLayout());
        p.setPreferredSize(new Dimension(460, 370));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(7, 12, 7, 12);
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = label("⚓  BATALHA NAVAL", Font.BOLD, 30);
        title.setForeground(ACCENT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        p.add(title, g);

        JLabel sub = label("Jogo Multijogador via Rede", Font.PLAIN, 13);
        sub.setForeground(TEXT_DIM);
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        g.gridy = 1;
        p.add(sub, g);

        g.gridy = 2; g.insets = new Insets(2, 30, 10, 30);
        p.add(separator(), g);
        g.insets = new Insets(7, 12, 7, 12);

        g.gridwidth = 1;
        JTextField nomeF = field("Jogador");
        JTextField hostF = field("localhost");
        JTextField portF = field("12345");

        g.gridy = 3; g.gridx = 0; p.add(label("Nome:", Font.PLAIN, 13), g);
        g.gridx = 1; p.add(nomeF, g);

        g.gridy = 4; g.gridx = 0; p.add(label("Servidor:", Font.PLAIN, 13), g);
        g.gridx = 1; p.add(hostF, g);

        g.gridy = 5; g.gridx = 0; p.add(label("Porta:", Font.PLAIN, 13), g);
        g.gridx = 1; p.add(portF, g);

        JButton btn = btn("LIGAR AO SERVIDOR", ACCENT);
        g.gridy = 6; g.gridx = 0; g.gridwidth = 2; g.insets = new Insets(14, 30, 6, 30);
        p.add(btn, g);

        JLabel err = label("", Font.PLAIN, 12);
        err.setHorizontalAlignment(SwingConstants.CENTER);
        g.gridy = 7; g.insets = new Insets(2, 12, 2, 12);
        p.add(err, g);

        btn.addActionListener(e -> {
            meuNome = nomeF.getText().trim().isEmpty() ? "Jogador" : nomeF.getText().trim();
            String host = hostF.getText().trim().isEmpty() ? "localhost" : hostF.getText().trim();
            int porta = 12345;
            try { porta = Integer.parseInt(portF.getText().trim()); } catch (NumberFormatException ignored) {}
            btn.setEnabled(false);
            err.setText("A ligar..."); err.setForeground(TEXT_DIM);
            final int fp = porta;
            new Thread(() -> ligar(host, fp, btn, err), "rede").start();
        });

        return p;
    }

    private JPanel panelAguardar() {
        JPanel p = bgPanel(new GridBagLayout());
        p.setPreferredSize(new Dimension(460, 200));
        JLabel lbl = label("⏳  A aguardar o segundo jogador...", Font.BOLD, 18);
        lbl.setForeground(TEXT);
        p.add(lbl);
        return p;
    }

    private JPanel panelColocacao() {
        JPanel p = bgPanel(new BorderLayout(12, 12));
        p.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel title = label("COLOCAR NAVIOS", Font.BOLD, 20);
        title.setForeground(ACCENT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setBorder(new EmptyBorder(0, 0, 8, 0));
        p.add(title, BorderLayout.NORTH);

        tPlac = new TabuleiroPanel(placGrid, false);
        tPlac.setClicavel(true);
        tPlac.setClickHandler(this::colocarNavio);
        p.add(tPlac, BorderLayout.CENTER);

        // Right sidebar
        JPanel side = bgPanel(null);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(BG_PANEL);
        side.setBorder(new CompoundBorder(new LineBorder(BORDER_C, 1, true), new EmptyBorder(14, 14, 14, 14)));
        side.setPreferredSize(new Dimension(195, 0));

        addSideTitle(side, "Navios a colocar:");
        side.add(Box.createVerticalStrut(8));

        naviosRestPanel = bgPanel(null);
        naviosRestPanel.setLayout(new BoxLayout(naviosRestPanel, BoxLayout.Y_AXIS));
        naviosRestPanel.setBackground(BG_PANEL);
        naviosRestPanel.setAlignmentX(LEFT_ALIGNMENT);
        side.add(naviosRestPanel);

        side.add(Box.createVerticalStrut(12));
        side.add(hSep());
        side.add(Box.createVerticalStrut(12));

        addSideTitle(side, "A colocar agora:");
        navioAtualLbl = label("—", Font.BOLD, 14);
        navioAtualLbl.setForeground(ACCENT);
        navioAtualLbl.setAlignmentX(LEFT_ALIGNMENT);
        side.add(navioAtualLbl);
        side.add(Box.createVerticalStrut(6));

        oriLbl = label("Horizontal ↔", Font.PLAIN, 12);
        oriLbl.setForeground(TEXT_DIM);
        oriLbl.setAlignmentX(LEFT_ALIGNMENT);
        side.add(oriLbl);
        side.add(Box.createVerticalStrut(8));

        JButton btnRotar = sideBtn("Rodar  [R]");
        JButton btnRand  = sideBtn("Aleatório");
        side.add(btnRotar);
        side.add(Box.createVerticalStrut(6));
        side.add(btnRand);
        side.add(Box.createVerticalGlue());

        side.add(hSep());
        side.add(Box.createVerticalStrut(8));
        JLabel hint = new JLabel("<html><font color='#82A8CC'>Clica no grid para<br>colocar. [R] roda.</font></html>");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hint.setAlignmentX(LEFT_ALIGNMENT);
        side.add(hint);

        p.add(side, BorderLayout.EAST);

        // Key R
        p.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('R'), "rot");
        p.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('r'), "rot");
        p.getActionMap().put("rot", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { rodar(); }
        });
        btnRotar.addActionListener(e -> rodar());
        btnRand.addActionListener(e -> colocacaoAleatoria());

        tPlac.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) { refreshPreview(); }
        });

        return p;
    }

    private JPanel panelJogo() {
        JPanel p = bgPanel(new BorderLayout(10, 10));
        p.setBorder(new EmptyBorder(12, 14, 12, 14));

        // Top bar
        JPanel topBar = bgPanel(new BorderLayout());
        topBar.setBackground(BG_PANEL);
        topBar.setBorder(new CompoundBorder(new LineBorder(BORDER_C, 1, true), new EmptyBorder(8, 14, 8, 14)));

        turnoLbl = label("A aguardar...", Font.BOLD, 15);
        turnoLbl.setForeground(TEXT);
        topBar.add(turnoLbl, BorderLayout.CENTER);

        // Painel direito: tiros restantes + botões de fim de jogo
        JPanel eastBar = bgPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        eastBar.setBackground(BG_PANEL);

        tirosLbl = label("", Font.BOLD, 13);
        tirosLbl.setForeground(WARN);
        eastBar.add(tirosLbl);

        fimBotoesPanel = bgPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        fimBotoesPanel.setBackground(BG_PANEL);
        fimBotoesPanel.setVisible(false);
        JButton btnVoltar = btn("↺  Voltar a Jogar", new Color(40, 130, 70));
        JButton btnSair   = btn("✕  Sair",            new Color(160, 35, 35));
        btnVoltar.setBorder(new EmptyBorder(5, 14, 5, 14));
        btnSair.setBorder(new EmptyBorder(5, 14, 5, 14));
        btnVoltar.addActionListener(e -> voltarAJogar());
        btnSair.addActionListener(e -> System.exit(0));
        fimBotoesPanel.add(btnVoltar);
        fimBotoesPanel.add(btnSair);
        eastBar.add(fimBotoesPanel);

        topBar.add(eastBar, BorderLayout.EAST);
        p.add(topBar, BorderLayout.NORTH);

        // Boards
        JPanel boards = bgPanel(new GridLayout(1, 2, 18, 0));

        tMeu = new TabuleiroPanel(meuGrid, false);
        JPanel myWrap = boardWrap("O MEU TABULEIRO", tMeu);
        boards.add(myWrap);

        tAdv = new TabuleiroPanel(advGrid, true);
        tAdv.setClickHandler(this::atirar);
        JPanel advWrap = boardWrap("TABULEIRO DO ADVERSÁRIO", tAdv);
        boards.add(advWrap);

        p.add(boards, BorderLayout.CENTER);

        // Log
        logArea = new JTextArea(5, 60);
        logArea.setEditable(false);
        logArea.setBackground(BG_PANEL);
        logArea.setForeground(TEXT_DIM);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBorder(new EmptyBorder(5, 8, 5, 8));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(new LineBorder(BORDER_C, 1));
        p.add(scroll, BorderLayout.SOUTH);

        return p;
    }

    // =========================================================================
    // Network
    // =========================================================================

    private void ligar(String host, int porta, JButton btn, JLabel err) {
        try {
            activeSocket = new Socket(host, porta);
            netOut = new PrintWriter(new OutputStreamWriter(activeSocket.getOutputStream(), "UTF-8"), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(activeSocket.getInputStream(), "UTF-8"));
            emJogo = true;

            netOut.println(Protocolo.montar(Protocolo.NOVO_JOGO, meuNome));

            SwingUtilities.invokeLater(() -> { cards.show(root, "AGUARDAR"); repack(); });

            String linha;
            while (emJogo && (linha = in.readLine()) != null) {
                final String msg = linha.trim();
                SwingUtilities.invokeLater(() -> processar(msg));
            }
        } catch (ConnectException ex) {
            SwingUtilities.invokeLater(() -> {
                err.setText("Não foi possível ligar ao servidor."); err.setForeground(DANGER);
                btn.setEnabled(true);
            });
        } catch (IOException ex) {
            if (emJogo) SwingUtilities.invokeLater(() -> log("Ligação perdida: " + ex.getMessage()));
        }
        emJogo = false;
    }

    private void processar(String msg) {
        String[] p = Protocolo.desmontar(msg);
        switch (p[0]) {
            case Protocolo.AGUARDAR          -> { cards.show(root, "AGUARDAR"); repack(); }
            case Protocolo.JOGO_INICIADO     -> { nomeAdv = p.length > 1 ? p[1] : "Adversário"; log("Adversário: " + nomeAdv); }
            case Protocolo.COLOCAR_BARCOS_REQ -> iniciarColocacao();
            case Protocolo.BARCOS_INVALIDOS  -> { JOptionPane.showMessageDialog(this, "Barcos inválidos: " + (p.length > 1 ? p[1] : ""), "Erro", JOptionPane.ERROR_MESSAGE); iniciarColocacao(); }
            case Protocolo.BARCOS_OK         -> { log("Barcos aceites! A aguardar adversário..."); turnoLbl.setText("A aguardar adversário..."); cards.show(root, "JOGO"); repack(); }
            case "INFO"                      -> log(p.length > 1 ? p[1] : "");
            case Protocolo.TEU_TURNO         -> onTeuTurno(p);
            case Protocolo.AGUARDAR_TURNO    -> onAguardarTurno();
            case Protocolo.RESULTADO_TIRO    -> onResultadoTiro(p);
            case Protocolo.TIRO_RECEBIDO     -> onTiroRecebido(p);
            case Protocolo.FIM_JOGO          -> onFimJogo(p);
            case Protocolo.ADVERSARIO_DESCONECTADO -> { log("Adversário desligou-se."); turnoLbl.setText("Adversário desligou-se."); turnoLbl.setForeground(DANGER); emJogo = false; }
            case Protocolo.JOGO_GUARDADO     -> log("Jogo guardado: " + (p.length > 1 ? p[1] : ""));
            case Protocolo.ERRO              -> log("Erro: " + (p.length > 1 ? p[1] : ""));
            default                          -> log("[srv] " + msg);
        }
    }

    // ── Turn handlers ─────────────────────────────────────────────────────────

    private void onTeuTurno(String[] p) {
        tirosRestantes = p.length > 1 ? Integer.parseInt(p[1]) : 3;
        meuTurno = true;
        tAdv.setClicavel(true);
        turnoLbl.setText("🎯  O TEU TURNO  —  " + meuNome);
        turnoLbl.setForeground(SUCCESS);
        tirosLbl.setText("Tiros: " + tirosRestantes + " / 3");
        cards.show(root, "JOGO");
    }

    private void onAguardarTurno() {
        meuTurno = false;
        tAdv.setClicavel(false);
        turnoLbl.setText("⏳  Turno de " + nomeAdv);
        turnoLbl.setForeground(WARN);
        tirosLbl.setText("");
    }

    private void onResultadoTiro(String[] p) {
        int l = Integer.parseInt(p[1]), c = Integer.parseInt(p[2]);
        char marca = marca(p[3]);
        advGrid[l][c] = marca;
        tAdv.repaint();
        tirosRestantes--;
        tirosLbl.setText("Tiros: " + tirosRestantes + " / 3");
        log("Disparaste em " + coord(l, c) + ": " + descrever(p));
    }

    private void onTiroRecebido(String[] p) {
        int l = Integer.parseInt(p[1]), c = Integer.parseInt(p[2]);
        meuGrid[l][c] = marca(p[3]);
        tMeu.repaint();
        log(nomeAdv + " disparou em " + coord(l, c) + ": " + descrever(p));
    }

    private void onFimJogo(String[] p) {
        String res = p.length > 1 ? p[1] : "";
        meuTurno = false; tAdv.setClicavel(false); emJogo = false;
        String txt; Color cor;
        if (res.equals(Protocolo.VITORIA)) { txt = "🏆  VITÓRIA!  Parabéns, " + meuNome + "!"; cor = SUCCESS; }
        else if (res.equals(Protocolo.DERROTA)) { txt = "💀  DERROTA.  Boa sorte da próxima vez!"; cor = DANGER; }
        else { txt = "🤝  EMPATE!"; cor = WARN; }
        turnoLbl.setText(txt); turnoLbl.setForeground(cor);
        tirosLbl.setText(""); log("--- " + txt + " ---");
        fimBotoesPanel.setVisible(true);
        topBar_revalidate();
    }

    private void topBar_revalidate() {
        // força o layout a recalcular para os botões aparecerem
        if (fimBotoesPanel != null) {
            fimBotoesPanel.getParent().revalidate();
            fimBotoesPanel.getParent().repaint();
        }
    }

    private void voltarAJogar() {
        emJogo = false;
        try { if (activeSocket != null && !activeSocket.isClosed()) activeSocket.close(); }
        catch (IOException ignored) {}

        for (char[] row : meuGrid) Arrays.fill(row, ' ');
        for (char[] row : advGrid)  Arrays.fill(row, ' ');
        meuTurno = false;
        tirosRestantes = 0;

        fimBotoesPanel.setVisible(false);
        turnoLbl.setText("A aguardar...");
        turnoLbl.setForeground(TEXT);
        tirosLbl.setText("");
        logArea.setText("");
        tMeu.repaint();
        tAdv.repaint();
        tAdv.setClicavel(false);

        cards.show(root, "CONECTAR");
        repack();
    }

    // =========================================================================
    // Placement
    // =========================================================================

    private void iniciarColocacao() {
        filaNavios.clear(); filaIdx = 0; placHoriz = true;
        placDados.setLength(0);
        for (int i = 0; i < 10; i++) for (int j = 0; j < 10; j++) placGrid[i][j] = ' ';

        // Bigger ships first for better UX
        java.util.List<TipoNavio> ordem = new ArrayList<>(Arrays.asList(TipoNavio.values()));
        Collections.reverse(ordem);
        for (TipoNavio t : ordem) for (int k = 0; k < t.getQuantidade(); k++) filaNavios.add(t);

        tPlac.setClicavel(true);
        refreshNaviosPanel();
        refreshNavioAtual();
        tPlac.repaint();
        cards.show(root, "COLOCACAO");
        repack();
    }

    private void refreshNaviosPanel() {
        naviosRestPanel.removeAll();
        // Count remaining per type
        Map<TipoNavio, Integer> rest = new LinkedHashMap<>();
        for (TipoNavio t : TipoNavio.values()) rest.put(t, 0);
        for (int i = filaIdx; i < filaNavios.size(); i++) rest.merge(filaNavios.get(i), 1, Integer::sum);

        for (TipoNavio tipo : new ArrayList<>(rest.keySet())) {
            int total = tipo.getQuantidade(), rem = rest.get(tipo);
            JPanel row = bgPanel(new BorderLayout(4, 0));
            row.setBackground(BG_PANEL);
            row.setAlignmentX(LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

            JPanel cells = bgPanel(new FlowLayout(FlowLayout.LEFT, 2, 3));
            cells.setBackground(BG_PANEL);
            for (int k = 0; k < tipo.getTamanho(); k++) {
                JPanel sq = new JPanel();
                sq.setPreferredSize(new Dimension(9, 9));
                sq.setBackground(rem > 0 ? new Color(65, 88, 108) : new Color(28, 48, 68));
                sq.setBorder(BorderFactory.createLineBorder(new Color(18, 38, 65), 1));
                cells.add(sq);
            }
            row.add(cells, BorderLayout.WEST);

            JLabel cnt = label(rem + "/" + total, Font.PLAIN, 11);
            cnt.setForeground(rem > 0 ? TEXT : TEXT_DIM);
            row.add(cnt, BorderLayout.EAST);

            naviosRestPanel.add(row);
            naviosRestPanel.add(Box.createVerticalStrut(2));
        }
        naviosRestPanel.revalidate();
        naviosRestPanel.repaint();
    }

    private void refreshNavioAtual() {
        if (filaIdx >= filaNavios.size()) return;
        TipoNavio t = filaNavios.get(filaIdx);
        navioAtualLbl.setText(t.getNome() + "  (" + t.getTamanho() + ")");
        oriLbl.setText(placHoriz ? "Horizontal ↔" : "Vertical ↕");
        tPlac.setPreview(t.getTamanho(), placHoriz);
    }

    private void rodar() {
        placHoriz = !placHoriz;
        if (filaIdx < filaNavios.size()) tPlac.setPreview(filaNavios.get(filaIdx).getTamanho(), placHoriz);
        refreshNavioAtual();
        refreshPreview();
    }

    private void refreshPreview() {
        if (filaIdx >= filaNavios.size()) return;
        int hl = tPlac.getHoverL(), hc = tPlac.getHoverC();
        if (hl < 0) return;
        TipoNavio t = filaNavios.get(filaIdx);
        tPlac.setPreviewOk(podeColocar(new Navio(t, hl, hc, placHoriz)));
        tPlac.repaint();
    }

    private boolean podeColocar(Navio n) {
        for (int[] pos : n.getPosicoes()) {
            int l = pos[0], c = pos[1];
            if (l < 0 || l >= 10 || c < 0 || c >= 10 || placGrid[l][c] != ' ') return false;
        }
        return true;
    }

    private void colocarNavio(int l, int c) {
        if (filaIdx >= filaNavios.size()) return;
        TipoNavio tipo = filaNavios.get(filaIdx);
        Navio n = new Navio(tipo, l, c, placHoriz);
        if (!podeColocar(n)) return;

        for (int[] pos : n.getPosicoes()) placGrid[pos[0]][pos[1]] = 'N';
        if (placDados.length() > 0) placDados.append(";");
        placDados.append(n.serializar());
        filaIdx++;

        if (filaIdx >= filaNavios.size()) confirmarColocacao();
        else { refreshNaviosPanel(); refreshNavioAtual(); refreshPreview(); }
        tPlac.repaint();
    }

    private void colocacaoAleatoria() {
        // Reset
        for (int i = 0; i < 10; i++) for (int j = 0; j < 10; j++) placGrid[i][j] = ' ';
        placDados.setLength(0); filaIdx = 0;
        Random rng = new Random();

        for (TipoNavio tipo : filaNavios) {
            boolean ok = false;
            for (int t = 0; t < 500 && !ok; t++) {
                Navio n = new Navio(tipo, rng.nextInt(10), rng.nextInt(10), rng.nextBoolean());
                if (podeColocar(n)) {
                    for (int[] pos : n.getPosicoes()) placGrid[pos[0]][pos[1]] = 'N';
                    if (placDados.length() > 0) placDados.append(";");
                    placDados.append(n.serializar());
                    filaIdx++; ok = true;
                }
            }
        }
        if (filaIdx == filaNavios.size()) {
            refreshNaviosPanel(); tPlac.clearPreview(); tPlac.repaint();
            confirmarColocacao();
        }
    }

    private void confirmarColocacao() {
        tPlac.clearPreview(); tPlac.setClicavel(false); tPlac.repaint();
        int r = JOptionPane.showConfirmDialog(this, "Confirmar disposição dos navios?",
            "Confirmar", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            for (int i = 0; i < 10; i++) for (int j = 0; j < 10; j++) meuGrid[i][j] = placGrid[i][j];
            netOut.println(Protocolo.montar(Protocolo.COLOCAR_BARCOS, placDados.toString()));
        } else {
            iniciarColocacao();
        }
    }

    // =========================================================================
    // Shooting
    // =========================================================================

    private void atirar(int l, int c) {
        if (!meuTurno || tirosRestantes <= 0 || advGrid[l][c] != ' ') return;
        netOut.println(Protocolo.montar(Protocolo.TIRO, String.valueOf(l), String.valueOf(c)));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private char marca(String res) {
        if (res.startsWith("AFUNDOU")) return 'A';
        if (res.startsWith("ACERTOU")) return 'X';
        return 'O';
    }

    private String descrever(String[] p) {
        String res = p[3];
        if (res.equals("AGUA")) return "Água!";
        String nomeNavio = p.length > 4 ? TipoNavio.valueOf(p[4]).getNome() : "navio";
        if (res.startsWith("ACERTOU")) return "Acertou em " + nomeNavio + "!";
        if (res.startsWith("AFUNDOU")) return "Afundou " + nomeNavio + "! 🚢";
        return res;
    }

    private String coord(int l, int c) { return (l + 1) + "" + (char) ('A' + c); }

    private void log(String msg) {
        if (logArea == null) return;
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void repack() {
        Dimension antes = getSize();
        pack();
        // se o utilizador já tinha aumentado a janela, manter o tamanho maior
        setSize(Math.max(getWidth(), antes.width), Math.max(getHeight(), antes.height));
        setLocationRelativeTo(null);
    }

    private static char[][] freshGrid() {
        char[][] g = new char[10][10];
        for (char[] row : g) Arrays.fill(row, ' ');
        return g;
    }

    // =========================================================================
    // UI factory
    // =========================================================================

    private JPanel bgPanel(LayoutManager lm) {
        JPanel p = lm != null ? new JPanel(lm) : new JPanel();
        p.setBackground(BG);
        return p;
    }

    private JLabel label(String text, int style, int size) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT);
        l.setFont(new Font("SansSerif", style, size));
        return l;
    }

    private JTextField field(String def) {
        JTextField tf = new JTextField(def, 14);
        tf.setBackground(new Color(18, 46, 88));
        tf.setForeground(TEXT);
        tf.setCaretColor(TEXT);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tf.setBorder(new CompoundBorder(new LineBorder(BORDER_C, 1, true), new EmptyBorder(5, 7, 5, 7)));
        return tf;
    }

    private JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(10, 18, 10, 18));
        b.addMouseListener(new MouseAdapter() {
            final Color orig = bg;
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(orig.brighter()); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(orig); }
        });
        return b;
    }

    private JButton sideBtn(String text) {
        JButton b = btn(text, new Color(42, 85, 148));
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setBorder(new EmptyBorder(7, 10, 7, 10));
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        return b;
    }

    private JSeparator separator() {
        JSeparator s = new JSeparator();
        s.setForeground(BORDER_C);
        return s;
    }

    private Component hSep() {
        JSeparator s = new JSeparator();
        s.setForeground(BORDER_C);
        s.setAlignmentX(LEFT_ALIGNMENT);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
    }

    private void addSideTitle(JPanel p, String text) {
        JLabel l = label(text, Font.BOLD, 13);
        l.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l);
    }

    private JPanel boardWrap(String title, TabuleiroPanel tab) {
        JPanel wrap = bgPanel(new BorderLayout(0, 6));
        JLabel lbl = label(title, Font.BOLD, 12);
        lbl.setForeground(TEXT_DIM);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        wrap.add(lbl, BorderLayout.NORTH);
        wrap.add(tab, BorderLayout.CENTER);
        return wrap;
    }
}