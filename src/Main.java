import file_database.Database;
import file_database.File_interface;
import gui.CentralTerminal_panel;
import gui.Godzilla_frame;
import network.Server;
import javax.crypto.*;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Vector;

public class Main {
    public static void main(String args[]) throws IllegalBlockSizeException, IOException, BadPaddingException, InterruptedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException {
        Vector<Image> icons = new Vector<>();

        icons.add(new ImageIcon(Main.class.getResource("images/icon_16.png")).getImage());
        icons.add(new ImageIcon(Main.class.getResource("images/icon_32.png")).getImage());
        icons.add(new ImageIcon(Main.class.getResource("images/icon_64.png")).getImage());
        icons.add(new ImageIcon(Main.class.getResource("images/icon_128.png")).getImage());

        Godzilla_frame.init().setIconImages(icons);
        File_interface.init_next();

        Runtime.getRuntime().addShutdownHook(shut_down);
    }

    private static Thread shut_down = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                Server.disconnect(true); //se è ancora connesso ad un server si disconnette

                if (Database.DEBUG) { CentralTerminal_panel.terminal_write("salvo le credenziali MClient - ", false); }
                save_MClient_key();
                if (Database.DEBUG) { CentralTerminal_panel.terminal_write("done\n salvo la server list - ", false); }
                save_server_list();

                if (Database.DEBUG) { CentralTerminal_panel.terminal_write("done\n salvo la cronologia del terminale", false); }
                File_interface.overwrite_file(File_interface.TERMINAL_LOG, CentralTerminal_panel.get_terminal_log());

                File_interface.close();
            } catch (IllegalBlockSizeException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (BadPaddingException e) {
                throw new RuntimeException(e);
            }
        }

        private void save_server_list() throws IllegalBlockSizeException, IOException, BadPaddingException {
            String new_file_txt = "";
            for (int i = 0; i < Database.serverList.size(); i++) {
                String key = (String) Database.serverList.keySet().toArray()[i];
                new_file_txt += key + ";" + Database.serverList.get(key) + "\n";
            }

            if (!new_file_txt.equals("")) {
                File_interface.overwrite_file(File_interface.SERVER_LIST, new_file_txt);
            }
        }

        private void save_MClient_key() throws IllegalBlockSizeException, IOException, BadPaddingException {
            String new_file_txt = "";
            for (int i = 0; i < Database.MClientKey.size(); i++) {
                String key = (String) Database.serverList.keySet().toArray()[i];
                new_file_txt += key + "\00{" + Database.MClientKey.get(key).el1 + "\00;" + Database.MClientKey.get(key).el2 + "\00}\n";
            }

            if (!new_file_txt.equals("")) {
                File_interface.overwrite_file(File_interface.MCLIENT_KEY, new_file_txt);
            }
        }
    });
}