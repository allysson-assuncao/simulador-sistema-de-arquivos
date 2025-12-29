package org.example.sistema_de_arquivos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

//Classe responsável por gerenciar o sistema de arquivos.
public class SistemaArquivos {

    private Diretorio raiz;
    private Diretorio diretorioAtual;
    private List<String> historicoComandos;

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
        this.historicoComandos = new ArrayList<>();

        inicializarEstruturaPadrao();

        try {
            cd("/home/user");
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public void registrarComando(String comando) {
        historicoComandos.add(comando);
    }

    private void inicializarEstruturaPadrao() {
        try {
            // 1. Diretórios da Raiz (Base Linux)
            mkdir("/home");
            mkdir("/lib");
            mkdir("/run");
            mkdir("/tmp");
            mkdir("/usr");
            mkdir("/var");
            mkdir("/etc");
            mkdir("/bin");

            // 2. Estrutura do Usuário
            mkdir("/home/user");
            mkdir("/home/user/Documents");
            mkdir("/home/user/Downloads");
            mkdir("/home/user/Desktop");
            mkdir("/home/user/Pictures");

            // 3. Subdiretórios de Sistema
            mkdir("/var/log");
            mkdir("/usr/bin");

            // 4. Arquivos Iniciais (usando touch)

            // Em Documents
            touch("/home/user/Documents/trabalho_so.txt");
            escreverNoArquivo("/home/user/Documents/trabalho_so.txt", "Rascunho do trabalho de Sistemas Operacionais\nData: 20/01/25");

            touch("/home/user/Documents/receita.txt");
            escreverNoArquivo("/home/user/Documents/receita.txt", "Ingredientes:\n- Leite\n- Ovos\n- Farinha");

            // Em Desktop
            touch("/home/user/Desktop/todo.list");
            escreverNoArquivo("/home/user/Desktop/todo.list", "1. Implementar comandos\n2. Testar cd\n3. Repetir");

            // Em Logs
            touch("/var/log/syslog");
            escreverNoArquivo("/var/log/syslog", "[INFO] Sistema de Arquivos Inicializado com Sucesso.");

        } catch (Exception e) {
            System.err.println("Erro ao inicializar estrutura padrão: " + e.getMessage());
        }
    }

    private void escreverNoArquivo(String caminho, String texto) {
        try {
            NoSistema no = resolverCaminho(caminho);
            if (no instanceof Arquivo) {
                ((Arquivo) no).setConteudo(texto);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public Diretorio getDiretorioAtual() {
        return diretorioAtual;
    }

    // Método essencial do sistema: Retorna o Nó apontado pelo caminho usando recursividade e tratando casos específicos
    private NoSistema resolverCaminho(String caminho) throws Exception {
        if (caminho == null || caminho.isEmpty()) return diretorioAtual;

        // Atalho simples
        if (caminho.equals("/")) return raiz;
        if (caminho.equals(".")) return diretorioAtual;

        // 1. Decide por onde começar a busca a partir do primeiro caractere
        Diretorio atualNavegacao;
        if (caminho.startsWith("/")) {
            atualNavegacao = raiz; // Caminho Absoluto
            // Remove a primeira barra para evitar a criação de uma string vazia no split
            caminho = caminho.substring(1);
        } else {
            atualNavegacao = diretorioAtual; // Caminho Relativo
        }

        // 2. Pega cada parte do caminho entre as barras
        String[] partes = caminho.split("/");

        // 3. Caminha pela árvore
        for (String parte : partes) {
            // Ignora partes vazias (caso de barras duplas //) ou ponto (.)
            if (parte.isEmpty() || parte.equals(".")) {
                continue;
            } else if (parte.equals("..")) {
                if (atualNavegacao.getPai() != null) {
                    atualNavegacao = atualNavegacao.getPai();
                }
                // Se pai for null (raiz), continua na raiz
                continue;
            }
            // Tenta descer um nível
            NoSistema proximo = atualNavegacao.getFilho(parte);
            if (proximo == null) {
                throw new Exception("Caminho não encontrado: " + parte);
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
        return atualNavegacao;
    }

    // PWD (Exibe o caminho atual completo)
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

    // CD (Navega pelo sistema de arquivos)
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

    // LS (Lista os diretórios e arquivos dentro de um diretório)
    public String ls(String caminhoOpcional, boolean mostrarOcultos, boolean formatoLongo) {
        try {
            Diretorio alvo = diretorioAtual;
            // Se passou caminho, resolve primeiro
            if (caminhoOpcional != null && !caminhoOpcional.isEmpty()) {
                NoSistema no = resolverCaminho(caminhoOpcional);
                if (no instanceof Arquivo) {
                    // Se for um arquivo, retorna somente os seus detalhes
                    return formatarSaidaLs(Collections.singletonList(no), formatoLongo);
                }
                alvo = (Diretorio) no;
            }
            // Pega os filhos
            List<NoSistema> conteudo = new ArrayList<>(alvo.getFilhos().values());

            // Filtra ocultos (começam com .) se a flag -a não estiver ativa
            if (!mostrarOcultos) {
                conteudo.removeIf(no -> no.getNome().startsWith("."));
            }
            // Ordena alfabeticamente
            conteudo.sort(Comparator.comparing(NoSistema::getNome));

            return formatarSaidaLs(conteudo, formatoLongo);

        } catch (Exception e) {
            return "Erro: " + e.getMessage();
        }
    }

    // Auxiliar de formatação do LS
    private String formatarSaidaLs(List<NoSistema> nos, boolean longo) {
        if (nos.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();

        for (NoSistema no : nos) {
            if (longo) {
                // Formato: -rw-r--r-- 1 user 1024 Jan 01 12:00 arquivo.txt
                sb.append(String.format("%s 1 %s %5d %s %s\n",
                        no.getPermissoes(),
                        no.getDono(),
                        no.getTamanho(),
                        no.getDataFormatada(),
                        no.getNome()
                ));
            } else {
                // Formato simples
                sb.append(no.getNome()).append("  ");
            }
        }
        if (!longo) sb.append("\n");
        return sb.toString();
    }

    // Tree (Exibe a arvore de diretórios a partir de um caminho de forma recursivo)
    public String tree(String caminho) {
        StringBuilder sb = new StringBuilder();
        sb.append(".\n");
        if (caminho.isEmpty()) {
            listarRecursivo(diretorioAtual, "", sb);
        } else {
            try {
                NoSistema no = resolverCaminho(caminho);
                if (no instanceof Diretorio) {
                    listarRecursivo((Diretorio) no, "", sb);
                }
            } catch (Exception e) {
                listarRecursivo(diretorioAtual, "", sb);
            }
        }
        return sb.toString();
    }

    private void listarRecursivo(Diretorio dir, String prefixo, StringBuilder sb) {
        List<NoSistema> filhos = new ArrayList<>(dir.getFilhos().values());
        // Ordenar para facilitar a leitura e identificação
        filhos.sort(Comparator.comparing(NoSistema::getNome));

        for (int i = 0; i < filhos.size(); i++) {
            NoSistema no = filhos.get(i);
            boolean isLast = (i == filhos.size() - 1);

            sb.append(prefixo);
            sb.append(isLast ? "└── " : "├── ");
            sb.append(no.getNome()).append("\n");

            if (no instanceof Diretorio) {
                listarRecursivo((Diretorio) no, prefixo + (isLast ? "    " : "│   "), sb);
            }
        }
    }

    // CAT (Exibe o conteúdo de arquivos)
    public String cat(String caminho) {
        try {
            NoSistema no = resolverCaminho(caminho);
            if (no instanceof Diretorio) {
                return "cat: " + no.getNome() + ": É um diretório";
            }
            return ((Arquivo) no).getConteudo();
        } catch (Exception e) {
            return "cat: " + e.getMessage();
        }
    }

    // HISTORY (Exibe o histórico dos comandos executados)
    public String getHistorico() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < historicoComandos.size(); i++) {
            sb.append(i + 1).append("  ").append(historicoComandos.get(i)).append("\n");
        }
        return sb.toString();
    }

    // MKDIR (Suporta "mkdir pasta" ou "mkdir /a/b/pasta")
    public String mkdir(String caminho) {
        try {
            // Tratamento para evitar a criação de pastas vazias
            if (caminho.endsWith("/")) {
                caminho = caminho.substring(0, caminho.length() - 1);
            }

            // Separa o caminho do pai e o nome do novo diretório
            String nomeNovoDir;
            Diretorio paiAlvo;

            int indiceUltimaBarra = caminho.lastIndexOf('/');

            if (indiceUltimaBarra == -1) {
                // Caso 1: Caminho relativo simples -> "pasta"
                nomeNovoDir = caminho;
                paiAlvo = diretorioAtual;
            } else {
                // Caso 2: Caminho composto -> "/pai/filho"
                String caminhoPai = caminho.substring(0, indiceUltimaBarra);
                nomeNovoDir = caminho.substring(indiceUltimaBarra + 1);

                // Se o pai for vazio, significa que é filho da raiz (ex: "/home")
                if (caminhoPai.isEmpty()) {
                    caminhoPai = "/";
                }

                // Busca o objeto do pai
                NoSistema noPai = resolverCaminho(caminhoPai);

                if (noPai == null) return "Erro: Caminho base não encontrado.";
                if (!(noPai instanceof Diretorio)) return "Erro: '" + caminhoPai + "' não é um diretório.";

                paiAlvo = (Diretorio) noPai;
            }

            // Validação: Já existe?
            if (paiAlvo.getFilho(nomeNovoDir) != null) {
                return "Erro: Já existe algo com o nome '" + nomeNovoDir + "'.";
            }

            // Criação e ligação
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

    // TOUCH (Cria um arquivo vazio)
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

    // ECHO (Sobrescreve > ou adiciona >> conteúdo em arquivos)
    public String escreverNoArquivo(String caminho, String texto, boolean append) {
        try {
            // 1. Resolver o arquivo ou o diretório pai (caso o arquivo não exista)
            NoSistema noAlvo = null;
            try {
                noAlvo = resolverCaminho(caminho);
            } catch (Exception e) {
                // Caso o arquivo não existe, tentamos cria-lo no diretório pai encontrado
            }

            Arquivo arquivo;
            if (noAlvo != null) {
                if (noAlvo instanceof Diretorio) {
                    return "Erro: '" + noAlvo.getNome() + "' é um diretório.";
                }
                arquivo = (Arquivo) noAlvo;
            } else {
                // Não existe, encontra o pai e cria
                String res = touch(caminho);
                if (res.startsWith("Erro")) return res; // Falha ao criar com touch
                arquivo = (Arquivo) resolverCaminho(caminho);
            }
            // 2. Escrever conteúdo
            if (append) {
                arquivo.appendConteudo(texto);
            } else {
                arquivo.setConteudo(texto);
            }
            return ""; // Sucesso (sem mensagem de erro)

        } catch (Exception e) {
            return "Erro ao escrever: " + e.getMessage();
        }
    }

    // RENAME (Renomeia arquivos ou diretórios)
    public String rename(String nomeAntigo, String novoNome) {
        try {
            // Validação simples do novo nome
            if (novoNome.contains("/")) return "Erro: O novo nome não pode conter barras (use mv para mover).";

            NoSistema alvo = resolverCaminho(nomeAntigo);
            if (alvo == raiz) return "Erro: Não é possível renomear a raiz.";

            Diretorio pai = alvo.getPai();

            // Verifica se o nome já existe no diretório pai
            if (pai.getFilho(novoNome) != null) {
                return "Erro: Já existe um arquivo/diretório com o nome '" + novoNome + "'.";
            }

            // Remove a referência do arquivo no map a partir da chave antiga
            pai.removerFilho(alvo.getNome());

            // Atualiza o nome interno do objeto
            alvo.setNome(novoNome);
            pai.adicionarFilho(alvo);

            return "Renomeado de '" + nomeAntigo + "' para '" + novoNome + "'.";

        } catch (Exception e) {
            return "Erro ao renomear: " + e.getMessage();
        }
    }

    // HEAD (Exibe as primeiras N linhas de um arquivo)
    public String head(String caminho, int linhas) {
        try {
            Arquivo arq = obterArquivoTexto(caminho);
            String[] todasLinhas = arq.getConteudo().split("\n");

            StringBuilder sb = new StringBuilder();
            int limite = Math.min(linhas, todasLinhas.length);

            for (int i = 0; i < limite; i++) {
                sb.append(todasLinhas[i]).append("\n");
            }
            return sb.toString();

        } catch (Exception e) {
            return e.getMessage();
        }
    }

    // TAIL (Exibe as últimas N linhas de um arquivo)
    public String tail(String caminho, int linhas) {
        try {
            Arquivo arq = obterArquivoTexto(caminho);
            String[] todasLinhas = arq.getConteudo().split("\n");

            StringBuilder sb = new StringBuilder();
            int inicio = Math.max(0, todasLinhas.length - linhas);

            for (int i = inicio; i < todasLinhas.length; i++) {
                sb.append(todasLinhas[i]).append("\n");
            }
            return sb.toString();

        } catch (Exception e) {
            return e.getMessage();
        }
    }

    // WC (Conta as palavras de um arquivo)
    public String wc(String caminho) {
        try {
            Arquivo arq = obterArquivoTexto(caminho);
            String conteudo = arq.getConteudo();

            int numLinhas = conteudo.isEmpty() ? 0 : conteudo.split("\n").length;
            int numBytes = conteudo.length();

            // Contar palavras (split por whitespace)
            int numPalavras = 0;
            if (!conteudo.trim().isEmpty()) {
                numPalavras = conteudo.trim().split("\\s+").length;
            }

            return String.format("%d %d %d %s", numLinhas, numPalavras, numBytes, arq.getNome());

        } catch (Exception e) {
            return e.getMessage();
        }
    }

    // Método auxiliar para validar se é arquivo
    private Arquivo obterArquivoTexto(String caminho) throws Exception {
        NoSistema no = resolverCaminho(caminho);
        if (no instanceof Diretorio) throw new Exception("Erro: '" + caminho + "' é um diretório.");
        return (Arquivo) no;
    }

    // PERMISSÕES E PROPRIEDADES
    public String chmod(String codigoOctal, String caminho) {
        try {
            // Validação do formato (tem que ser 3 números de 0 a 7)
            if (codigoOctal.length() != 3 || !codigoOctal.matches("[0-7]{3}")) {
                return "Erro: Formato inválido. Use 3 dígitos octais (ex: 755, 777).";
            }

            // Busca o alvo
            NoSistema alvo = resolverCaminho(caminho);

            //  Monta a nova string de permissão
            StringBuilder sb = new StringBuilder();

            // O primeiro caractere indica se é diretório (d) ou arquivo (-)
            if (alvo instanceof Diretorio) {
                sb.append("d");
            } else {
                sb.append("-");
            }

            // Converte os 3 números para letras
            sb.append(converterPermissaoOctal(codigoOctal.charAt(0))); // Dono
            sb.append(converterPermissaoOctal(codigoOctal.charAt(1))); // Grupo
            sb.append(converterPermissaoOctal(codigoOctal.charAt(2))); // Outros

            // Aplica a mudança
            alvo.setPermissoes(sb.toString());
            return "Permissões de '" + alvo.getNome() + "' alteradas para " + sb.toString();

        } catch (Exception e) {
            return "Erro ao executar chmod: " + e.getMessage();
        }
    }

    public String chown(String novoDono, String caminho) {
        try {
            // Busca o alvo
            NoSistema alvo = resolverCaminho(caminho);

            if (novoDono == null || novoDono.trim().isEmpty()) {
                return "Erro: Nome de usuário inválido.";
            }

            // Aplica a mudança
            String antigo = alvo.getDono();
            alvo.setDono(novoDono);

            return "Dono de '" + alvo.getNome() + "' alterado: " + antigo + " -> " + novoDono;

        } catch (Exception e) {
            return "Erro ao executar chown: " + e.getMessage();
        }
    }

    // Método Auxiliar: Converte número ('7') para string ("rwx")
    private String converterPermissaoOctal(char numero) {
        switch (numero) {
            case '0': return "---";
            case '1': return "--x";
            case '2': return "-w-";
            case '3': return "-wx";
            case '4': return "r--";
            case '5': return "r-x";
            case '6': return "rw-";
            case '7': return "rwx";
            default: return "---";
        }
    }

    // FIND
    public String find(String caminho, String nome) {
        try {
            NoSistema inicio = resolverCaminho(caminho);

            if (!(inicio instanceof Diretorio dir)) {
                return "find: '" + caminho + "': Não é um diretório\n";
            }

            StringBuilder resultado = new StringBuilder();
            buscarRecursivo(dir, nome, resultado);
            return resultado.toString();

        } catch (Exception e) {
            return "find: caminho inválido\n";
        }
    }

    // FIND - Metodo auxiliar
    private void buscarRecursivo(NoSistema no, String nomeBuscado, StringBuilder resultado) {
        if (no.getNome().equals(nomeBuscado)) {
            resultado.append(montarCaminho(no)).append("\n");
        }

        if (no instanceof Diretorio dir) {
            for (NoSistema filho : dir.getFilhos().values()) {
                buscarRecursivo(filho, nomeBuscado, resultado);
            }
        }
    }

    // Metodo auxiliar: monta caminho do nó até o topo
    private String montarCaminho(NoSistema no) {
        if (no == null || no.getPai() == null) return "/";

        String pai = montarCaminho(no.getPai());
        return (pai.equals("/") ? "" : pai) + "/" + no.getNome();
    }

    // GREP
    public String grep(String termo, String caminho) {
        try {
            Arquivo arq = obterArquivoTexto(caminho);
            String conteudo = arq.getConteudo();

            StringBuilder sb = new StringBuilder();
            String[] linhas = conteudo.split("\n");

            for (String linha : linhas) {
                if (linha.contains(termo)) {
                    sb.append(linha).append("\n");
                }
            }

            return sb.toString();

        } catch (Exception e) {
            return "grep: " + e.getMessage() + "\n";
        }
    }
}
