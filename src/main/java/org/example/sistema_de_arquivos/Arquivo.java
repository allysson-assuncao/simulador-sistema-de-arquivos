package org.example.sistema_de_arquivos;

import java.time.LocalDateTime;

public class Arquivo extends NoSistema {
    private StringBuilder conteudo;

    public Arquivo(String nome, Diretorio pai) {
        super(nome, pai);
        this.conteudo = new StringBuilder();
        this.permissoes = "-rw-r--r--";// Permissão padrão de arquivo
    }

    public void setConteudo(String texto) {
        this.conteudo = new StringBuilder(texto);
    }

    public void appendConteudo(String texto) {
        // Verifica se o conteúdo atual existe e se não termina com \n
        if (!conteudo.isEmpty() && conteudo.charAt(conteudo.length() - 1) != '\n') {
            this.conteudo.append("\n");
        }

        this.conteudo.append(texto).append("\n");
        this.dataModificacao = LocalDateTime.now();
    }

    public String getConteudo() {
        return conteudo.toString();
    }

    @Override
    public String getTipo() {
        return "ARQUIVO";
    }

    @Override
    public int getTamanho() {
        return conteudo.length(); // Tamanho em caracteres (bytes aproximados)
    }

}
