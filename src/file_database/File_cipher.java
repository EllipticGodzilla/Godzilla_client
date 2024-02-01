package file_database;

import gui.CentralTerminal_panel;
import gui.StringVectorOperator;
import gui.TempPanel;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

public class File_cipher {
    private static Cipher encrypter = null;
    private static Cipher decrypter = null;

    protected static void init() throws InterruptedException {
        if (is_empty(Database.FileCypherKey_test)) { //se non è stata impostata una password
            throw new RuntimeException("non è specificata una password per cifrare i file");
        } else {
            TempPanel.request_string("inserisci la chiave per i database: ", AES_init); //chiede la password
        }
    }

    private static boolean is_empty(byte[] arr) { //se l'array contiene solo zero è considerato vuoto e ritorna true, altrimenti false
        for (byte b : arr) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    private static StringVectorOperator AES_init = new StringVectorOperator() {
        @Override
        public void success() { //ha ricevuto un password, controlla sia giusta ed inizializza i cipher
            try {
                if (Database.DEBUG) { CentralTerminal_panel.terminal_write("password ricevuta, ", false); }
                MessageDigest md = MessageDigest.getInstance("SHA3-512");
                byte[] hash = md.digest(input.elementAt(0).getBytes());

                if (Arrays.compare(Arrays.copyOfRange(hash, 32, 64), Database.FileCypherKey_test) != 0) { //se la password inserita è sbagliata
                    fail();
                } else {
                    if (Database.DEBUG) { CentralTerminal_panel.terminal_write("password corretta, ", false); }
                    decript_files(hash);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void fail() {
            if (Database.DEBUG) { CentralTerminal_panel.terminal_write("la password inserita non è corretta, o è stato premuto cancella\n", true); }
            TempPanel.show_msg("password non corretta, riprovare", error_init, false);
        }
    };

    private static void decript_files(byte[] key_hash) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeySpecException, InterruptedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        //utilizza i primi 32byte dell'hash come key ed iv per inizializzare AES
        byte[] key_bytes = Arrays.copyOf(key_hash, 16);
        byte[] iv_bytes = Arrays.copyOfRange(key_hash, 16, 32);

        SecretKey key = new SecretKeySpec(key_bytes, "AES");
        IvParameterSpec iv = new IvParameterSpec(iv_bytes);

        //inzializza encrypter e decrypter con key ed iv appena calcolati
        encrypter = Cipher.getInstance("AES/CBC/PKCS5Padding");
        decrypter = Cipher.getInstance("AES/CBC/PKCS5Padding");

        encrypter.init(Cipher.ENCRYPT_MODE, key, iv);
        decrypter.init(Cipher.DECRYPT_MODE, key, iv);
        if (Database.DEBUG) { CentralTerminal_panel.terminal_write("inizializzati i cipher\n", false); }

        if (Database.DEBUG) { CentralTerminal_panel.terminal_write("leggo il file con la chiave pubblica della CA\n", false); }
        File_interface.init_ca_public_key();
    }

    private static StringVectorOperator error_init = new StringVectorOperator() {
        @Override
        public void success() {
            TempPanel.request_string("inserisci la chiave per i database: ", AES_init); //richiede la password
        }

        @Override
        public void fail() {} //essendo un messaggio non può "fallire"
    };

    protected static byte[] decrypt(byte[] txt) throws IllegalBlockSizeException, BadPaddingException {
        return decrypter.doFinal(txt);
    }

    protected static byte[] crypt(byte[] txt) throws IllegalBlockSizeException, BadPaddingException {
        return encrypter.doFinal(txt);
    }
}
