package Programa;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;


public class Programa {

    public static void main(String[] args) {
        Connection con = null;
        try {
            con = Conexao.criarConexao();
            if (con != null) {
                System.out.println("\nPrograma Iniciado com Sucesso!");

                exibirMenu(con);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void exibirMenu(Connection con) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\nOlá Giovanna. \nBem-vindo ao seu sistema de gerenciamento de produtos!");
        int opcao;

        do {
            // Verifica se existem produtos com estoque baixo antes de mostrar o alerta
            if (existemProdutosComEstoqueBaixo(con)) {
                listarProdutosComEstoqueBaixo(con);
            }

            // Verifica se existem produtos com data de validade vencida antes de mostrar o alerta
            if (existemProdutosComValidadeVencida(con)) {
                listarProdutosComValidadeVencida(con);
            }

            System.out.println("\nEscolha uma opção:");
            System.out.println("1. Adicionar Produto");
            System.out.println("2. Remover Produto");
            System.out.println("3. Ver Estoque");
            System.out.println("0. Sair do Sistema");

            opcao = scanner.nextInt();
            scanner.nextLine(); // Limpa o buffer de entrada

            switch (opcao) {
                case 1:
                    adicionarProduto(con);
                    break;
                case 2:
                    removerProduto(con);
                    break;
                case 3:
                    verEstoqueCompleto(con);
                    break;
                case 0:
                    System.out.println("Saindo do sistema. Até mais!");
                    break;
                default:
                    System.out.println("Opção inválida. Tente novamente.");
                    break;
            }

            verificarEstoque(con);

        } while (opcao != 0);

        scanner.close();
    }

    public static void adicionarProduto(Connection con) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\nAdicionar Produto");
        System.out.print("Nome do Produto: ");
        String nome = scanner.nextLine();

        System.out.println("Escolha o tipo do Produto:");
        System.out.println("1. Bebidas");
        System.out.println("2. Alimentos");
        System.out.println("3. Utensílios");
        int tipoOpcao = scanner.nextInt();
        scanner.nextLine(); // Limpa o buffer de entrada

        String tipo = "";
        switch (tipoOpcao) {
            case 1:
                tipo = "Bebidas";
                break;
            case 2:
                tipo = "Alimentos";
                break;
            case 3:
                tipo = "Utensílios";
                break;
            default:
                System.out.println("Opção de tipo inválida. Produto não será adicionado.");
                return;
        }

        System.out.print("Preço do Produto: ");
        double preco = scanner.nextDouble();
        scanner.nextLine(); // Limpa o buffer de entrada

        System.out.print("Descrição do Produto: ");
        String descricao = scanner.nextLine();

        System.out.print("Quantidade: ");
        int quantidade = scanner.nextInt();
        scanner.nextLine(); // Limpa o buffer de entrada

        boolean perecivel = false;
        Date dataValidade = null;
        if (tipoOpcao == 1 || tipoOpcao == 2) {
            System.out.print("O produto é perecível (S/N)? ");
            String resposta = scanner.nextLine();
            if (resposta.equalsIgnoreCase("S")) {
                perecivel = true;
                System.out.print("Data de validade (YYYY-MM-DD): ");
                String dataValidadeStr = scanner.nextLine();
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    dataValidade = dateFormat.parse(dataValidadeStr);
                } catch (ParseException e) {
                    System.out.println("Data de validade inválida. Produto não será adicionado.");
                    return;
                }
            }
        }

        try {
            String sql;
            if (perecivel) {
                sql = "INSERT INTO tbl_produtos (nome, tipo, preco, descricao, qtd, perecivel, data_validade) VALUES (?, ?, ?, ?, ?, ?, ?)";
            } else {
                sql = "INSERT INTO tbl_produtos (nome, tipo, preco, descricao, qtd) VALUES (?, ?, ?, ?, ?)";
            }
            PreparedStatement preparedStatement = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, nome);
            preparedStatement.setString(2, tipo);
            preparedStatement.setDouble(3, preco);
            preparedStatement.setString(4, descricao);
            preparedStatement.setInt(5, quantidade);

            if (perecivel) {
                preparedStatement.setBoolean(6, true);
                preparedStatement.setDate(7, new java.sql.Date(dataValidade.getTime()));
            }

            int linhasAfetadas = preparedStatement.executeUpdate();

            if (linhasAfetadas > 0) {
                ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    System.out.println("Produto adicionado com sucesso. ID do produto: " + id);
                } else {
                    System.out.println("Falha ao obter o ID do produto.");
                }
            } else {
                System.out.println("Falha ao adicionar o produto.");
            }

            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void removerProduto(Connection con) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\nRemover Produto");
        System.out.println("Escolha o tipo de ação:");
        System.out.println("1. Remover do Estoque");
        System.out.println("2. Apagar Permanentemente");
        int acao = scanner.nextInt();
        scanner.nextLine(); // Limpa o buffer de entrada

        if (acao == 1) {
            // Listar os produtos antes de remover do estoque
            listarProdutosComIDs(con);

            System.out.print("Digite o ID do Produto a ser removido do estoque: ");
            int idProduto = scanner.nextInt();
            scanner.nextLine(); // Limpa o buffer de entrada

            System.out.print("Digite a quantidade a ser removida do estoque: ");
            int quantidadeRemover = scanner.nextInt();
            scanner.nextLine(); // Limpa o buffer de entrada

            try {
                String sql = "UPDATE tbl_produtos SET qtd = qtd - ? WHERE id = ? AND qtd >= ?";
                PreparedStatement preparedStatement = con.prepareStatement(sql);
                preparedStatement.setInt(1, quantidadeRemover);
                preparedStatement.setInt(2, idProduto);
                preparedStatement.setInt(3, quantidadeRemover);

                int linhasAfetadas = preparedStatement.executeUpdate();

                if (linhasAfetadas > 0) {
                    System.out.println("Produto removido do estoque com sucesso.");
                } else {
                    System.out.println("Produto com o ID especificado não foi encontrado no estoque ou a quantidade a ser removida é maior do que o estoque disponível.");
                }

                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else if (acao == 2) {
            // Listar os produtos antes de apagar permanentemente
            listarProdutosComIDs(con);

            System.out.print("Digite o ID do Produto a ser apagado permanentemente: ");
            int idProduto = scanner.nextInt();
            scanner.nextLine(); // Limpa o buffer de entrada

            try {
                String sql = "DELETE FROM tbl_produtos WHERE id = ?";
                PreparedStatement preparedStatement = con.prepareStatement(sql);
                preparedStatement.setInt(1, idProduto);

                int linhasAfetadas = preparedStatement.executeUpdate();

                if (linhasAfetadas > 0) {
                    System.out.println("Produto apagado permanentemente com sucesso.");
                } else {
                    System.out.println("Produto com o ID especificado não foi encontrado.");
                }

                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Opção de ação inválida. Saindo da remoção de produtos.");
        }
    }

    public static void listarProdutosComIDs(Connection con) {
        try {
            String sql = "SELECT id, nome, qtd FROM tbl_produtos";
            PreparedStatement preparedStatement = con.prepareStatement(sql);

            ResultSet resultSet = preparedStatement.executeQuery();

            System.out.println("\nLista de Produtos Disponíveis:");
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String nome = resultSet.getString("nome");
                int quantidade = resultSet.getInt("qtd");
                System.out.println("\nID: " + id + "    - QTD em Estoque: " + quantidade + "   - Nome: " + nome );
            }

            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void listarProdutosPorTipo(Connection con, String tipo) {
        try {
            String sql = "SELECT id, nome FROM tbl_produtos WHERE tipo = ?";
            PreparedStatement preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, tipo);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String nome = resultSet.getString("nome");
                System.out.println("Produto :  ID " + id + " - " + nome);
            }

            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void verEstoqueCompleto(Connection con) {
        try {
            String sql = "SELECT id, nome, tipo, preco, qtd FROM tbl_produtos";
            PreparedStatement preparedStatement = con.prepareStatement(sql);

            ResultSet resultSet = preparedStatement.executeQuery();

            System.out.println("\nEstoque Completo:");
            System.out.println("\nID  | Tipo       |  Preço    |  QTD |  Nome ");
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String nome = resultSet.getString("nome");
                String tipo = resultSet.getString("tipo");
                double preco = resultSet.getDouble("preco");
                int quantidade = resultSet.getInt("qtd");

                
                System.out.println("\n " + id + " |  "+ tipo + " |  " +  "R$ " +preco + " |  " + quantidade + "   |  " + nome);
               
                System.out.println();
            }

            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void verificarEstoque(Connection con) {
        // Implemente a lógica de verificação do estoque e exibição de mensagem de alerta
    }

    public static boolean existemProdutosComEstoqueBaixo(Connection con) {
        try {
            String sql = "SELECT COUNT(*) AS total FROM tbl_produtos WHERE qtd <= 3";
            PreparedStatement preparedStatement = con.prepareStatement(sql);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                int total = resultSet.getInt("total");
                return total > 0;
            }

            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void listarProdutosComEstoqueBaixo(Connection con) {
        try {
            String sql = "SELECT id, nome, tipo, preco, qtd FROM tbl_produtos WHERE qtd <= 3";
            PreparedStatement preparedStatement = con.prepareStatement(sql);

            ResultSet resultSet = preparedStatement.executeQuery();

            System.out.println("\nALERTA !!!!!");
            System.out.println("\nProdutos com Estoque Baixo:");
            System.out.println("\nID  | Tipo       |  Preço    |  QTD |  Nome ");
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String nome = resultSet.getString("nome");
                String tipo = resultSet.getString("tipo");
                double preco = resultSet.getDouble("preco");
                int quantidade = resultSet.getInt("qtd");

                System.out.println("\n " + id + " |  " + tipo + " |  " + "R$ " + preco + " |  " + quantidade + "   |  " + nome);

                System.out.println();
            }

            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static boolean existemProdutosComValidadeVencida(Connection con) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date dataAtual = new Date();
            String dataAtualStr = dateFormat.format(dataAtual);
            
            // Use java.sql.Date para representar a data atual
            java.sql.Date dataAtualSQL = java.sql.Date.valueOf(dataAtualStr);

            String sql = "SELECT COUNT(*) AS total FROM tbl_produtos WHERE perecivel = true AND data_validade < ?";
            PreparedStatement preparedStatement = con.prepareStatement(sql);
            preparedStatement.setDate(1, dataAtualSQL); // Defina a data atual com setDate

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                int total = resultSet.getInt("total");
                return total > 0;
            }

            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void listarProdutosComValidadeVencida(Connection con) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date dataAtual = new Date();
            String dataAtualStr = dateFormat.format(dataAtual);

            // Utilize uma comparação DATE válida
            String sql = "SELECT id, nome, data_validade FROM tbl_produtos WHERE perecivel = true AND data_validade::date < ?::date";
            PreparedStatement preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, dataAtualStr);

            ResultSet resultSet = preparedStatement.executeQuery();

            System.out.println("\nProdutos Vencidos:");
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String nome = resultSet.getString("nome");
                String dataValidade = resultSet.getString("data_validade");
                System.out.println("ID: " + id + " - Data de Validade: " + dataValidade +  " - Nome: " + nome );
            }

            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
