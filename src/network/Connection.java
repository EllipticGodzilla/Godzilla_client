package network;

import file_database.Database;
import gui.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Connection {
    private static Socket sck;
    private static BufferedOutputStream output;
    private static BufferedInputStream input;

    private static String paired_usr;

    private static AESCipher cipher = null;
    private static SecureRandom random = new SecureRandom();

    private static boolean secure = false;
    private static boolean dynamic = false;

    public static boolean init(String ip, int port) throws IOException {
        if (Connection.isClosed()) {
            sck = new Socket(ip, port);

            output = new BufferedOutputStream(sck.getOutputStream());
            input = new BufferedInputStream(sck.getInputStream());

            return true;
        }
        else {
            return false;
        }
    }

    public static synchronized void write(String msg) {
        write(msg.getBytes());
    }

    public static synchronized void write(byte[] msg) { //non si aspetta nessuna risposta e non invia una risposta
        write(msg, null);
    }

    public static synchronized void write(byte[] msg, On_arrival action) { //non invia una risposta, e si aspetta una risposta
        byte conv_code = (action == null)? 0x00 : register_conv(action);

        write(conv_code, msg);
    }

    public static synchronized void write(byte conv_code, byte[] msg, On_arrival action) { //invia una risposta e si aspetta una risposta
        //registra la nuova azione da eseguire una volta ricevuta la risposta
        Receiver.new_conv(conv_code, action);
        write(conv_code, msg); //invia la risposta
    }

    public static synchronized void write(byte conv_code, byte[] msg) { //invia una risposta
        try {
            if (!isClosed()) { //se è effettivamente connesso a qualcuno
                if (Database.DEBUG) {
                    CentralTerminal_panel.terminal_write("invio al server: (" + new String(msg) + ") " + ((secure) ? "secure\n" : "\n"), false);
                }

                byte[] msg_prefix = concat_array(new byte[] {conv_code}, msg); //concatena conv_code e msg[]

                direct_write(msg_prefix); //se possibile cifra e invia il messaggio
            }
        } catch (IllegalBlockSizeException | IOException | BadPaddingException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    //copia dei metodi write(...) per inviare messaggi dalle mod, l'unica differenza è che prima del messaggio viene aggiunto "<nome mod>:"
    public static synchronized void mod_write(String msg) {
        write(ButtonTopBar_panel.active_mod + ":" + msg);
    }

    public static synchronized void mod_write(byte[] msg) {
        byte[] prefix = (ButtonTopBar_panel.active_mod + ":").getBytes();
        prefix = concat_array(prefix, msg); //concatena i due array prefix[] e msg[]

        write(prefix);
    }

    public static synchronized void mod_write(byte[] msg, On_arrival action) {
        byte[] prefix = (ButtonTopBar_panel.active_mod + ":").getBytes();
        prefix = concat_array(prefix, msg); //concatena i due array prefix[] e msg[]

        write(prefix, action);
    }

    public static synchronized void mod_write(byte conv_code, byte[] msg) {
        byte[] prefix = (ButtonTopBar_panel.active_mod + ":").getBytes();
        prefix = concat_array(prefix, msg); //concatena i due array prefix[] e msg[]

        write(conv_code, prefix);
    }

    public static synchronized void mod_write(byte conv_code, byte[] msg, On_arrival action) {
        byte[] prefix = (ButtonTopBar_panel.active_mod + ":").getBytes();
        prefix = concat_array(prefix, msg); //concatena i due array prefix[] e msg[]

        write(conv_code, prefix, action);
    }
    //fine dei metodi per inviare messaggi dalle mod

    public static void direct_write(String msg) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException {
        direct_write(msg.getBytes());
    }

    public static void direct_write(byte[] msg) throws IOException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        if (secure) { //è una connessione sicura cifra il messaggio
            msg = cipher.encode(msg);
        }

        output.write(new byte[]{(byte) (msg.length & 0xff), (byte) ((msg.length >> 8) & 0xff)}); //invia 2 byte che indicano la dimensione del messaggio
        output.write(msg); //invia il messaggio

        output.flush();
    }

    public static String wait_for_string() { //se non è una connessione dinamica, attende che venga inviata una stringa
        if (!dynamic) {
            return new String(wait_for_bytes());
        }
        else {
            if (Database.DEBUG) { CentralTerminal_panel.terminal_write("wait_for_string() può essere utilizzato solamente quando la connessione non è dinamica!\n", true); }
            throw new RuntimeException("wait_for_string() is a only non-dynamic connection function!");
        }
    }

    public static byte[] wait_for_bytes() { //se non è una connessione dinamica, attende che vengano inviati dei bytes
        if (!dynamic) {
            try {
                if (Database.DEBUG) { CentralTerminal_panel.terminal_write("attendo per dei bytes: ", false); }

                byte[] msg_size_byte = input.readNBytes(2); //legge la dimensione del messaggio che sta arrivando
                int msg_len = (msg_size_byte[0] & 0Xff) | (msg_size_byte[1] << 8); //trasforma i due byte appena letti in un intero

                byte[] msg = input.readNBytes(msg_len); //legge il messaggio appena arrivato

                if (secure) { //se può decifra il messaggio
                    msg = cipher.decode(msg);
                }

                if (Database.DEBUG) { CentralTerminal_panel.terminal_write("(" + new String(msg) + ")\n", false); }

                return msg; //se la connessione non è dinamica non viene aggiunto il byte conv_code quindi ritorna il messaggio così come è arrivato
            } catch (IOException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) { //connessione con il server chiusa
                throw new RuntimeException(e);
            }
        }
        else { //se la connessione è dinamica non si possono ricevere bytes in questo modo
            if (Database.DEBUG) { CentralTerminal_panel.terminal_write("wait_for_bytes() può essere utilizzato solamente quando la connessione non è dinamica!\n", true); }
            throw new RuntimeException("wait_for_bytes() is a only non-dynamic connection function!");
        }
    }

    public static boolean set_cipher(AESCipher cipher) {
        if (!secure) {
            Connection.cipher = cipher;
            Receiver.set_cipher(cipher);

            secure = true;

            return true;
        }
        else {
            return false;
        }
    }

    public static void start_dinamic() {
        Receiver.init(input);
        Receiver.start();
        dynamic = true;
    }

    public static void close() throws IOException {
        //chiude la connessione con il server
        sck.close();

        input.close();
        output.close();
        Receiver.stop();

        //resetta tutte le variabili
        secure = false;
        dynamic = false;
        paired_usr = null;
    }

    //registra un nuovo prefisso per riconoscere i messaggi indirizzati ad una specifica mod (es "login" o "pair")
    public static void register_prefix(String mod_name, String prefix, On_arrival action) {
        Receiver.register_prefix(mod_name, prefix, action);
    }

    public static void pair(String usr) {
        Connection.paired_usr = usr; //imposta il nome dell'utente con cui si è connessi

        //attiva il pannello con i bottoni per attivare le mod
        Godzilla_frame.enable_panel(Godzilla_frame.BUTTON_TOPBAR);
        ButtonTopBar_panel.setEnabled(true);

        //aggiorna i pulsanti in Client_panel
        ClientList_panel.update_buttons();

        CentralTerminal_panel.terminal_write("connesso al client: " + Connection.paired_usr + "\n", false);
    }

    public static void unpair(boolean notify_server) {
        if (is_paired()) { //se è connesso ad un client
            if (notify_server) {
                Receiver.unpair(); //invia una notifica al server per lo scollegamento
            }
            String usr = Connection.paired_usr; //ricorda il nome dell'utente a cui era collegato
            Connection.paired_usr = null;

            ButtonTopBar_panel.end_mod(); //chiude qualsiasi mod, se ce ne è una attiva

            //disattiva i pulsanti per le mod e resetta il terminal
            Godzilla_frame.disable_panel(Godzilla_frame.BUTTON_TOPBAR);
            ButtonTopBar_panel.setEnabled(false);
            CentralTerminal_panel.reset_panel();

            //aggiorna i pulsanti in Client_panel
            ClientList_panel.update_buttons();

            CentralTerminal_panel.terminal_write("disconnesso dal client: " + usr + "\n", false);
        }
        else {
            if (Database.DEBUG) { CentralTerminal_panel.terminal_write("impossibile scollegarsi da un client, non c'è nessun client appaiato\n", true); }
        }
    }

    public static String get_paired_usr() {
        return (paired_usr == null)? "" : paired_usr;
    }

    public static boolean is_paired() {
        return paired_usr != null;
    }

    public static boolean isClosed() { //ritorna true se non si è mai connesso (sck == null) o se si è connesso ma è stato disconnesso (sck.isClosed())
        return sck == null || sck.isClosed();
    }

    private static byte register_conv(On_arrival action) { //registra una nuova conversazione e ritorna il conv_code associato
        byte[] conv_code = new byte[1];
        do { //per evitare di avere conv_code duplicati o 0x00
            random.nextBytes(conv_code); //genera un byte casuale che utilizzerà come conv_code
        } while (conv_code[0] == 0x00 || !Receiver.new_conv(conv_code[0], action));

        return conv_code[0];
    }

    private static byte[] concat_array(byte[] arr1, byte[] arr2) {
        int arr1_len = arr1.length;

        arr1 = Arrays.copyOf(arr1, arr1.length + arr2.length);
        System.arraycopy(arr2, 0, arr1, arr1_len, arr2.length);

        return arr1;
    }
}

abstract class Receiver {
    private static BufferedInputStream input;

    private static Map<Byte, On_arrival> conv_map = new LinkedHashMap<>(); //memorizza tutte le conversazioni che ha aperto con il server
    private static Map<String, Map<String, On_arrival>> mod_prefix = new LinkedHashMap<>(); //mappa con tutti i prefissi registrati dalle varie mod e le azioni a loro associate, divisi per mod

    private static String pairing_usr = "";
    private static boolean secure = false;
    private static AESCipher cipher;

    public static void init(BufferedInputStream input) {
        Receiver.input = input;
    }

    public static void start() {
        if (reading == false && input != null) { //se non sta leggendo da un altro server
            new Thread(reader).start(); //inizia a leggere da questo server
        }
        else if (input == null) {
            throw new RuntimeException("non è stato inizializzato il receiver");
        }
        else if (reading == true){
            throw new RuntimeException("sto già ascoltando da un server");
        }
    }

    public static boolean new_conv(byte conv_code, On_arrival action) {
        return conv_map.putIfAbsent(conv_code, action) == null; // true se il conv_code non era già registrato, false se è un duplicato
    }

    public static boolean set_cipher(AESCipher cipher) {
        if (!secure) {
            Receiver.cipher = cipher;
            secure = true;

            return true;
        }
        else {
            return false;
        }
    }

    public static void stop() {
        input = null; //genera un errore nel thread reader e lo stoppa
        conv_map = new LinkedHashMap<>(); //resetta le conversazioni

        //resetta tutte le variabili
        secure = false;
    }

    private static boolean reading = false;
    private static Runnable reader = () -> {
        reading = true;

        try {
            while (!Connection.isClosed()) {
                byte[] msg_size_byte = input.readNBytes(2);
                int msg_len = (msg_size_byte[0] & 0Xff) | (msg_size_byte[1] << 8);

                byte[] msg = input.readNBytes(msg_len);
                if (secure) {
                    msg = cipher.decode(msg);
                }

                byte conv_code = msg[0]; //memorizza il codice della conversazione
                msg = Arrays.copyOfRange(msg, 1, msg.length); //elimina il conv_code dal messaggio

                On_arrival conv_action = conv_map.get(conv_code); //ricava l'azione da eseguire per questa conversazione
                if (conv_action == null) { //se non è registrata nessuna azione processa il messaggio normalmente
                    process_msg(conv_code, msg);
                }
                else { //se è specificata un azione la esegue
                    conv_map.remove(conv_code);
                    conv_action.on_arrival(conv_code, msg);
                }
            }
        } catch (Exception e) {
            Server.disconnect(true);
        }

        reading = false;
    };

    private static void process_msg(byte conv_code, byte[] msg) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        String msg_str = new String(msg);

        try {
            if (msg_str.equals("EOC") && !Connection.is_paired()) { //se il server si scollega
                Server.disconnect(false);
            }
            else if (msg_str.equals("EOC")) { //se il client a cui è appaiato si scollega
                Connection.unpair(false);
            }
            else if (msg_str.substring(0, 5).equals("pair:")) { //se è un messaggio per appaiarsi con un altro client
                if (!Connection.is_paired()) { //se non è appaiato con nessuno
                    pairing_usr = msg_str.substring(5);
                    CentralTerminal_panel.terminal_write("l'utente " + pairing_usr + " ha chiesto di collegarsi\n", false);
                    
                    pair_act.conv_code = conv_code;
                    TempPanel.show_msg("l'utente " + pairing_usr + " ha chiesto di collegarsi", pair_act, true);
                }
                else { //se è già appaiato con un client
                    if (Database.DEBUG) {
                        CentralTerminal_panel.terminal_write("l'utente " + pairing_usr + " ha tentato di collegarsi\n", true);
                    }
                    Connection.write(conv_code, "\000".getBytes()); //rifiuta
                }
            }
            else if (msg_str.substring(0, 7).equals("clList:")) { //riceve la lista aggiornata dei client collegati al server
                String clients_list = msg_str.substring(7);

                CentralTerminal_panel.terminal_write("lista aggiornata dei client connessi al server: " + clients_list + "\n", false);
                ClientList_panel.update_client_list(clients_list);
            }
            else if (!ButtonTopBar_panel.active_mod.equals("")) { //manage dei messaggi inviati fra le mod, se ce ne è una attiva
                check_registered_prefix(ButtonTopBar_panel.active_mod, conv_code, msg_str, msg);
            }
            else {
                CentralTerminal_panel.terminal_write("è stato ricevuto dal server (" + msg_str + ") non riconosciuto\n", true);
            }
        }
        catch (StringIndexOutOfBoundsException e) { //è stato ricevuto un messaggio non riconosciuto
            CentralTerminal_panel.terminal_write("è stato ricevuto dal server (" + msg_str + ") non è riconosciuto\n", true);
        }
    }

    private static void check_registered_prefix(String active_mod, byte conv_code, String msg_str, byte[] msg) { //msg = <mod name>:<prefix>:<msg>
        String[] msg_split = msg_str.split(":");
        if (msg_split[0].equals(active_mod)) { //se la mod per cui è riferito il messaggio è quella attiva in questo momento
            Map<String, On_arrival> prefix = mod_prefix.get(active_mod); //trova la mappa con tutti i prefissi registrati dalla mod attiva in questo momento

            //elimina il prefisso dal messaggio
            String msg_prefix = msg_split[1]; //ricava il prefisso registrato da questa mod
            byte[] msg_body_byte = Arrays.copyOfRange(msg, active_mod.length() + msg_prefix.length() + 2, msg.length); //ricava il corpo del messaggio in forma di byte[]

            On_arrival action = prefix.get(msg_prefix); //cerca l'azione da eseguire associata a questo prefisso
            if (action != null) { //se questo prefisso è stato registrato ed ha trovato l'azione associata
                action.on_arrival(conv_code, msg_body_byte);
            }
            else {
                CentralTerminal_panel.terminal_write("è stato ricevuto dal server (" + msg_str + ") non è stato registrato il prefisso dalla mod (" + ButtonTopBar_panel.active_mod + ")\n", true);
            }
        }
        else {
            CentralTerminal_panel.terminal_write("è stato ricevuto un messaggio per la mod (" + msg_split[0] + ") che non è attiva\n", true);
        }
    }

    private static PairingOperator pair_act = new PairingOperator() { //quando viene premuto "ok" o "cancella" nella temp panel
        @Override
        public synchronized void success() { //connessione accettata
            if (Database.DEBUG) { CentralTerminal_panel.terminal_write("accettata la connessione con il client\n", false); }

            Connection.write(conv_code, "\001".getBytes(), pairing_check);
        }

        @Override
        public synchronized void fail() { //connessione rifiutata
            if (Database.DEBUG) { CentralTerminal_panel.terminal_write("rifiutata la connessione con il client\n", false); }

            Connection.write(conv_code, "\000".getBytes());
        }

        //attende una conferma dell'appaiamento dal server
        private On_arrival pairing_check = (conv_code, msg) -> {
            if (msg[0] == 1) {
                Connection.pair(pairing_usr);
            }
        };
    };

    public static void unpair() {
        Connection.write("EOC");
    }
    public static void register_prefix(String mod_name, String pref, On_arrival action) { //registra un prefisso per i messaggi ricevuti dal server
        Map<String, On_arrival> prefix_map = mod_prefix.get(mod_name);

        if (prefix_map != null) { //se questa mod ha già altri prefissi registrati
            if (!prefix_map.containsKey(pref)) { //se non contiene già questo prefisso
                prefix_map.put(pref, action);
            }
            else { //se è già stato registrato questo prefisso
                CentralTerminal_panel.terminal_write("impossibile registrare il prefisso \"" + pref + "\" per la mod \"" + mod_name + "\", è già stato registrato", true);
                throw new RuntimeException("impossibile registrare il prefisso \"" + pref + "\" per la mod \"" + mod_name + "\", è già stato registrato");
            }
        }
        else { //se è il primo che registra
            prefix_map = new LinkedHashMap<>();
            mod_prefix.put(mod_name, prefix_map);

            prefix_map.put(pref, action);
        }
    }
}

class PairingOperator implements StringVectorOperator {
    public byte conv_code = 0x00;

    @Override
    public void success() {}
    @Override
    public void fail() {}
}