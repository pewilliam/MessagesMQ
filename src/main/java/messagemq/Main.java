package messagemq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Main {
    private static String currentUser = "";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("peredo");
        factory.setPassword("peredo");
        factory.setVirtualHost("/");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        Scanner sc = new Scanner(System.in, StandardCharsets.UTF_8.name());

        System.out.print("User: ");
        currentUser = sc.nextLine();

        while (currentUser.isEmpty()) {
            Utils.clearScreen();
            System.out.println("Usuário inválido. Por favor, tente novamente.");
            System.out.print("Usuário: ");
            currentUser = sc.nextLine();
        }

        // Declara a fila para o usuário atual
        channel.queueDeclare(currentUser, false, false, false, null);
        Utils.safePrintln("\nLogado com sucesso!");

        MessageHandler messageHandler = new MessageHandler(channel, currentUser);
        CommandHandler commandHandler = new CommandHandler(channel, currentUser);

        messageHandler.startListening();

        while (true) {
            Utils.safePrint(">> ");
            String input = sc.nextLine();

            if (input.toLowerCase().equals("sair")) {
                break;
            } else if (input.startsWith("!")) {
                commandHandler.handleCommand(input);
            } else {
                messageHandler.handleMessage(input);
            }
        }

        sc.close();
        channel.close();
        connection.close();
    }
}
