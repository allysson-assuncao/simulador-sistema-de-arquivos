package org.example.sistema_de_arquivos;

public class Arquivo extends NoSistema {
    private StringBuilder conteudo;

    public Arquivo(String nome, Diretorio pai) {
        super(nome, pai);
        this.conteudo = new StringBuilder();
    }

    public void setConteudo(String texto) {
        this.conteudo = new StringBuilder(texto);
    }

    public void appendConteudo(String texto) {
        this.conteudo.append(texto).append("\n");
    }

    public String getConteudo() {
        return conteudo.toString();
    }

    @Override
    public String getTipo() {
        return "ARQUIVO";
    }
}
