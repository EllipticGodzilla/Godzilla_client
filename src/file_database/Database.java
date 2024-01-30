package file_database;

import javax.crypto.Cipher;
import java.util.LinkedHashMap;
import java.util.Map;

public class Database {
    public static Map<String, String> serverList = new LinkedHashMap<>(); //lega il nome del server al suo indirizzo (non all'ip)
    public static Map<String, Pair<byte[], byte[]>> MClientKey = new LinkedHashMap<>(); //lega l'indirizzo del server con key ed iv scelti per la connessione (MClient)
    public static byte[] FileCypherKey_test = new byte[32]; //ultimi 32 byte dell hash SHA3-512 della chiave utilizzata per cifrare i file
    public static Cipher CAPublicKey; //decripta utilizzando la chiave pubblica della CA
    public static boolean DEBUG = true;
}
