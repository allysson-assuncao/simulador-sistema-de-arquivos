package org.example.sistema_de_arquivos;
import java.io.*;

public class Compactador {

    /**
     * Compacta o arquivo usando o algoritmo RLE.
     * Formato: [Quantidade][Byte]
     */

    public static void compactar(File arquivoOrigem, File arquivoDestino) throws IOException {
        // FileInputStream para ler bytes
        // PushbackInputStream permite devolver um byte lido (essencial para o RLE)
        try (PushbackInputStream in = new PushbackInputStream(new FileInputStream(arquivoOrigem));
             FileOutputStream out = new FileOutputStream(arquivoDestino)) {

            int byteAtual;

            // Lê o primeiro byte do arquivo
            while ((byteAtual = in.read()) != -1) {
                int contador = 1;
                int proximoByte;

                // Loop interno: Verifica quantos bytes idênticos existem na sequência
                // O limite de 255 é porque vamos gravar a quantidade em 1 único byte (0-255)
                while ((proximoByte = in.read()) != -1) {
                    if (proximoByte == byteAtual && contador < 255) {
                        contador++;
                    } else {
                        // Se o byte for diferente, ou se chegamos a 255 repetições:
                        // Devolvemos o byte para o fluxo, para ele ser processado na próxima volta
                        in.unread(proximoByte);
                        break;
                    }
                }

                // --- GRAVAÇÃO NO ARQUIVO ZIPADO ---
                // 1. Escreve a QUANTIDADE de repetições
                out.write(contador);

                // 2. Escreve o BYTE (o dado em si)
                out.write(byteAtual);
            }
        }
    }

    /**
     * Descompacta o arquivo lendo o par [Quantidade][Byte]
     */

    public static void descompactar(File arquivoZipado, File arquivoDestino) throws IOException {
        try (FileInputStream in = new FileInputStream(arquivoZipado);
             FileOutputStream out = new FileOutputStream(arquivoDestino)) {

            int quantidade;

            // O loop lê sempre de par em par:
            // 1º lê a QUANTIDADE
            while ((quantidade = in.read()) != -1) {
                // 2º lê o BYTE DE DADOS
                int valorByte = in.read();

                // Segurança: Se o arquivo acabou no meio do par (corrompido)
                if (valorByte == -1) break;

                // Escreve o byte no destino X vezes (baseado na quantidade)
                for (int i = 0; i < quantidade; i++) {
                    out.write(valorByte);
                }
            }
        }
    }
}