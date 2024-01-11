package Programa;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexao {

	static final String URL = "jdbc:postgresql://localhost:5432/postgres"; //incica o caminho do banco de dados
	static final String USER = "postgres"; // aqui vai o nome usuario que vc quer acessar
	static final String PASS = "Rh132010*"; // aqui a senha do seu banco

	public static Connection criarConexao() throws ClassNotFoundException, SQLException{
	Class.forName("org.postgresql.Driver");
	Connection conecta = DriverManager.getConnection(URL, USER, PASS);
	if (conecta != null){
	System.out.print("Entrando no Sistema...");
	return conecta;
	}
	return null;
	}
	
}
