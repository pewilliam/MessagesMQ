package messagemq;

import com.google.protobuf.ByteString;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MessageHandler {
    private Channel channel;
    private String currentUser;
    private String target = "";
    private String group = "";

    public MessageHandler(Channel channel, String currentUser) {
        this.channel = channel;
        this.currentUser = currentUser;
    }

    public void startListening() throws IOException {
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                    byte[] body) throws IOException {
                MessageProto.Mensagem msg = MessageProto.Mensagem.parseFrom(body);

                if (msg.getEmissor().equals(currentUser)) {
                    return;
                }

                String prefix = msg.getGrupo().isEmpty() ? "" : "#" + msg.getGrupo();
                Utils.safePrintln(
                        "\n(" + msg.getData() + " às " + msg.getHora() + ") " + msg.getEmissor() + prefix + " diz: "
                                + new String(msg.getConteudo().getCorpo().toByteArray(), StandardCharsets.UTF_8));
                Utils.safePrint(target.isEmpty() ? group + ">> " : target + ">> ");
            }
        };

        channel.basicConsume(currentUser, true, consumer);
    }

    public void handleMessage(String message) throws IOException {
        if (message.startsWith("@")) {
            clearGroup();
            setTarget(message);
            Utils.safePrint(target + ">> ");
        } else if (message.startsWith("#")) {
            clearTarget();
            setGroup(message);
            Utils.safePrint(group + ">> ");
        } else {
            if (group.isEmpty() && target.isEmpty()) {
                Utils.safePrintln("Por favor, defina um destinatário ou grupo usando @nome ou #grupo.");
                return;
            }

            MessageProto.Mensagem.Builder mensagem = MessageProto.Mensagem.newBuilder()
                    .setData(new SimpleDateFormat("dd/MM/yyyy").format(new Date()))
                    .setHora(new SimpleDateFormat("HH:mm:ss").format(new Date()))
                    .setEmissor(currentUser)
                    .setGrupo(group)
                    .setConteudo(MessageProto.Conteudo.newBuilder()
                            .setTipo("text/plain")
                            .setCorpo(ByteString.copyFrom(message.getBytes(StandardCharsets.UTF_8)))
                            .build());

            byte[] msgBytes = mensagem.build().toByteArray();

            if (!group.isEmpty()) {
                channel.basicPublish(group, "", null, msgBytes);
            } else {
                channel.basicPublish("", target, null, msgBytes);
            }

            Utils.safePrint(target.isEmpty() ? group + ">> " : target + ">> ");
        }
    }

    private void setTarget(String message) {
        target = message.substring(1).trim();
    }

    private void clearTarget() {
        target = "";
    }

    private void setGroup(String message) {
        group = message.substring(1).trim();
    }

    private void clearGroup() {
        group = "";
    }
}
