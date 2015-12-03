package br.ufac.chatssl;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Iterator;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class Servidor {

	private ArrayList<PrintWriter> listaClientes;
	private Socket socketCliente;
	private ServerSocket socketServidor;
	private SSLServerSocket socketServidorSSL;
	private int port = 6789;
	private int portSSL = 5678;
	
	public static void main(String[] args) {
		
		boolean ssl = false;
		
		if (args.length > 0) {
			ssl = args[0].equals("ssl");
		}
		
		new Servidor().run(ssl);
		
	}
	
	public void run(boolean ssl) {
		
		listaClientes = new ArrayList<PrintWriter>();
		
		
		try {
			
			if (ssl) {
				
				KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
				ks.load(new FileInputStream("src/br/ufac/chatssl/cert.keys"), "ufac2015".toCharArray());

				KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				kmf.init(ks, "ufac2015".toCharArray());

				TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); 
				tmf.init(ks);

				SSLContext sc = SSLContext.getInstance("SSL"); 
				TrustManager[] trustManagers = tmf.getTrustManagers(); 
				sc.init(kmf.getKeyManagers(), trustManagers, null); 

				SSLServerSocketFactory ssf = sc.getServerSocketFactory(); 
				socketServidorSSL = (SSLServerSocket) ssf.createServerSocket(portSSL);
				
			} else {
				
				socketServidor = new ServerSocket(port);
				
			}
			
			System.out.println("Servidor iniciado");
			
			while (true) {
				
				socketCliente = (ssl) ? socketServidorSSL.accept() : socketServidor.accept();
				PrintWriter writer = new PrintWriter(socketCliente.getOutputStream());
				listaClientes.add(writer);
								
				InputStreamReader isrUsuario = new InputStreamReader(socketCliente.getInputStream());
				BufferedReader readerUsuario = new BufferedReader(isrUsuario);
				final String usuario = readerUsuario.readLine();
				
				Thread t = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							InputStreamReader isr = new InputStreamReader(socketCliente.getInputStream());
							BufferedReader reader = new BufferedReader(isr);
							
							String mensagem;
							
							while ((mensagem = reader.readLine()) != null) {
								enviarMensagens(mensagem, usuario);
							}
						} catch (Exception e) {
							System.out.println("ERRO: " + e.getMessage());
						}
					}
				});
				
				t.start();
				
				enviarMensagens(usuario + " entrou no chat.", "");
				
			}
			
		} catch (Exception e) {
			System.out.println("ERRO: " + e.getMessage());
		}
		
		try {
			socketServidor.close();
		} catch (IOException e) {
			System.out.println("ERRO: " + e.getMessage());
		}
		
	}

	public void enviarMensagens(String mensagem, String usuario) {
		
		Iterator<PrintWriter> it = listaClientes.iterator();
		String saida = "";
		
		while (it.hasNext()) {
			try {
				PrintWriter writer = (PrintWriter) it.next();
				if (usuario.equals("")) {
					saida = mensagem + "\n";
				} else if (mensagem.equals("[sair]")) {
					saida = usuario + " saiu do chat.\n";
				} else {
					saida = usuario + " diz: " + mensagem + "\n";
				}
				writer.println(saida);
				writer.flush();
			} catch (Exception e) {
				System.out.println("ERRO: " + e.getMessage());
			}
		}
		
	}
	
	
	
}