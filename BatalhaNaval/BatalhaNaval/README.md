# BatalhaNaval — TP3

**Batalha Naval em rede** implementado em Java com Sockets e Threads.

## Estrutura do Projeto (Maven)

```
BatalhaNaval/
├── pom.xml
├── README.md
└── src/main/java/batalhanaval/
    ├── protocol/
    │   └── Protocolo.java          ← Comandos cliente↔servidor
    ├── model/
    │   ├── TipoNavio.java          ← Enum: 5 tipos de navios
    │   ├── TipoResultado.java      ← Enum: Água / Acertou / Afundou
    │   ├── Navio.java              ← Um navio com posições e acertos
    │   ├── ResultadoTiro.java      ← Resultado de um disparo
    │   ├── Tabuleiro.java          ← Grelha 10×10, coloca/dispara
    │   ├── FaseJogo.java           ← Fases: aguardar / barcos / em jogo / fim
    │   └── EstadoJogo.java         ← Estado completo + guardar/carregar
    ├── server/
    │   ├── Servidor.java           ← Main do servidor
    │   ├── GestorJogo.java         ← Árbitro central (synchronized)
    │   └── ClienteHandler.java     ← Thread por cliente
    └── client/
        └── Cliente.java            ← Main do cliente (CLI com 2 threads)
```

## Compilar

```bash
mvn package
# ou (sem Maven):
mkdir -p target/classes
find src -name "*.java" | xargs javac -d target/classes -encoding UTF-8
jar cfm target/BatalhaNaval-server.jar manifest-server.txt -C target/classes .
jar cfm target/BatalhaNaval-client.jar manifest-client.txt -C target/classes .
```

## Executar

### Servidor
```bash
java -jar target/BatalhaNaval-server.jar [porta]
# porta padrão: 12345
```

### Cliente (2 instâncias, em terminais diferentes)
```bash
java -jar target/BatalhaNaval-client.jar [host] [porta]
# ex: java -jar target/BatalhaNaval-client.jar localhost 12345
```

## Funcionalidades Implementadas

### Servidor
- [x] Cria novo jogo ou carrega estado guardado (`.bns`)
- [x] Aguarda ligação de dois jogadores via TCP
- [x] Valida disposição dos barcos (4×1, 3×2, 2×3, 1×4, 1×5)
- [x] Sorteia o jogador inicial aleatoriamente
- [x] 3 tiros por turno, alternância automática
- [x] Informa resultado de cada tiro: Água / Acertou em X / Afundou X
- [x] Guarda estado automaticamente em caso de desconexão
- [x] Guarda estado a pedido de um jogador

### Cliente
- [x] Liga ao IP:Porta do servidor
- [x] Interface CLI responsiva com **2 threads** (receção separada do input)
- [x] Colocação de barcos interativa com visualização do tabuleiro
- [x] Disparo com validação de coordenadas (ex: `3 E` ou `3 4`)
- [x] Dois tabuleiros 10×10: próprio (navios visíveis) e adversário (só tiros)
- [x] Comando `guardar` no turno para gravar o estado
- [x] Comando `sair` para terminar

## Navios

| Tipo              | Casas | Quantidade |
|-------------------|-------|------------|
| Submarino         | 1     | 4          |
| Contratorpedeiro  | 2     | 3          |
| Navio de Guerra   | 3     | 2          |
| Couraçado         | 4     | 1          |
| Porta-Aviões      | 5     | 1          |

## Protocolo de Comunicação

Mensagens de texto simples separadas por `|` via TCP:

```
NOVO_JOGO|nome        → cliente inicia
COLOCAR_BARCOS|dados  → cliente envia barcos (serialização: TipoNavio|linha|col|H/V;...)
TIRO|linha|coluna     → cliente dispara
GUARDAR_JOGO          → cliente pede guardamento
TEU_TURNO|tirosRest   → servidor notifica turno
RESULTADO_TIRO|l|c|res → servidor informa resultado
FIM_JOGO|VITORIA/DERROTA/EMPATE
```

## Guardar / Carregar

Os jogos são guardados em `saves/jogo_YYYYMMDD_HHmmss.bns` (serialização Java).  
Ao iniciar o servidor, lista os ficheiros disponíveis e pergunta se quer carregar.
