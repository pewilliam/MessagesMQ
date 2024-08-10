package messagemq;

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
}
