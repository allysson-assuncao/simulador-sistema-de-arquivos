package org.example.sistema_de_arquivos;

//Classe responsável por gerenciar o sistema de arquivos.
public class SistemaArquivos {

    private Diretorio raiz;
    private Diretorio diretorioAtual;

    public SistemaArquivos() {
        // Raiz é um caso especial: nome "/" e pai null
        // Instancia direta para burlar a validação do construtor na raiz
        this.raiz = new Diretorio("root", null) {
            @Override
            public String getNome() {
                return "/";
            }
        };
        this.diretorioAtual = raiz;
    }

    public Diretorio getDiretorioAtual() {
        return diretorioAtual;
    }

    // Método essencial do sistema: Retorna o Nó apontado pelo caminho usando recursividade e tratando casos específicos
    private NoSistema resolverCaminho(String caminho) throws Exception {
        if (caminho == null || caminho.isEmpty()) return diretorioAtual;

        // 1. Decide por onde começar a busca a partir do primeiro caractere
        Diretorio atualNavegacao;
        if (caminho.startsWith("/")) {
            atualNavegacao = raiz; // Caminho Absoluto
        } else {
            atualNavegacao = diretorioAtual; // Caminho Relativo
        }

        // 2. Pega cada parte do caminho entre as barras
        String[] partes = caminho.split("/");

        // 3. Caminha pela árvore
        for (String parte : partes) {
            if (parte.isEmpty() || parte.equals(".")) {
                break; // Talvez nem precise
            } else if (parte.equals("..")) {
                if (atualNavegacao.getPai() != null) {
                    atualNavegacao = atualNavegacao.getPai();
                }
                // Se pai for null (raiz), continua na raiz
            } else {
                // Tenta descer um nível
                NoSistema proximo = atualNavegacao.getFilho(parte);
                if (proximo == null) {
                    throw new Exception("Caminho inválido: '" + parte + "' não existe.");
                }

                // Se tem um caminho no meio do caminho: erro
                if (proximo instanceof Arquivo) {
                    if (parte.equals(partes[partes.length - 1])) {
                        return proximo;
                    }
                    throw new Exception("Erro: '" + parte + "' não é um diretório.");
                }

                // É um diretório, prossegue até o fim do caminho
                atualNavegacao = (Diretorio) proximo;
            }
        }
        return atualNavegacao;
    }

    // PWD
    public String getCaminhoCompleto() {
        if (diretorioAtual == raiz) return "/";

        StringBuilder sb = new StringBuilder();
        Diretorio temp = diretorioAtual;
        while (temp != raiz) {
            sb.insert(0, "/" + temp.getNome());
            temp = temp.getPai();
        }
        return sb.toString();
    }

    // CD
    public String cd(String caminho) {
        try {
            NoSistema alvo = resolverCaminho(caminho);
            if (alvo instanceof Diretorio) {
                this.diretorioAtual = (Diretorio) alvo;
                return "";
            } else {
                return "Erro: '" + alvo.getNome() + "' não é um diretório.";
            }
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    // LS
    public String ls(String caminhoOpcional) {
        try {
            Diretorio alvo = diretorioAtual;
            if (caminhoOpcional != null && !caminhoOpcional.isEmpty()) {
                NoSistema no = resolverCaminho(caminhoOpcional);
                if (no instanceof Diretorio) alvo = (Diretorio) no;
                else return no.getNome(); // Se for arquivo, mostra só o nome
            }

            if (alvo.getFilhos().isEmpty()) return "";

            StringBuilder saida = new StringBuilder();
            for (NoSistema filho : alvo.getFilhos().values()) {
                if (filho instanceof Diretorio) saida.append("[D] ");
                else saida.append("[A] ");
                saida.append(filho.getNome()).append("  ");
            }
            return saida.toString();

        } catch (Exception e) {
            return e.getMessage();
        }
    }

    // MKDIR (Suporta "mkdir pasta" ou "mkdir /a/b/pasta")
    public String mkdir(String caminho) {
        try {
            // Separa o caminho do pai e o nome do novo diretório
            String nomeNovoDir;
            Diretorio paiAlvo;

            int indiceUltimaBarra = caminho.lastIndexOf('/');

            if (indiceUltimaBarra == -1) {
                // Caso simples: "mkdir pasta" (diretório atual)
                nomeNovoDir = caminho;
                paiAlvo = diretorioAtual;
            } else {
                // Caso complexo: "mkdir pai/filho"
                String caminhoPai = caminho.substring(0, indiceUltimaBarra);
                nomeNovoDir = caminho.substring(indiceUltimaBarra + 1); // Pega o que está depois da última barra

                // Se o caminho for apenas "/", o pai é a raiz
                if (caminhoPai.isEmpty()) caminhoPai = "/";

                NoSistema noPai = resolverCaminho(caminhoPai);
                if (!(noPai instanceof Diretorio)) return "Erro: Caminho base não é diretório.";
                paiAlvo = (Diretorio) noPai;
            }

            // Validação final e criação
            if (paiAlvo.getFilho(nomeNovoDir) != null) {
                return "Erro: Já existe algo com o nome '" + nomeNovoDir + "'.";
            }

            Diretorio novo = new Diretorio(nomeNovoDir, paiAlvo);
            paiAlvo.adicionarFilho(novo);
            return "Diretório '" + nomeNovoDir + "' criado com sucesso.";

        } catch (Exception e) {
            return "Erro ao criar diretório: " + e.getMessage();
        }
    }

    // RM (Remove arquivos ou diretórios vazios)
    public String rm(String caminho) {
        try {
            // É necessário encontrar o alvo e remover a referência do pai
            NoSistema alvo = resolverCaminho(caminho);

            if (alvo == raiz) return "Erro: Não é possível remover a raiz.";

            Diretorio pai = alvo.getPai();

            if (alvo instanceof Diretorio) {
                if (((Diretorio) alvo).temFilhos()) {
                    return "Erro: O diretório não está vazio (use rm -rf para remover tudo de forma recursiva).";
                }
            }

            pai.removerFilho(alvo.getNome());
            return "Removido: " + alvo.getNome();

        } catch (Exception e) {
            return "Erro ao remover: " + e.getMessage();
        }
    }

    // TOUCH: Cria um arquivo vazio ou atualiza timestamp
    public String touch(String caminho) {
        try {
            // Resolve o pai (diretório onde o arquivo ficará)
            String nomeArquivo;
            Diretorio paiAlvo;

            int ultimaBarra = caminho.lastIndexOf('/');
            if (ultimaBarra == -1) {
                nomeArquivo = caminho;
                paiAlvo = diretorioAtual;
            } else {
                String caminhoPai = caminho.substring(0, ultimaBarra);
                nomeArquivo = caminho.substring(ultimaBarra + 1);
                if (caminhoPai.isEmpty()) caminhoPai = "/";

                NoSistema noPai = resolverCaminho(caminhoPai);
                if (!(noPai instanceof Diretorio)) return "Erro: Caminho inválido.";
                paiAlvo = (Diretorio) noPai;
            }

            // Verifica se já existe
            NoSistema existente = paiAlvo.getFilho(nomeArquivo);
            if (existente != null) {
                if (existente instanceof Diretorio) return "Erro: Já existe um diretório com esse nome.";
                // Simulando a atualização do timestamp
                return "Arquivo '" + nomeArquivo + "' atualizado.";
            }

            // Criação do arquivo
            Arquivo novoArq = new Arquivo(nomeArquivo, paiAlvo);
            paiAlvo.adicionarFilho(novoArq);
            return "Arquivo '" + nomeArquivo + "' criado.";

        } catch (Exception e) {
            return "Erro: " + e.getMessage();
        }
    }

}
