package Socket;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {
    private static final String HOST = "localhost";
    private static final int PUERTO = 5005;
    private static final String MSG_SALIDA = "SALIR";

    public static void main(String[] args) {
        System.out.println("=== CLIENTE CHAT ===");
        System.out.println("Conectando al servidor " + HOST + ": " + PUERTO + "...");

        try (
                Socket socket = new Socket(HOST, PUERTO);
                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter salida = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream()), true);
                Scanner teclado = new Scanner(System.in)
        ) {
            System.out.println("Conexion establecida!\n");

            // Hilo que escucha al servidor
            Thread recibir = new Thread(() -> {
                try {
                    String mensajeServidor;
                    while ((mensajeServidor = entrada.readLine()) != null) {
                        System.out.println(mensajeServidor);
                    }
                } catch (IOException e) {
                    System.out.println("Conexión cerrada.");
                }
            });

            recibir.start();

            // Hilo que envía mensajes
            while (true) {
                String mensajeUsuario = teclado.nextLine();
                salida.println(mensajeUsuario);

                if (mensajeUsuario.equalsIgnoreCase("SALIR")) {
                    break;
                }
            }

                System.out.println();
            } catch (ConnectException e) {
            System.err.println("[ERROR] No se pudo conectar. Asegurate de que el Servidor este corriendo.");
            } catch (IOException e) {
                System.err.println("[ERROR] Problema de conexion: " + e.getMessage());
            }
    }
}
