package messagemq;

public class Utils {
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
}
