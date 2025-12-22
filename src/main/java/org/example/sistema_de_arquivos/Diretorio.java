package org.example.sistema_de_arquivos;

import java.util.HashMap;
import java.util.Map;

public class Diretorio extends NoSistema {
    // HashMap: Representa os filhos diretos de cada diretório
    // A chave de cada nó é apenas o seu nome, o valor é o objeto em sí
    private Map<String, NoSistema> filhos;

    public Diretorio(String nome, Diretorio pai) {
        super(nome, pai);
        this.filhos = new HashMap<>();
        this.permissoes = "drwxr-xr-x"; // Permissão padrão de diretório (d no início)
    }

    public void adicionarFilho(NoSistema no) {
        filhos.put(no.getNome(), no);
    }

    public void removerFilho(String nome) {
        filhos.remove(nome);
    }

    public NoSistema getFilho(String nome) {
        return filhos.get(nome);
    }

    public Map<String, NoSistema> getFilhos() {
        return filhos;
    }

    public boolean temFilhos() {
        return !filhos.isEmpty();
    }

    @Override
    public String getTipo() {
        return "DIRETORIO";
    }

    @Override
    public int getTamanho() {
        return 4096; // Tamanho padrão de diretório em Linux (metadados), não a soma dos filhos
    }


}
