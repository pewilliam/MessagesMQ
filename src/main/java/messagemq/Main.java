package messagemq;

import com.google.protobuf.ByteString;
import com.rabbitmq.client.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Main {
    private static String target = "";
    private static String group = "";
    private static String currentUser = "";
    private static String message;

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
            System.out.print("\033[H\033[2J");
            System.out.println("Usuário inválido. Por favor, tente novamente.");
            System.out.print("Usuário: ");
            currentUser = sc.nextLine();
        }

        // Declara a fila para o usuário atual
        channel.queueDeclare(currentUser, false, false, false, null);
        safePrintln("\nLogado com sucesso!");

        safePrint(">> ");

        // Define o consumidor para ouvir mensagens
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                    byte[] body) throws IOException {
                MessageProto.Mensagem msg = MessageProto.Mensagem.parseFrom(body);

                if (msg.getEmissor().equals(currentUser)) {
                    return;
                }

                String prefix = msg.getGrupo().isEmpty() ? "" : "#" + msg.getGrupo();
                safePrintln("\n(" + msg.getData() + " às " + msg.getHora() + ") " + msg.getEmissor() + prefix + " diz: "
                        + new String(msg.getConteudo().getCorpo().toByteArray(), StandardCharsets.UTF_8));
                safePrint(target.isEmpty() ? group + ">> " : target + ">> ");
            }
        };

        channel.basicConsume(currentUser, true, consumer);

        while (true) {
            // Recebe a mensagem do usuário
            message = sc.nextLine();

            if (message.toLowerCase().equals("sair")) {
                break;
            } else if (message.startsWith("!")) {
                commandTreatment(message, channel);
                safePrint(">> ");
                continue;
            } else {
                messageTreatment(message, channel);
            }
        }

        sc.close();
        channel.close();
        connection.close();
    }

    private static void safePrintln(String s) {
        synchronized (System.out) {
            System.out.println(s);
        }
    }

    private static void safePrint(String s) {
        synchronized (System.out) {
            System.out.print(s);
        }
    }

    private static void setTarget(String message) {
        target = message.substring(1).trim();
    }

    private static void clearTarget() {
        target = "";
    }

    private static void setGroup(String message) {
        group = message.substring(1).trim();
    }

    private static void clearGroup() {
        group = "";
    }

    private static void messageTreatment(String message, Channel channel) throws IOException {
        if (message.startsWith("@")) {
            clearGroup();
            setTarget(message);
            safePrint(target + ">> ");
        } else if (message.startsWith("#")) {
            clearTarget();
            setGroup(message);
            safePrint(group + ">> ");
        } else {
            if (group.isEmpty() && target.isEmpty()) {
                safePrintln("Por favor, defina um destinatário ou grupo usando @nome ou #grupo.");
                safePrint(">> ");
                return;
            }

            MessageProto.Mensagem.Builder mensagem = MessageProto.Mensagem.newBuilder()
                    .setData(new SimpleDateFormat("dd/MM/yyyy").format(new Date()))
                    .setHora(new SimpleDateFormat("HH:mm:ss").format(new Date()))
                    .setEmissor(currentUser)
                    .setGrupo(group)
                    .setConteudo(MessageProto.Conteudo.newBuilder()
                            .setTipo("text/plain")
                            .setCorpo(ByteString.copyFrom(message.getBytes(StandardCharsets.UTF_8))) // Conversão do
                                                                                                     // byte[] para
                                                                                                     // ByteString
                            .build());

            byte[] msgBytes = mensagem.build().toByteArray();

            if (!group.isEmpty()) {
                channel.basicPublish(group, "", null, msgBytes);
            } else {
                channel.basicPublish("", target, null, msgBytes);
            }

            safePrint(target.isEmpty() ? group + ">> " : target + ">> ");
        }
    }

    private static void commandTreatment(String message, Channel channel) throws IOException {
        try {
            if (message.startsWith("!addGroup")) {
                String groupName = message.split(" ")[1];
                channel.exchangeDeclare(groupName, "fanout");

                // Adiciona o próprio usuário ao grupo
                channel.queueBind(currentUser, groupName, "");
                safePrintln("Grupo '" + groupName + "' criado e você foi adicionado.");
            } else if (message.startsWith("!addUser")) {
                String[] parts = message.split(" ");
                String user = parts[1];
                String groupName = parts[2];

                channel.queueBind(user, groupName, "");
                safePrintln("Usuário '" + user + "' foi adicionado ao grupo '" + groupName + "'.");
            } else if (message.startsWith("!delFromGroup")) {
                String[] parts = message.split(" ");
                String user = parts[1];
                String groupName = parts[2];

                channel.queueUnbind(user, groupName, "");
                safePrintln("Usuário '" + user + "' foi removido do grupo '" + groupName + "'.");
            } else if (message.startsWith("!removeGroup")) {
                String groupName = message.split(" ")[1];
                channel.exchangeDelete(groupName);
                safePrintln("Grupo '" + groupName + "' foi removido.");
            } else {
                safePrintln("Comando não existe.");
            }
        } catch (Exception e) {
            safePrintln("Erro ao executar o comando. Verifique se os parâmetros estão corretos.");
        }
    }
}
