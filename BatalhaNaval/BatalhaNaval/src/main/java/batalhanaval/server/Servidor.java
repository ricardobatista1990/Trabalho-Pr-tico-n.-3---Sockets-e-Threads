package batalhanaval.server;

import batalhanaval.model.*;
import batalhanaval.protocol.Protocolo;

import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Scanner;

/**
 * Servidor da Batalha Naval.
 *
 * Uso: java -cp BatalhaNaval-server.jar batalhanaval.server.Servidor [porta]
 *
 * - Aguarda dois clientes
 * - Árbitro de todas as regras
 * - Guarda/carrega estado do jogo
 * - Usa uma thread por cliente para comunicação não bloqueante
 */
public class Servidor {

    public static final int PORTA_PADRAO = 12345;

    public static void main(String[] args) throws IOException {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : PORTA_PADRAO;

        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║    BATALHA NAVAL - SERVIDOR        ║");
        System.out.println("╚════════════════════════════════════╝");
        System.out.println("Porta: " + porta);

        // Perguntar se quer carregar jogo guardado
        EstadoJogo estado = null;
        Scanner sc = new Scanner(System.in);

        String[] guardados = EstadoJogo.listarGuardados();
        if (guardados.length > 0) {
            System.out.println("\nJogos guardados disponíveis:");
            for (int i = 0; i < guardados.length; i++)
                System.out.println("  " + (i + 1) + ") " + guardados[i]);
            System.out.print("Carregar jogo? (número ou Enter para novo): ");
            String linha = sc.nextLine().trim();
            if (!linha.isEmpty()) {
                try {
                    int idx = Integer.parseInt(linha) - 1;
                    estado = EstadoJogo.carregar(guardados[idx]);
                    System.out.println("Jogo carregado: " + guardados[idx]);
                } catch (Exception e) {
                    System.out.println("Erro ao carregar. Novo jogo.");
                }
            }
        }

        if (estado == null) {
            estado = new EstadoJogo();
            System.out.println("Novo jogo criado.");
        }

        // Iniciar servidor
        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            System.out.println("\nAguardando jogadores em porta " + porta + "...\n");

            GestorJogo gestor = new GestorJogo(estado, serverSocket);
            gestor.iniciar();
        }
    }
}
