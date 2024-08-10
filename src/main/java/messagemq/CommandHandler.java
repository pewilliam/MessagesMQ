package messagemq;

import com.rabbitmq.client.Channel;

import java.io.IOException;

public class CommandHandler {
    private Channel channel;
    private String currentUser;

    public CommandHandler(Channel channel, String currentUser) {
        this.channel = channel;
        this.currentUser = currentUser;
    }

    public void handleCommand(String command) throws IOException {
        try {
            if (command.startsWith("!addGroup")) {
                String groupName = command.split(" ")[1];
                channel.exchangeDeclare(groupName, "fanout");

                // Adiciona o próprio usuário ao grupo
                channel.queueBind(currentUser, groupName, "");
                Utils.safePrintln("Grupo '" + groupName + "' criado e você foi adicionado.");
            } else if (command.startsWith("!addUser")) {
                String[] parts = command.split(" ");
                String user = parts[1];
                String groupName = parts[2];

                channel.queueBind(user, groupName, "");
                Utils.safePrintln("Usuário '" + user + "' foi adicionado ao grupo '" + groupName + "'.");
            } else if (command.startsWith("!delFromGroup")) {
                String[] parts = command.split(" ");
                String user = parts[1];
                String groupName = parts[2];

                channel.queueUnbind(user, groupName, "");
                Utils.safePrintln("Usuário '" + user + "' foi removido do grupo '" + groupName + "'.");
            } else if (command.startsWith("!removeGroup")) {
                String groupName = command.split(" ")[1];
                channel.exchangeDelete(groupName);
                Utils.safePrintln("Grupo '" + groupName + "' foi removido.");
            } else {
                Utils.safePrintln("Comando não existe.");
            }
        } catch (Exception e) {
            Utils.safePrintln("Erro ao executar o comando. Verifique se os parâmetros estão corretos.");
        }
    }
}
