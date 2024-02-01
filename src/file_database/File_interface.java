package file_database;

import gui.ButtonTopBar_panel;
import gui.CentralTerminal_panel;
import gui.ServerList_panel;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Pattern;
public class File_interface extends Database {
    public static String jar_path;

    private static final SecureFile ServerList; //protetto
    private static final SecureFile TerminalLog; //publico
    private static final byte[] CAPublicKey_text;
    private static final byte[] FileCipherKey_text;

    public static final int SERVER_LIST = 0;
    public static final int TERMINAL_LOG = 1;

    static { //inizializza tutti i SecureFile esterni al jar e legge il contenuto dei file interni
        try {
            jar_path = File_interface.class.getProtectionDomain().getCodeSource().getLocation().getPath(); //calcola la abs path del jar eseguito
            jar_path = jar_path.substring(0, jar_path.length() - 1); //rimuove l'ultimo /
            jar_path = jar_path.substring(0, jar_path.lastIndexOf('/')); //rimuove Godzilla.jar dalla fine della path

            ServerList = new SecureFile(jar_path + "/database/ServerList.dat", true);
            TerminalLog = new SecureFile(jar_path + "/database/TerminalLog.dat", false);
            CAPublicKey_text = File_interface.class.getClassLoader().getResourceAsStream("files/CAPublicKey.dat").readAllBytes();
            FileCipherKey_text = File_interface.class.getClassLoader().getResourceAsStream("files/FileCipherKey.dat").readAllBytes();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void init_file_cipher_key() throws InterruptedException  {
        byte[] key_test = Base64.getDecoder().decode(FileCipherKey_text); //copia i 32 byte per testare la validità della key inserita nel database
        for (int i = 0; i < key_test.length; i++) {
            FileCypherKey_test[i] = key_test[i];
        }

        if (Database.DEBUG) { CentralTerminal_panel.terminal_write("chiedo la password per decifrare i database\n", false); }
        File_cipher.init();
    }

    public static void init_ca_public_key() throws IllegalBlockSizeException, IOException, BadPaddingException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException {
        byte[] ca_key = CAPublicKey_text; //copia la public key della CA nel database

        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey key = kf.generatePublic(new X509EncodedKeySpec(ca_key));

        Database.CAPublicKey = Cipher.getInstance("RSA");
        Database.CAPublicKey.init(Cipher.DECRYPT_MODE, key);

        if (Database.DEBUG) { CentralTerminal_panel.terminal_write("leggo il file contenente la server list\n", false); }
        init_server_list();
    }

    public static void init_server_list() throws IllegalBlockSizeException, IOException, BadPaddingException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        /*
         * il testo è formattato in questo modo:
         * <nome1>;<indirizzo1>
         * <nome2>;<indirizzo2>
         * ...
         * ...
         *
         * viene diviso ad ogni ';' o '\n' ricavando un array:
         * {<nome1>, <indirizzo1>, <nome2>, <indirizzo2>, ...}
         * che poi verrà inserito nel database
         */
        String server_list = new String(ServerList.read());

        Pattern p = Pattern.compile("[;\n]");
        String[] info = p.split(server_list);

        if (info.length != 1) { //il file è vuoto, Pattern.split("") ritorna un array con un solo elemento vuoto
            for (int i = 0; i < info.length; i += 2) {
                Database.serverList.put(info[i], info[i + 1]);
            }

            if (Database.DEBUG) { CentralTerminal_panel.terminal_write("aggiorno la gui con le informazioni dei database\n", false); }
            ServerList_panel.update_gui();
            ButtonTopBar_panel.init_buttons();
        }
    }

    public static String read_file(int file_id) throws IOException, IllegalBlockSizeException, BadPaddingException {
        switch (file_id) {
            case SERVER_LIST:
                return new String(ServerList.read());

            case TERMINAL_LOG:
                return new String(TerminalLog.read());

            default:
                throw new RuntimeException("impossibile trovare il file con id: " + file_id);
        }
    }

    public static void append_to_file(int file_id, String txt) throws IOException, IllegalBlockSizeException, BadPaddingException {
        switch (file_id) {
            case SERVER_LIST:
                ServerList.append(txt);
                break;

            case TERMINAL_LOG:
                TerminalLog.append(txt);
                break;

            default:
                throw new RuntimeException("impossibile trovare il file con id: " + file_id);
        }
    }

    public static void overwrite_file(int file_id, String txt) throws IOException, IllegalBlockSizeException, BadPaddingException {
        switch (file_id) {
            case SERVER_LIST:
                ServerList.replace(txt);
                break;

            case TERMINAL_LOG:
                TerminalLog.replace(txt);
                break;

            default:
                throw new RuntimeException("impossibile trovare il file con id: " + file_id);
        }
    }

    public static void close() throws IOException {
        ServerList.close();
        TerminalLog.close();
    }

}

class SecureFile {
    private final boolean IS_PROTECTED;
    private FileOutputStream fos;

    private final File F;

    public SecureFile(String pathname, boolean is_prot) throws FileNotFoundException {
        F = new File(pathname);

        this.IS_PROTECTED = is_prot;

        fos = new FileOutputStream(F, true);
    }

    protected byte[] read() throws IOException, IllegalBlockSizeException, BadPaddingException {
        FileInputStream fis = new FileInputStream(F);
        byte[] txt = fis.readAllBytes();
        fis.close();

        if (IS_PROTECTED) {
            txt = File_cipher.decrypt(txt);
        }

        return txt;
    }

    protected void append(String txt) throws IOException, IllegalBlockSizeException, BadPaddingException {
        if (IS_PROTECTED) {
            String file_txt = read() + txt;
            replace(file_txt);
        } else {
            fos.write(txt.getBytes());
        }
    }

    protected void replace(String txt) throws IOException, IllegalBlockSizeException, BadPaddingException {
        clear_file();
        byte[] txt_b = txt.getBytes();

        if (IS_PROTECTED) {
            txt_b = File_cipher.crypt(txt_b);
        }

        fos.write(txt_b);
    }

    protected void replace(byte[] txt) throws IOException, IllegalBlockSizeException, BadPaddingException {
        clear_file();

        if (IS_PROTECTED) {
            txt = File_cipher.crypt(txt);
        }

        fos.write(txt);
    }

    private void clear_file() throws IOException {
        new FileOutputStream(F, false).close();
    }

    protected void close() throws IOException {
        fos.close();
    }

}