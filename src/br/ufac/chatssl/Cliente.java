package br.ufac.chatssl;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.TitledBorder;

public class Cliente {

	private JTextArea txtMensagens;
	private JTextField txtMensagemUsuario;
	private InputStreamReader isr;
	private BufferedReader reader;
	private PrintWriter writer;
	private Socket socket;
	private SSLSocket socketSSL;
	private int port = 6789;
	private int portSSL = 5678;
	
	private JTextField tfHost;
	private JTextField tfUsuario;
	private JCheckBox ssl;
	
	public static void main(String[] args) {
		new Cliente().run();
	}
	
	public void run() {
		
		JFrame frameChat = new JFrame("ChatSSL");
		JPanel panelMensagens = new JPanel();
		panelMensagens.setBorder(new TitledBorder(null, "Mensagens", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		JPanel panelConexao = new JPanel();
		panelConexao.setBorder(new TitledBorder(null, "Conexão", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		
		txtMensagens = new JTextArea(20, 50);
		txtMensagens.setLineWrap(true);
		txtMensagens.setWrapStyleWord(true);
		txtMensagens.setEditable(false);
		
		JScrollPane scrollMensagens = new JScrollPane(txtMensagens, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		panelMensagens.add(scrollMensagens);
		
		frameChat.getContentPane().add(BorderLayout.CENTER, panelMensagens);
		
		
		frameChat.getContentPane().add(panelConexao, BorderLayout.NORTH);
		
		JLabel lblUsurio = new JLabel("Usuário:");
		panelConexao.add(lblUsurio);
		
		tfUsuario = new JTextField();
		panelConexao.add(tfUsuario);
		tfUsuario.setColumns(10);
		
		JLabel lblHost = new JLabel("Host:");
		panelConexao.add(lblHost);
		
		tfHost = new JTextField();
		panelConexao.add(tfHost);
		tfHost.setColumns(10);
		
		JButton btnConectar = new JButton("Conectar");
		btnConectar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				conectar(tfHost.getText());
			}
		});
		
		ssl = new JCheckBox("SSL");
		panelConexao.add(ssl);
		panelConexao.add(btnConectar);
		
		JPanel panelEnviar = new JPanel();
		panelEnviar.setBorder(new TitledBorder(null, "Enviar mensagem", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		frameChat.getContentPane().add(panelEnviar, BorderLayout.SOUTH);
		
		txtMensagemUsuario = new JTextField(20);
		panelEnviar.add(txtMensagemUsuario);
		JButton btnEnviar = new JButton("Enviar");
		panelEnviar.add(btnEnviar);
		btnEnviar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				enviarMensagem();
			}
		});
		
		frameChat.pack();
		frameChat.setVisible(true);
		frameChat.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frameChat.setLocationRelativeTo(null);
		
		frameChat.addWindowListener(new WindowListener() {
			
			@Override
			public void windowClosing(WindowEvent e) {
				if ( JOptionPane.showConfirmDialog(null, "Deseja sair do chat?", "Sair", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION ) {
					try {
						writer.println("[sair]");
						writer.flush();
						socket.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					} finally {
						System.exit(0);
					}
				}
			}

			@Override
			public void windowActivated(WindowEvent e) {}

			@Override
			public void windowClosed(WindowEvent e) {}

			@Override
			public void windowDeactivated(WindowEvent e) {}

			@Override
			public void windowDeiconified(WindowEvent e) {}

			@Override
			public void windowIconified(WindowEvent e) {}

			@Override
			public void windowOpened(WindowEvent e) {}
			
		});
		
	}

	protected void enviarMensagem() {
			
		if (txtMensagemUsuario.getText().equals("")) {
			return;
		}
		
		writer.println(txtMensagemUsuario.getText());
		writer.flush();
		
		txtMensagemUsuario.setText("");
		txtMensagemUsuario.requestFocus();
		
	}

	protected void conectar(String host) {

		try {
			
			if (ssl.isSelected()) {
				
				KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
				ks.load(new FileInputStream("src/br/ufac/chatssl/cert.keys"), "ufac2015".toCharArray());

				KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				kmf.init(ks, "ufac2015".toCharArray());

				TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); 
				tmf.init(ks);

				SSLContext sc = SSLContext.getInstance("SSL"); 
				TrustManager[] trustManagers = tmf.getTrustManagers(); 
				sc.init(kmf.getKeyManagers(), trustManagers, null); 

				SSLSocketFactory ssf = sc.getSocketFactory(); 
				socketSSL = (SSLSocket) ssf.createSocket(host, portSSL);
				socketSSL.startHandshake();
				
				isr = new InputStreamReader(socketSSL.getInputStream());
				reader = new BufferedReader(isr);
				writer = new PrintWriter(socketSSL.getOutputStream());
				
			} else {
				
				socket = new Socket(host, port);
				
				isr = new InputStreamReader(socket.getInputStream());
				reader = new BufferedReader(isr);
				writer = new PrintWriter(socket.getOutputStream());
				
			}
			
			writer.println(tfUsuario.getText());
			writer.flush();
			
			txtMensagens.append("Servidor encontrado, conexão estabilizada!" + "\n");
			
		} catch (Exception e) {
			
			JOptionPane.showMessageDialog(null, "Conexão com o host '" + host + "' falhou.", "ERRO", JOptionPane.ERROR_MESSAGE);
			System.out.println(e.getMessage());
			
		}
		
		Thread atualizadorMensagens = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				String mensagem;
				
				try {
					while ((mensagem = reader.readLine()) != null) {
						txtMensagens.append(mensagem + "\n");
					}
				} catch (IOException e) {
					JOptionPane.showMessageDialog(null, "Falha na comunição.", "ERRO", JOptionPane.ERROR_MESSAGE);
					System.out.println(e.getMessage());
				}
				
			}
			
		});
		
		atualizadorMensagens.start();
		
	}
	
}