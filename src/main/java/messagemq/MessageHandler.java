package messagemq;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.protobuf.ByteString;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class MessageHandler {
    private Channel channel;
    private String currentUser;

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
                Utils.safePrint(Utils.getTarget().isEmpty() ? Utils.getGroup() : Utils.getTarget());
            }
        };

        channel.basicConsume(currentUser, true, consumer);
    }

    public void handleMessage(String message) throws IOException {
        if (message.startsWith("@")) {
            Utils.clearGroup();
            Utils.setTarget(message);
            Utils.safePrint(Utils.getTarget());
        } else if (message.startsWith("#")) {
            Utils.clearTarget();
            Utils.setGroup(message);
            Utils.safePrint(Utils.getGroup());
        } else {
            if (Utils.getTarget().isEmpty() && Utils.getGroup().isEmpty()) {
                Utils.safePrintln("Por favor, defina um destinatário ou grupo usando @nome ou #grupo.");
                return;
            }

            MessageProto.Mensagem.Builder mensagem = MessageProto.Mensagem.newBuilder()
                    .setData(new SimpleDateFormat("dd/MM/yyyy").format(new Date()))
                    .setHora(new SimpleDateFormat("HH:mm:ss").format(new Date()))
                    .setEmissor(currentUser)
                    .setGrupo(Utils.getGroup())
                    .setConteudo(MessageProto.Conteudo.newBuilder()
                            .setTipo("text/plain")
                            .setCorpo(ByteString.copyFrom(message.getBytes(StandardCharsets.UTF_8)))
                            .build());

            byte[] msgBytes = mensagem.build().toByteArray();

            if (!Utils.getGroup().isEmpty()) {
                channel.basicPublish(Utils.getGroup(), "", null, msgBytes);
            } else {
                channel.basicPublish("", Utils.getTarget(), null, msgBytes);
            }

            Utils.safePrint(Utils.getTarget().isEmpty() ? Utils.getGroup() : Utils.getTarget());
        }
    }
}
