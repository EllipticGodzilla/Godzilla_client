import file_database.Database;
import file_database.File_interface;
import gui.CentralTerminal_panel;
import gui.Godzilla_frame;
import network.Server;
import javax.crypto.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Vector;

public class Main {
    public static void main(String args[]) throws IOException, InterruptedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException, URISyntaxException {
        JFrame main_frame = Godzilla_frame.init();
        File_interface.init_file_cipher_key();

        //imposta l'icona del main frame
        Vector<Image> icons = new Vector<>();

        icons.add(new ImageIcon(File_interface.jar_path + "/images/icon_16.png").getImage());
        icons.add(new ImageIcon(File_interface.jar_path + "/images/icon_32.png").getImage());
        icons.add(new ImageIcon(File_interface.jar_path + "/images/icon_64.png").getImage());
        icons.add(new ImageIcon(File_interface.jar_path + "/images/icon_128.png").getImage());

        main_frame.setIconImages(icons);

        //aggiunge un shutdown hook
        Runtime.getRuntime().addShutdownHook(shut_down);
    }

    private static Thread shut_down = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                Server.disconnect(true); //se è ancora connesso ad un server si disconnette

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
    });
}