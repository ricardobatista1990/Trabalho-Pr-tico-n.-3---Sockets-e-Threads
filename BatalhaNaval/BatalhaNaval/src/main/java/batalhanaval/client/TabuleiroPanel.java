package batalhanaval.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.BiConsumer;

/**
 * Painel gráfico de um tabuleiro 10×10.
 * O tamanho da célula é calculado dinamicamente a partir do espaço disponível,
 * pelo que o painel escala correctamente ao redimensionar a janela.
 */
public class TabuleiroPanel extends JPanel {

    // Tamanho preferido/mínimo da célula (usado no preferred size inicial)
    private static final int CELL_PREF = 38;
    private static final int CELL_MIN  = 20;
    private static final int HEADER    = 22;
    private static final int PAD       = 6;

    private static final Color COR_AGUA       = new Color(25, 75, 140);
    private static final Color COR_AGUA_HOVER = new Color(40, 110, 190);
    private static final Color COR_NAVIO      = new Color(65, 88, 108);
    private static final Color COR_ACERTO     = new Color(210, 45, 45);
    private static final Color COR_MISS       = new Color(170, 195, 220);
    private static final Color COR_AFUNDADO   = new Color(130, 15, 15);
    private static final Color COR_PREV_OK    = new Color(80, 200, 80, 160);
    private static final Color COR_PREV_NOK   = new Color(220, 60, 60, 160);
    private static final Color COR_GRID       = new Color(15, 45, 95);
    private static final Color COR_HDR_TEXT   = new Color(160, 200, 240);

    private final char[][] grelha;
    private final boolean ocultarNavios;

    private BiConsumer<Integer, Integer> clickHandler;
    private boolean clicavel = false;

    private int hoverL = -1;
    private int hoverC = -1;
    private int previewTam = 0;
    private boolean previewH = true;
    private boolean previewOk = false;

    public TabuleiroPanel(char[][] grelha, boolean ocultarNavios) {
        this.grelha = grelha;
        this.ocultarNavios = ocultarNavios;

        int prefSide = PAD + HEADER + 10 * CELL_PREF + PAD;
        int minSide  = PAD + HEADER + 10 * CELL_MIN  + PAD;
        setPreferredSize(new Dimension(prefSide, prefSide));
        setMinimumSize(new Dimension(minSide, minSide));
        setBackground(new Color(12, 35, 70));

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int[] c = pixelToCell(e.getX(), e.getY());
                hoverL = c != null ? c[0] : -1;
                hoverC = c != null ? c[1] : -1;
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!clicavel || clickHandler == null) return;
                int[] c = pixelToCell(e.getX(), e.getY());
                if (c != null) clickHandler.accept(c[0], c[1]);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                hoverL = -1; hoverC = -1; repaint();
            }
        });
    }

    /** Tamanho de célula calculado dinamicamente a partir do espaço disponível. */
    private int cellSize() {
        int availW = getWidth()  - 2 * PAD - HEADER;
        int availH = getHeight() - 2 * PAD - HEADER;
        if (availW <= 0 || availH <= 0) return CELL_PREF;
        return Math.max(CELL_MIN, Math.min(availW, availH) / 10);
    }

    private int[] pixelToCell(int x, int y) {
        int cs = cellSize();
        int ox = PAD + HEADER, oy = PAD + HEADER;
        if (x < ox || y < oy) return null;
        int col = (x - ox) / cs;
        int row = (y - oy) / cs;
        if (col >= 0 && col < 10 && row >= 0 && row < 10) return new int[]{row, col};
        return null;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int cs = cellSize();
        int ox = PAD + HEADER, oy = PAD + HEADER;
        int fontSize = Math.max(9, Math.min(13, cs / 3));

        g.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        g.setColor(COR_HDR_TEXT);

        // Cabeçalho das colunas (A–J)
        for (int j = 0; j < 10; j++) {
            String s = String.valueOf((char) ('A' + j));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(s, ox + j * cs + (cs - fm.stringWidth(s)) / 2, PAD + HEADER - 5);
        }
        // Cabeçalho das linhas (1–10)
        for (int i = 0; i < 10; i++) {
            String s = String.valueOf(i + 1);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(s, PAD + (HEADER - fm.stringWidth(s)) / 2,
                         oy + i * cs + (cs + fm.getAscent()) / 2 - 2);
        }

        // Células
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                int cx = ox + j * cs, cy = oy + i * cs;
                char cell = grelha[i][j];
                if (ocultarNavios && cell == 'N') cell = ' ';

                Color bg;
                if      (cell == 'N') bg = COR_NAVIO;
                else if (cell == 'X') bg = COR_ACERTO;
                else if (cell == 'O') bg = COR_MISS;
                else if (cell == 'A') bg = COR_AFUNDADO;
                else                  bg = (i == hoverL && j == hoverC && clicavel) ? COR_AGUA_HOVER : COR_AGUA;

                g.setColor(bg);
                g.fillRect(cx + 1, cy + 1, cs - 1, cs - 1);

                if      (cell == 'X' || cell == 'A') drawCross(g, cx, cy, cs, cell == 'A');
                else if (cell == 'O')                drawCircle(g, cx, cy, cs);

                g.setColor(COR_GRID);
                g.drawRect(cx, cy, cs, cs);
            }
        }

        // Overlay de preview (colocação de barcos)
        if (previewTam > 0 && hoverL >= 0 && hoverC >= 0) {
            g.setColor(previewOk ? COR_PREV_OK : COR_PREV_NOK);
            for (int k = 0; k < previewTam; k++) {
                int pl = hoverL + (previewH ? 0 : k);
                int pc = hoverC + (previewH ? k : 0);
                if (pl >= 0 && pl < 10 && pc >= 0 && pc < 10) {
                    g.fillRect(ox + pc * cs + 1, oy + pl * cs + 1, cs - 1, cs - 1);
                }
            }
        }
    }

    private void drawCross(Graphics2D g, int cx, int cy, int cs, boolean afundado) {
        int p = Math.max(3, cs / 5);
        g.setColor(afundado ? new Color(255, 200, 50) : Color.WHITE);
        float stroke = Math.max(1.5f, cs / 14f);
        g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx + p, cy + p, cx + cs - p, cy + cs - p);
        g.drawLine(cx + cs - p, cy + p, cx + p, cy + cs - p);
        g.setStroke(new BasicStroke(1));
    }

    private void drawCircle(Graphics2D g, int cx, int cy, int cs) {
        int p = Math.max(3, cs / 4);
        g.setColor(new Color(90, 130, 175));
        float stroke = Math.max(1.5f, cs / 16f);
        g.setStroke(new BasicStroke(stroke));
        g.drawOval(cx + p, cy + p, cs - p * 2, cs - p * 2);
        g.setStroke(new BasicStroke(1));
    }

    public void setClickHandler(BiConsumer<Integer, Integer> h) { this.clickHandler = h; }

    public void setClicavel(boolean b) {
        this.clicavel = b;
        setCursor(b ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
    }

    public void setPreview(int tam, boolean horizontal) { previewTam = tam; previewH = horizontal; }
    public void setPreviewOk(boolean ok) { previewOk = ok; }
    public void clearPreview() { previewTam = 0; }

    public int getHoverL() { return hoverL; }
    public int getHoverC() { return hoverC; }
}
