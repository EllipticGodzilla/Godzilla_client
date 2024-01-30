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
        if (is_empty(Database.FileCypherKey_test)) { //se non è ancora stata impostata una password, o se si vuole resettare
            TempPanel.request_string("inserisci una password per i database: ", psw_gen);
        } else {
            TempPanel.request_string("inserisci la chiave per i database: ", AES_init); //richiede la password
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

    private static StringVectorOperator psw_gen = new StringVectorOperator() {
        @Override
        public void success() { //ha ricevuto una password per cifrare i file, la salva e inizia a cifrare i file utilizzando questa
            if (Database.DEBUG) { CentralTerminal_panel.terminal_write("ricevuta password per i file, ", false); }
            try {
                MessageDigest md = MessageDigest.getInstance("SHA3-512");
                byte[] hash = md.digest(input.elementAt(0).getBytes());

                //copia gli ultimi 32 byte nel file FileCipherKey.dat
                byte[] test_byte = Arrays.copyOfRange(hash, 32, 64);
                File_interface.overwrite_file(
                        File_interface.FILE_CIPHER_KEY,
                        Base64.getEncoder().encodeToString(test_byte)
                );
                if (Database.DEBUG) { CentralTerminal_panel.terminal_write("salvata la password nel database, ", false); }

                //utilizza i primi 32 byte dell'hash per key ed iv
                byte[] key_bytes = Arrays.copyOf(hash, 16);
                byte[] iv_bytes = Arrays.copyOfRange(hash, 16, 32);

                SecretKey key = new SecretKeySpec(key_bytes, "AES");
                IvParameterSpec iv = new IvParameterSpec(iv_bytes);

                //inzializza encrypter e decrypter con key ed iv appena calcolati
                encrypter = Cipher.getInstance("AES/CBC/PKCS5Padding");
                decrypter = Cipher.getInstance("AES/CBC/PKCS5Padding");

                encrypter.init(Cipher.ENCRYPT_MODE, key, iv);
                decrypter.init(Cipher.DECRYPT_MODE, key, iv);
                if (Database.DEBUG) { CentralTerminal_panel.terminal_write("inizializzati i cipher\n", false); }

                File_interface.init_next();

            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (InvalidAlgorithmParameterException e) {
                throw new RuntimeException(e);
            } catch (NoSuchPaddingException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeyException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (BadPaddingException e) {
                throw new RuntimeException(e);
            } catch (IllegalBlockSizeException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeySpecException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void fail() {
            TempPanel.show_msg("è necessaria una password per cifrare i database", error_psw, false);
        }
    };

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
                    //utilizza i primi 32byte dell'hash come key ed iv per inizializzare AES
                    byte[] key_bytes = Arrays.copyOf(hash, 16);
                    byte[] iv_bytes = Arrays.copyOfRange(hash, 16, 32);

                    SecretKey key = new SecretKeySpec(key_bytes, "AES");
                    IvParameterSpec iv = new IvParameterSpec(iv_bytes);

                    //inzializza encrypter e decrypter con key ed iv appena calcolati
                    encrypter = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    decrypter = Cipher.getInstance("AES/CBC/PKCS5Padding");

                    encrypter.init(Cipher.ENCRYPT_MODE, key, iv);
                    decrypter.init(Cipher.DECRYPT_MODE, key, iv);
                    if (Database.DEBUG) { CentralTerminal_panel.terminal_write("inizializzati i cipher\n", false); }

                    File_interface.init_next();
                }

            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (NoSuchPaddingException e) {
                throw new RuntimeException(e);
            } catch (InvalidAlgorithmParameterException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeyException e) {
                throw new RuntimeException(e);
            } catch (IllegalBlockSizeException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (BadPaddingException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeySpecException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void fail() {
            if (Database.DEBUG) { CentralTerminal_panel.terminal_write("la password inserita non è corretta, o è stato premuto cancella\n", true); }
            TempPanel.show_msg("password non corretta, riprovare", error_init, false);
        }
    };

    private static StringVectorOperator error_psw = new StringVectorOperator() {
        @Override
        public void success() {
            TempPanel.request_string("inserisci una password per i database: ", psw_gen);
        }

        @Override
        public void fail() {} //essendo un messaggio non può "fallire"
    };

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
