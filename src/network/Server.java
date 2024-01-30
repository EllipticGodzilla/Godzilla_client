package network;

import file_database.Database;
import gui.*;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Pattern;

public abstract class Server {
    public static final int E_INVCE = -4; //error codes
    public static final int E_GEN = -3;
    public static final int E_INVIP = -2;
    public static final int E_CONR = -1;

    public static String mail = "";
    public static String registered_name = "";
    private static String username = "";
    private static byte[] pub_key;

    private static final String CA_DNS_IP = "127.0.0.1";
    private static final int CA_DNS_PORT = 9696;

    private static final int SERVER_PORT = 31415;

    private static MessageDigest sha3_hash;

    static { //inizializza sha3_hash
        try {
            sha3_hash = MessageDigest.getInstance("SHA3-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static int start_connection_with(String link) {
        if (Connection.isClosed()) {
            boolean dns_alive = false; //distingue fra connessione rifiutata dal DNS e dal server

            try {
                String ip;
                if (is_an_ip(link)) { //se viene dato l'indirizzo ip del server a cui collegarsi
                    ip = link;

                    if (Database.DEBUG) { CentralTerminal_panel.terminal_write("conosciamo già l'indirizzo ip: " + ip + "\nmi collego al server DNS: ", false); }

                    link = get_from_dns('r', ip);

                    if (Database.DEBUG) { CentralTerminal_panel.terminal_write("ricevuto l'indirizzo (" + link + ")\n", false); }
                } else { //se viene dato il link, si collega al DNS per ricevere l'indirizzo ip
                    if (Database.DEBUG) { CentralTerminal_panel.terminal_write("conosciamo il link del server, link: " + link + "\nmi collego al server DNS: ", false); }

                    ip = get_from_dns('d', link);

                    if (Database.DEBUG) { CentralTerminal_panel.terminal_write("ricevuto l'indirizzo ip (" + ip + ")\n", false); }
                }

                dns_alive = true;
                Connection.init(ip, SERVER_PORT);

                //riceve il certificato del server e controlla sia tutto in regola
                boolean check_ce = check_certificate(link, ip);
                if (check_ce == true) { //se il certificato è valido, genera una chiave sint size = (bis.read() & 0xff) | ((bis.read() & 0xff) int size = (bis.read() & 0xff) | ((bis.read() & 0xff) << 8);<< 8);immetrica e la invia al server
                    Cipher pubKey_cipher = get_pub_cipher();
                    secure_with_aes(pubKey_cipher);
                } else { //se è stato trovato un errore nel verificare il certificato
                    CentralTerminal_panel.terminal_write("certificato non valido!", true);
                    TempPanel.show_msg("il certificato ricevuto dal server non è valido, chiudo la connessione");

                    Connection.close();

                    return E_INVCE;
                }

                return 0;
            } catch (UnknownHostException e) {
                CentralTerminal_panel.terminal_write("è stato inserito un indirizzo ip non valido\n", true);
                TempPanel.show_msg("l'indirizzo ip è stato inserito male, non ha senso");
                return E_INVIP;
            } catch (ConnectException e) {
                if (dns_alive) { //se la connessione è stata rifiutata dal server
                    CentralTerminal_panel.terminal_write("il server a cui si è cercato di connettersi non è raggiungibile\n", true);
                    TempPanel.show_msg("connessione rifiutata, il server non è raggiungibile");
                } else { //se la connessione è stata rifiutata dal DNS
                    CentralTerminal_panel.terminal_write("il server DNS non è raggiungibile\n", true);
                    TempPanel.show_msg("errore nella configurazione del DNS, server non raggiungibile");
                }
                return E_CONR;
            } catch (IOException e) {
                CentralTerminal_panel.terminal_write("connessione interrotta inaspettatamente\n", true);
                TempPanel.show_msg("impossibile connettersi al server");
                return E_GEN;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        else {
            CentralTerminal_panel.terminal_write("già connesso ad un server!\n", true);
            TempPanel.show_msg("già connessi ad un server!");

            return E_GEN;
        }
    }

    public static String get_from_dns(char prefix, String msg) throws IOException, IllegalBlockSizeException, BadPaddingException {
        Connection.init(CA_DNS_IP, CA_DNS_PORT);
        Connection.write(prefix + msg, false); //aggiungendo d all'inizio specifichi al dns che vuoi conoscere l'indirizzo ip partendo dal link

        String response = Connection.wait_for_string();
        Connection.close();

        return response;
    }

    public static boolean check_certificate(String link, String ip) throws IllegalBlockSizeException, NoSuchAlgorithmException {
        try {
            //riceve le informazioni per il controllo del certificato dal server
            String server_info = Connection.wait_for_string();
            byte[] server_certificate = Connection.wait_for_bytes();

            if (Database.DEBUG) {
                CentralTerminal_panel.terminal_write("ricevuto il certificato del server - ", false);
            }

            //controlla che le informazioni coincidano con il certificato
            byte[] dec_ce = Database.CAPublicKey.doFinal(server_certificate); //decifra il certificato trovando l'hash delle informazioni del server
            byte[] server_info_hash = sha3_hash(server_info.getBytes());

            Pattern patt = Pattern.compile("[;]");
            String[] info_array = patt.split(server_info);

            /*
             * controlla che:
             * 1) l'hash calcolato da server_info e quello decifrato dal certificato coincidano => il certificato è stato effettivamente rilasciato dalla CA
             * 2) l'indirizzo scritto nelle server_info (e quindi nel certificato) coincide con quello voluto
             * 3) l'indirizzo ip scritto nelle server_info (e quindi nel certificato) coincida con quello voluto
             */
            if (Arrays.compare(dec_ce, server_info_hash) == 0 && info_array[1].equals(link) && info_array[2].equals(ip)) {
                if (Database.DEBUG) {
                    CentralTerminal_panel.terminal_write("valido!\n", false);
                }

                registered_name = info_array[0]; //salva il nome con cui si è registrato questo server
                mail = info_array[4]; //salva la mail con cui è possibile contattare admin del server
                pub_key = Base64.getDecoder().decode(info_array[3].getBytes());

                return true;
            } else {
                if (Database.DEBUG) {
                    CentralTerminal_panel.terminal_write("non valido\n", true);
                }
                return false;
            }
        } catch (BadPaddingException e) {
            return false;
        }
    }

    public static void secure_with_aes(Cipher pubKey_cipher) throws NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, IOException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        KeyGenerator kg = KeyGenerator.getInstance("AES"); //genera una chiave AES di sessione
        SecretKey key = kg.generateKey();

        byte check[] = new byte[8]; //genera 8 byte random che il server dovrà inviare cifrati con key per confermare di aver ricevuto correttamente la chiave di sessione
        new SecureRandom().nextBytes(check);

        byte[] aes_key_encoded = pubKey_cipher.doFinal(merge_array(key.getEncoded(), check)); //cifra la chiave AES assieme al check code con la chiave pubblica del server
        Connection.write(aes_key_encoded, true);

        AESCipher cipher = new AESCipher(key, check);
        Connection.set_cipher(cipher); //da ora la connessione verrà cifrata con la chiave AES appena generata

        byte[] received_check = Connection.wait_for_bytes(); //attende che il server invii i check bytes cifrati con la chiave di sessione
        if (Arrays.compare(received_check, check) != 0) { //se i due array non coincidono, i byte di check sono stati cifrati in modo errato da parte del server
            CentralTerminal_panel.terminal_write("il server non ha cifrato correttamente il check code, chiudo la connessione\n", true);
            Connection.close(); //chiude la connessione
        }
        else { //se il server cifra correttamente i byte di check prosegue con il protocollo:
            CentralTerminal_panel.terminal_write("il server ha cifrato correttamente il check code\n", false);

            CentralTerminal_panel.terminal_write("connessione sicura instaurata con il server\n", false);
            ServerList_panel.update_button(); //disattiva il tasto per collegarsi ad un server ed attiva quello per scollegarsi

            Connection.start_dinamic(); //inizia a ricevere messaggi dal server in modo dinamico
            login_register(); //si registra o fa il login
        }

    }

    public static void disconnect(boolean notify_server) {
        try {
            if (!Connection.isClosed()) {
                CentralTerminal_panel.terminal_write("disconnessione dal server\n", false);
                Godzilla_frame.set_title("Godzilla - Client");

                //se è appaiato con un altro client si scollega
                if (Connection.is_paired()) {
                    Connection.unpair(notify_server);
                }

                //si scollega dal server
                if (notify_server) { //se deve avvisare il server che si sta scollegando
                    Connection.write("EOC", false); //avvisa che sta chiudendo la connessione
                }
                Connection.close();

                //disattiva il pannello client list e resetta la lista dei client
                ClientList_panel.update_client_list("");
                ClientList_panel.setEnabled(false);
                Godzilla_frame.disable_panel(Godzilla_frame.CLIENT_LIST);

                //aggiorna quali pulsati sono attivi nella server_list gui
                ServerList_panel.update_button();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] merge_array(byte[] arr1, byte[] arr2) { //unisce i due array
        byte[] merged_array = Arrays.copyOf(arr1, arr1.length + arr2.length);
        System.arraycopy(arr2, 0, merged_array, arr1.length, arr2.length);

        return merged_array;
    }

    private static void login_register() {
        if (Database.DEBUG) { CentralTerminal_panel.terminal_write("chiedo se vuole fare il login: action = " + login_or_register + "\n", false); }
        TempPanel.show_msg("se vuoi fare il login premi \"ok\", altrimenti cancella", login_or_register, true);
    }

    private static StringVectorOperator login_or_register = new StringVectorOperator() {
        @Override
        public void success() { //se vuole fare il login
            Vector<String> requests = new Vector<>();
            requests.add("inserisci nome utente: ");
            requests.add("inserisci password: ");

            if (Database.DEBUG) { CentralTerminal_panel.terminal_write("chiedo nome utente e password per il login: action = " + login + "\n", false); }
            TempPanel.request_string(requests, login); //richiede nome utente e password
        }

        @Override
        public void fail() { //se vuole registrarsi o se vuole uscire
            if (Database.DEBUG) { CentralTerminal_panel.terminal_write("chiedo se vuole registrarsi: action = " + login_or_register + "\n", false); }
            TempPanel.show_msg("se vuoi registrarti premi \"ok\", altrimenti \"cancella\" e verrai disconnesso", register_exit, true);
        }
    };

    private static StringVectorOperator login = new StringVectorOperator() {
        @Override
        public void success() { //sono stati inseriti nome utente e password
            try {
                if (Database.DEBUG) { CentralTerminal_panel.terminal_write("provo a fare il login - ", false); }
                byte[] psw_hash = psw_array(input.elementAt(0).getBytes(), input.elementAt(1).getBytes()); //calcola l'hash da utilizzare come password per effettuare il login

                //forma il messaggio da inviare al server "login:<usr>;sha3-256(<psw>+0xff.ff^<usr>)" e lo invia
                byte[] server_msg = Arrays.copyOf(("login:" + input.elementAt(0) + ";").getBytes(), 7 + input.elementAt(0).length() + psw_hash.length);
                System.arraycopy(psw_hash, 0, server_msg, 7 + input.elementAt(0).length(), psw_hash.length);

                Connection.write(server_msg, false, login_result); //attende una risposta dal server
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        private On_arrival login_result = (msg) -> { //una volta ricevuta la risposta dal server
            if (Arrays.compare(msg, "log".getBytes()) == 0) { //se il login è andato a buon fine
                if (Database.DEBUG) { CentralTerminal_panel.terminal_write("password e nome utente giusti\n", false); }
                CentralTerminal_panel.terminal_write("login effettuato con successo! username = " + input.elementAt(0) + "\n", false);

                username = input.elementAt(0);
                Godzilla_frame.set_title(registered_name + " - " + username);

                //attiva il pannello con la lista dei client
                CentralTerminal_panel.terminal_write("attivato il pannello con la lista dei client\n", false);
                Godzilla_frame.enable_panel(Godzilla_frame.CLIENT_LIST);
                ClientList_panel.setEnabled(true);
            }
            else { //se nome utente o password sono sbagliati
                if (Database.DEBUG) { CentralTerminal_panel.terminal_write("password o nome utente sbagliati\n", true); }

                TempPanel.show_msg("nome utente o password errati, premere \"ok\" per ritentare", login_or_register, true);
            }
        };

        @Override
        public void fail() { //è stato premuto "cancella" viene chiesto nuovamente se vuole fare il login o se vuole registrarsi/scollegarsi
            if (Database.DEBUG) { CentralTerminal_panel.terminal_write("chiedo se vuole fare il login: action = " + login_or_register+ "\n", false); }
            TempPanel.show_msg("se vuoi fare il login premi \"ok\", altrimenti cancella", login_or_register, true);
        }
    };

    private static StringVectorOperator register_exit = new StringVectorOperator() {
        @Override
        public void success() { //vuole registrarsi
            Vector<String> requests = new Vector<>();
            requests.add("inserisci nome utente: ");
            requests.add("inserisci password: ");

            TempPanel.request_string(requests, register); //richiede nome utente e password
        }

        @Override
        public void fail() { //si disconnette dal server
            disconnect(true);
        }
    };

    private static StringVectorOperator register = new StringVectorOperator() {
        @Override
        public void success() {
            try {
                byte[] psw_hash = psw_array(input.elementAt(0).getBytes(), input.elementAt(1).getBytes()); //calcola l'hash da utilizzare come password per registrarsi

                //forma il messaggio da inviare al server "register:<usr>;sha3-256(<psw>+0xff..ff^<usr>)" e lo invia
                byte[] server_msg = Arrays.copyOf(("register:" + input.elementAt(0) + ";").getBytes(), 10 + input.elementAt(0).length() + psw_hash.length);
                System.arraycopy(psw_hash, 0, server_msg, 10 + input.elementAt(0).length(), psw_hash.length);

                Connection.write(server_msg, false, register_result);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        private On_arrival register_result = (msg) -> {
            if (Arrays.compare(msg, "reg".getBytes()) == 0) { //se la registrazione è andata a buon fine
                CentralTerminal_panel.terminal_write("registrazione effettuata con successo! username = " + input.elementAt(0) + "\n", false);

                username = input.elementAt(0);
                Godzilla_frame.set_title(registered_name + " - " + username);

                //attiva il pannello con la lista dei client
                CentralTerminal_panel.terminal_write("attivato il pannello con la lista dei client\n", false);
                Godzilla_frame.enable_panel(Godzilla_frame.CLIENT_LIST);
                ClientList_panel.setEnabled(true);
            }
            else { //se nome utente o password sono sbagliati
                TempPanel.show_msg("nome utente o password errati, premere \"ok\" per ritentare", login_or_register, true);
            }
        };

        @Override
        public void fail() { //se viene premuto cancella, chiede nuovamente se vuole fare il login o registrarsi/scollegarsi
            TempPanel.show_msg("se vuoi fare il login premi \"ok\", altrimenti cancella", login_or_register, true);
        }
    };

    private static byte[] psw_array(byte[] usr, byte[] psw) throws NoSuchAlgorithmException { //ritorna sha3-256(<psw>+0xff..ff^<usr>)
        byte[] usr_inverse = new byte[psw.length];

        for (int i = 0; i < usr.length; i++) {
            usr_inverse[i] = (byte) (usr[i] ^ 0xff);
        }

        int psw_len = psw.length;
        psw = Arrays.copyOf(psw, psw_len + usr_inverse.length); //aumenta la lunghezza di psw[]
        System.arraycopy(usr_inverse, 0, psw, psw_len, usr_inverse.length); //copia usr_inverse[] in psw[]

        return sha3_hash(psw); //ritorna l'hash di psw[]
    }

    private static boolean is_an_ip(String txt) {
        String[] segm = txt.split("\\.");

        if (segm.length != 4) { return false; } //un indirizzo ip ha 4 segmenti separati da "."

        try {
            for (int i = 0; i < segm.length; i++) {
                if (Integer.valueOf(segm[i]) > 255) { //in un ip ogni segmento può arrivare massimo a 255
                    return false;
                }
            }
        }
        catch (NumberFormatException e) { //un segmento non rappresenta un numero
            return false;
        }

        return true; //sono 4 segmenti dove ogni segmento è un numero minore o uguale a 255
    }

    //genera un Cipher RSA con la chiave pubblica del server a cui si sta collegando
    private static Cipher get_pub_cipher() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException {
        KeyFactory key_f = KeyFactory.getInstance("RSA");
        PublicKey server_key = key_f.generatePublic(new X509EncodedKeySpec(pub_key));

        Cipher server_cipher = Cipher.getInstance("RSA");
        server_cipher.init(Cipher.ENCRYPT_MODE, server_key);

        return server_cipher;
    }

    private static byte[] sha3_hash(byte[] txt) throws NoSuchAlgorithmException { //calcola l'hash di txt secondo l'algoritmo sha3
        return sha3_hash.digest(txt);
    }
}
