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

                channel.queueBind(currentUser, groupName, "");
                Utils.safePrintln("Grupo '" + groupName + "' criado e você foi adicionado.");
                Utils.printPrompt();
            } else if (command.startsWith("!addUser")) {
                String[] parts = command.split(" ");
                String user = parts[1];
                String groupName = parts[2];

                channel.queueBind(user, groupName, "");
                Utils.safePrintln("Usuário '" + user + "' foi adicionado ao grupo '" + groupName + "'.");
                Utils.printPrompt();
            } else if (command.startsWith("!delFromGroup")) {
                String[] parts = command.split(" ");
                String user = parts[1];
                String groupName = parts[2];

                channel.queueUnbind(user, groupName, "");
                Utils.safePrintln("Usuário '" + user + "' foi removido do grupo '" + groupName + "'.");
                Utils.printPrompt();
            } else if (command.startsWith("!removeGroup")) {
                String groupName = command.split(" ")[1];
                channel.exchangeDelete(groupName);
                Utils.safePrintln("Grupo '" + groupName + "' foi removido.");
                Utils.printPrompt();
            } else if (command.startsWith("!listUsers")) {
                String groupName = command.split(" ")[1];
                Utils.listUsersInGroup(groupName);
                Utils.printPrompt();
            } else if (command.startsWith("!listGroups")) {
                Utils.listGroups(currentUser);
                Utils.printPrompt();
            } else {
                Utils.safePrintln("Comando não existe.");
                Utils.printPrompt();
            }
        } catch (Exception e) {
            Utils.safePrintln("Erro ao executar o comando. Verifique se os parâmetros estão corretos.");
            Utils.printPrompt();
        }
    }
}
