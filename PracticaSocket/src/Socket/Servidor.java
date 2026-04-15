package Socket;

import java.io.*;
import java.net.*;
import java.util.*;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;


public class Servidor {

    private static final int PUERTO = 5005;
    private static final String MSG_SALIDA = "SALIR";

    private static Map<String, ClienteHandler> clientes = new ConcurrentHashMap<>();

    public static void main(String[] args) {

        System.out.println("Esperando conexiones en el puerto " + PUERTO + "...\n");

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) { //escucha al servidor

            // el servidor acepta una conexión y la atiende, despues espera otra
            while (true) {

                Socket clienteSocket = serverSocket.accept();

                System.out.println("[CONEXION] Cliente conectado desde: " +
                        clienteSocket.getInetAddress().getHostAddress());

                new Thread(new ClienteHandler(clienteSocket)).start();
            }

        } catch (IOException e) {
            System.err.println("[ERROR] No se pudo iniciar el servidor: " + e.getMessage());
        }
    }

    static class ClienteHandler implements Runnable {
        private Socket socket;
        private BufferedReader entrada;
        private PrintWriter salida;
        String nombre;

        public ClienteHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                //Crea canales de entrada y salida
                entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                salida = new PrintWriter(socket.getOutputStream(), true);

                //Pide nombre al usuario
                salida.println("Ingrese su nombre: ");
                String nombreBase = entrada.readLine();

                //Genera un nombre único
                nombre = generarNombreUnico(nombreBase);

                //Agrega a la lista de clientes
                clientes.put(nombre, this);

                //Mensaje de bienvenida
                salida.println("Bienvenido " + nombre);
                mostrarMenu();

                //Loop de mensajes
                String mensaje;
                while ((mensaje = entrada.readLine()) != null) {
                    log(nombre + ": " + mensaje);

                    if (mensaje.equalsIgnoreCase("SALIR")){
                        salida.println("Adiós!");
                        break;
                    }

                    procesarComando(mensaje);
                }
            } catch (IOException e) {
                log("Error con cliente " + nombre);
            } finally {
                desconectar();
            }
        }

        private void procesarComando(String mensaje) {
            if (mensaje.startsWith("ALL ")) {
                enviarATodos(nombre + ": " + mensaje.substring(4));
            } else if (mensaje.startsWith("MSG ")) {
                String[] partes = mensaje.split(" ", 3);

                if (partes.length < 3) {
                    salida.println("Uso: MSG <usuario> <mensaje>");
                    return;
                }

                String destino = partes[1];
                String msg = partes[2];

                ClienteHandler cliente = clientes.get(destino);

                if(cliente != null) {
                    cliente.salida.println("(privado) " + nombre + ": " + msg);
                } else {
                    salida.println("Usuario no existe: " + destino);
                }
            } else if (mensaje.equalsIgnoreCase("LIST")) {
                salida.println("Conectados: " + clientes.keySet());

            } else if (mensaje.equalsIgnoreCase("TIME")) {
                salida.println("Hora: " + java.time.LocalDateTime.now());

            } else if (mensaje.startsWith("CALC ")) {
                String expr = mensaje.substring(5);
                salida.println("Resultado: " + evaluar(expr));

            } else {
                salida.println("Comando no reconocido");
            }
        }

        private void enviarATodos(String msg) {
            for(ClienteHandler c : clientes.values()) {
                c.salida.println(msg);
            }
        }

        private String generarNombreUnico(String base) {
            String nombreFinal = base;
            int i = 1;

            while (clientes.containsKey(nombreFinal)) {
                nombreFinal = base + i;
                i++;
            }
            return nombreFinal;
        }

        private double evaluar(String expr) {
            try {
                return evaluarSimple(expr);
            } catch (Exception e) {
                return Double.NaN;
            }
        }

        private double evaluarSimple(String expr) {
            expr = expr.replaceAll(" ", "");

            // primero * y /
            List<Double> numeros = new ArrayList<>();
            List<Character> operadores = new ArrayList<>();

            String num = "";
            for (char c : expr.toCharArray()) {
                if (Character.isDigit(c) || c == '.') {
                    num += c;
                } else {
                    numeros.add(Double.parseDouble(num));
                    operadores.add(c);
                    num = "";
                }
            }
            numeros.add(Double.parseDouble(num));

            // resolver * y /
            for (int i = 0; i < operadores.size(); i++) {
                if (operadores.get(i) == '*' || operadores.get(i) == '/') {
                    double a = numeros.get(i);
                    double b = numeros.get(i + 1);

                    double res = (operadores.get(i) == '*') ? a * b : a / b;

                    numeros.set(i, res);
                    numeros.remove(i + 1);
                    operadores.remove(i);
                    i--;
                }
            }

            // resolver + y -
            double resultado = numeros.get(0);
            for (int i = 0; i < operadores.size(); i++) {
                if (operadores.get(i) == '+') {
                    resultado += numeros.get(i + 1);
                } else {
                    resultado -= numeros.get(i + 1);
                }
            }

            return resultado;
        }

        private void desconectar() {
            try {
                clientes.remove(nombre);
                socket.close();
                log(nombre + " desconectado");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void mostrarMenu() {
            salida.println("Comandos disponibles: ");
            salida.println("ALL <msg>");
            salida.println("MSG <usuario> <msg>");
            salida.println("LIST");
            salida.println("TIME");
            salida.println("CALC <expr>");
            salida.println("SALIR");
        }

        private void log(String msg) {
            System.out.println("[SERVER] " + msg);
        }
    }
}
