package network;

import file_database.Database;
import gui.*;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

public class Connection {
    private static Socket sck;
    private static BufferedOutputStream output;
    private static BufferedInputStream input;

    private static String paired_usr;

    private static AESCipher cipher = null;

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

    public static synchronized void write(String msg, boolean reply) {
        write(msg.getBytes(), reply);
    }

    public static void send_mod_msg(String msg, boolean reply) {
        write(((reply)? "r" : "n") + msg, false);
    }

    public static synchronized void write(String msg, boolean reply, On_arrival action) {
        write(msg.getBytes(), reply, action);
    }

    public static synchronized void write(byte[] msg, boolean reply, On_arrival action) {
        if (Receiver.wait_for_msg(action)) { //se riesce a impostare action come azione una volta ricevuta una risposta
            write(msg, reply);
        }
        else { //se c'è già un azione per il prossimo messaggio ricevuto, aggiunge questo alla coda
            Receiver.add_to_queue(new CQueue_el(msg, reply, action));
        }
    }

    public static void write(byte[] msg, boolean reply) {
        try {
            if (!isClosed()) { //se è effettivamente connesso a qualcuno
                if (Database.DEBUG) {
                    CentralTerminal_panel.terminal_write("invio al server: (" + new String(msg) + ") " + ((secure) ? "secure\n" : "\n"), false);
                }

                byte[] msg_prefix = new byte[msg.length + 1];
                msg_prefix[0] = (reply) ? (byte) 1 : (byte) 0;
                System.arraycopy(msg, 0, msg_prefix, 1, msg.length);

                //se deve cifrare il messaggio
                if (secure) {
                    msg_prefix = cipher.encode(msg_prefix);
                }

                output.write(new byte[]{(byte) (msg_prefix.length & 0xff), (byte) ((msg_prefix.length >> 8) & 0xff)});
                output.write(msg_prefix);

                output.flush();
            }
        } catch (IllegalBlockSizeException | IOException | BadPaddingException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static String wait_for_string() {
        if (!dynamic) {
            return new String(wait_for_bytes());
        }
        else {
            if (Database.DEBUG) { CentralTerminal_panel.terminal_write("wait_for_string() può essere utilizzato solamente quando la connessione non è dinamica!\n", true); }
            throw new RuntimeException("wait_for_string() is a only non-dynamic connection function!");
        }
    }

    public static byte[] wait_for_bytes() {
        if (!dynamic) {
            try {
                if (Database.DEBUG) { CentralTerminal_panel.terminal_write("attendo per dei bytes: ", false); }

                byte[] msg_size_byte = input.readNBytes(2);
                int msg_len = (msg_size_byte[0] & 0Xff) | (msg_size_byte[1] << 8);

                byte[] msg = input.readNBytes(msg_len);

                if (secure) { //se deve decifrare il messaggio
                    msg = cipher.decode(msg);
                }

                if (Database.DEBUG) { CentralTerminal_panel.terminal_write("(" + new String(msg) + ")\n", false); }

                return Arrays.copyOfRange(msg, 1, msg.length); //rimuove il primo byte essendo un indicatore per quando il receiver è in modalità dinamica
            } catch (IOException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) { //connessione con il server chiusa
                throw new RuntimeException(e);
            }
        }
        else {
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

    public static void register_prefix(String mod_name, String prefix, Prefix_action action) {
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
}

abstract class Receiver {
    private static BufferedInputStream input;

    private static final int TIMER = 10000; //dopo 10s che attende una risposta dal server ma non la riceve chiude la connessione

    private static Vector<CQueue_el> queue = new Vector<>();

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
            throw new RuntimeException("already reading from a server");
        }
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

        //resetta tutte le variabili
        secure = false;
    }

    protected static synchronized boolean wait_for_msg(On_arrival arrival) {
        if (Receiver.arrival == null) { //se non sta attendendo già un messaggio
            Receiver.arrival = arrival;
            new Thread(timer).start();

            return true;
        }
        else {
            return false;
        }
    }

    public static synchronized void add_to_queue(CQueue_el element) { //aggiunge un nuovo messaggio alla coda da inviare
        queue.add(element);
    }

    private static void queue_next() { //invia il prossimo messaggio nella queue
        if (queue.size() != 0) { //se c'è almeno un messaggio in coda
            CQueue_el el = queue.get(0);
            queue.remove(0);

            arrival = el.get_action();
            Connection.write(el.get_msg(), el.get_reply());
        } else { //se la coda è vuota resetta arrival
            arrival = null;
        }
    }

    private static On_arrival arrival = null;
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

                if (msg[0] == 0) { //il primo byte del messaggio = 0 indica che non è una risposta ma un nuovo comando dal server
                    process_server_msg(Arrays.copyOfRange(msg, 1, msg.length));
                } else { //il primo byte diverso da zero indica che è una risposta
                    if (arrival != null) { //se qualcuno sta attendendo un messaggio dal server
                        On_arrival c_arrival = arrival; //ricorda l'azione da eseguire per questo messaggio
                        queue_next(); //fa partire il prossimo messaggio in queue

                        c_arrival.on_arrival(Arrays.copyOfRange(msg, 1, msg.length));
                    } else {
                        if (Database.DEBUG) {
                            CentralTerminal_panel.terminal_write("non è stato specificato una azione ed è stata ricevuta una risposta dal server\n", true);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Server.disconnect(true);
        }

        reading = false;
    };

    private static Runnable timer = () -> {
        try {
            String arriva_pos = arrival.toString();

            Thread.sleep(TIMER * 1000);

            if (arrival != null && arriva_pos.equals(arrival.toString())) { //se non è cambiato l'oggetto arrival, è sempre la stessa richiesta ed è scaduto il timer
                On_arrival c_arrival = arrival;

                queue_next(); //fa partire il prossimo messaggio in coda
                c_arrival.timedout(); //timedout alla azione corrente
            }
        } catch (Exception e) { }
    };

    private static void process_server_msg(byte[] msg) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        String msg_str = new String(msg);

        try {
            if (msg_str.equals("EOC") && !Connection.is_paired()) { //se il server si scollega
                Server.disconnect(false);
            } else if (msg_str.equals("EOC")) { //se il client a cui è appaiato si scollega
                Connection.unpair(false);
            } else if (msg_str.substring(0, 5).equals("pair:")) { //se è un messaggio per appaiarsi con un altro client
                if (!Connection.is_paired()) { //se non è appaiato con nessuno
                    pairing_usr = msg_str.substring(5);
                    CentralTerminal_panel.terminal_write("l'utente " + pairing_usr + " ha chiesto di collegarsi\n", false);
                    TempPanel.show_msg("l'utente " + pairing_usr + " ha chiesto di collegarsi", pair_act, true);
                } else { //se è già appaiato con un client
                    if (Database.DEBUG) {
                        CentralTerminal_panel.terminal_write("l'utente " + pairing_usr + " ha tentato di collegarsi\n", true);
                    }
                    Connection.write("\000", true); //rifiuta
                }
            } else if (msg_str.substring(0, 7).equals("clList:")) { //riceve la lista aggiornata dei client collegati al server
                String clients_list = msg_str.substring(7);

                CentralTerminal_panel.terminal_write("lista aggiornata dei client connessi al server: " + clients_list + "\n", false);
                ClientList_panel.update_client_list(clients_list);
            } else if (!ButtonTopBar_panel.active_mod.equals("")) { //manage dei messaggi inviati fra le mod, se ce ne è una attiva
                check_registered_prefix(ButtonTopBar_panel.active_mod, msg_str, msg);
            } else {
                CentralTerminal_panel.terminal_write("è stato ricevuto dal server (" + msg_str + ") non è riconosciuto\n", true);
            }
        }
        catch (StringIndexOutOfBoundsException e) { //è stato ricevuto un messaggio non riconosciuto
            CentralTerminal_panel.terminal_write("è stato ricevuto dal server (" + msg_str + ") non è riconosciuto\n", true);
        }
    }

    private static void check_registered_prefix(String mod_name, String msg_str, byte[] msg) {
        Map<String, Prefix_action> prefix = mod_prefix.get(mod_name); //trova la mappa con tutti i prefissi registrati dalla mod attiva in questo momento

        //elimina tutti gli "header" con le informazioni necessarie all'invio del messaggio, e ricava solo il contenuto
        String msg_prefix = msg_str.split(":")[0];
        String msg_body_str = msg_str.substring(msg_prefix.length() + 1);
        byte[] msg_body_byte = Arrays.copyOfRange(msg, msg_prefix.length() + 1, msg.length);

        Prefix_action action = prefix.get(msg_prefix);
        if (action != null) { //se è stato registrato questo prefisso
            action.start(msg_body_str, msg_body_byte);
        } else { //se questo prefisso non è stato registrato da questa mod
            CentralTerminal_panel.terminal_write("è stato ricevuto dal server (" + msg_str + ") non è stato registrato il prefisso dalla mod (" + ButtonTopBar_panel.active_mod + ")\n", true);
        }
    }

    private static StringVectorOperator pair_act = new StringVectorOperator() {
        @Override
        public synchronized void success() { //connessione accettata
            if (Database.DEBUG) { CentralTerminal_panel.terminal_write("accettata la connessione con il client\n", false); }

            Connection.write("\001", true, pairing_check);
        }

        @Override
        public synchronized void fail() { //connessione rifiutata
            if (Database.DEBUG) { CentralTerminal_panel.terminal_write("rifiutata la connessione con il client\n", false); }
            Connection.write("\000", true);
        }

        private On_arrival pairing_check = new On_arrival() {
            @Override
            public void on_arrival(byte[] msg) {
                if (msg[0] == 1) {
                    Connection.pair(pairing_usr);
                }
            }

            @Override
            public void timedout() throws IOException { //qualcosa è andato storto, l'appaiamento è annullato
                CentralTerminal_panel.terminal_write("non è stata ricevuta una risposta positiva dal server, appaiamento annullato\n", true);
            }
        };
    };

    public static void unpair() {
        Connection.write("EOC", false);
    }

    private static Map<String, Map<String, Prefix_action>> mod_prefix = new LinkedHashMap<>();
    public static void register_prefix(String mod_name, String pref, Prefix_action action) { //registra un prefisso per i messaggi ricevuti dal server
        Map<String, Prefix_action> prefix_map = mod_prefix.get(mod_name);

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