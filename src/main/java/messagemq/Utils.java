package messagemq;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class Utils {
    private static String target = "";
    private static String group = "";

    public static void safePrintln(String s) {
        synchronized (System.out) {
            System.out.println(s);
        }
    }

    public static void safePrint(String s) {
        synchronized (System.out) {
            System.out.print(s);
        }
    }

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static void setTarget(String message) {
        target = message.substring(1).trim();
    }

    public static String getTarget() {
        return target;
    }

    public static void clearTarget() {
        target = "";
    }

    public static void setGroup(String message) {
        group = message.substring(1).trim();
    }

    public static String getGroup() {
        return group;
    }

    public static void clearGroup() {
        group = "";
    }

    public static void printPrompt() {
        safePrint(getTarget().isEmpty() ? getGroup() + ">> " : getTarget() + ">> ");
    }

    public static void listUsersInGroup(String groupName) {

        try {
            String apiUrl = "http://localhost:15672/api/exchanges/%2F/" + groupName + "/bindings/source";
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization",
                    "Basic " + Base64.getEncoder().encodeToString("guest:guest".getBytes()));

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Parse the JSON response to get the list of users (queues)
            JSONArray bindings = new JSONArray(response.toString());
            List<String> users = new ArrayList<>();
            for (int i = 0; i < bindings.length(); i++) {
                JSONObject binding = bindings.getJSONObject(i);
                String destination = binding.getString("destination");
                users.add(destination);
            }

            safePrintln("Usuários no grupo '" + groupName + "': " + String.join(", ", users));
        } catch (Exception e) {
            safePrintln("Erro ao listar usuários do grupo '" + groupName + "'.");
            e.printStackTrace();
        }
    }

    public static void listGroups(String currentUser) {
        try {
            String apiUrl = "http://localhost:15672/api/queues/%2F/" + currentUser + "/bindings";
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization",
                    "Basic " + Base64.getEncoder().encodeToString("guest:guest".getBytes()));

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Parse the JSON response to get the list of groups (exchanges)
            JSONArray bindings = new JSONArray(response.toString());
            List<String> groups = new ArrayList<>();
            for (int i = 0; i < bindings.length(); i++) {
                JSONObject binding = bindings.getJSONObject(i);
                String source = binding.getString("source");
                if (!source.isEmpty()) {
                    groups.add(source);
                }
            }

            safePrintln("Grupos dos quais você faz parte: " + String.join(", ", groups));
        } catch (Exception e) {
            safePrintln("Erro ao listar grupos do usuário.");
            e.printStackTrace();
        }
    }
}
