package org.example.sistema_de_arquivos;

public abstract class NoSistema {
    protected String nome;
    protected Diretorio pai; // Referência essencial para 'cd ..'

    public NoSistema(String nome, Diretorio pai) {
        this.nome = nome;
        this.pai = pai;
    }

    public String getNome() { return nome; }
    public Diretorio getPai() { return pai; }

    // Método abstrato que para efetivar comandos como 'ls -l' ou 'stat' eventualmente
    public abstract String getTipo();
}
