package gui;

import file_database.Database;
import network.Connection;
import network.On_arrival;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Pattern;

public abstract class ClientList_panel extends Database {
    private static JButton connect = null;
    private static JButton disconnect = null;
    private static GList clients_list = null;
    private static JPanel client_panel = null;

    protected static JPanel init() {
        if (client_panel == null) {
            client_panel = new JPanel();
            client_panel.setBackground(new Color(58, 61, 63));
            client_panel.setLayout(new GridBagLayout());

            //inizializza tutti i componenti della gui
            connect = new JButton();
            disconnect = new JButton();
            clients_list = new GList();
            GScrollPane clients_scroller = new GScrollPane(clients_list);
            JTextArea spacer = new JTextArea();

            disconnect.setEnabled(false);

            spacer.setBackground(new Color(58, 61, 63));
            spacer.setFocusable(false);

            connect.setIcon(new ImageIcon(ClientList_panel.class.getResource("/images/power_on.png")));
            connect.setRolloverIcon(new ImageIcon(ClientList_panel.class.getResource("/images/power_on_sel.png")));
            connect.setPressedIcon(new ImageIcon(ClientList_panel.class.getResource("/images/power_on_pres.png")));
            connect.setDisabledIcon(new ImageIcon(ClientList_panel.class.getResource("/images/power_on_dis.png")));
            disconnect.setIcon(new ImageIcon(ClientList_panel.class.getResource("/images/power_off.png")));
            disconnect.setRolloverIcon(new ImageIcon(ClientList_panel.class.getResource("/images/power_off_sel.png")));
            disconnect.setPressedIcon(new ImageIcon(ClientList_panel.class.getResource("/images/power_off_pres.png")));
            disconnect.setDisabledIcon(new ImageIcon(ClientList_panel.class.getResource("/images/power_off_dis.png")));

            connect.setBorder(null);
            disconnect.setBorder(null);
            spacer.setBorder(null);

            connect.addActionListener(connect_list);
            disconnect.addActionListener(disconnect_list);

            GridBagConstraints c = new GridBagConstraints();

            c.fill = GridBagConstraints.BOTH;
            c.weighty = 0; //i due pulsanti non si ridimensionano
            c.weightx = 0;

            c.gridx = 0;
            c.gridy = 0;
            c.insets = new Insets(0, 0, 5, 5);
            client_panel.add(disconnect, c);

            c.gridx = 2;
            c.gridy = 0;
            c.insets = new Insets(0, 5, 5, 0);
            client_panel.add(connect, c);

            c.weightx = 1; //lo spacer dovrà allungarsi sulle x per permettere ai pulsanti di rimanere delle stesse dimensioni

            c.gridx = 1;
            c.gridy = 0;
            c.insets = new Insets(0, 5, 5, 5);
            client_panel.add(spacer, c);

            c.weighty = 1; //la lista di client dovrà allungarsi sulle y per compensare i pulsanti

            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 3;
            c.insets = new Insets(5, 0, 0, 0);
            client_panel.add(clients_scroller, c);

            connect.setEnabled(false);
            disconnect.setEnabled(false);
            clients_scroller.setPreferredSize(new Dimension(0, 0));
        }

        return client_panel;
    }

    public static ActionListener connect_list = e -> {
        On_arrival pair_resp = new On_arrival() {
            @Override
            public void on_arrival(byte[] msg) {
                if (msg[0] == 1) { //appaiamento accettato
                    Connection.pair(clients_list.getSelectedValue());
                    CentralTerminal_panel.terminal_write("collegamento con " + clients_list.getSelectedValue() + " instaurato con successo!\n", false);
                    TempPanel.show_msg("collegamento con " + clients_list.getSelectedValue() + " instaurato con successo!");
                }
                else { //appaiamento rifiutato
                    CentralTerminal_panel.terminal_write("collegamento con " + clients_list.getSelectedValue() + " rifiutato\n", false);
                    TempPanel.show_msg("collegamento con " + clients_list.getSelectedValue() + " rifiutato");
                }
            }

            @Override
            public void timedout() {
                CentralTerminal_panel.terminal_write("collegamento con " + clients_list.getSelectedValue() + " rifiutato, tempo scaduto\n", false);
                TempPanel.show_msg("collegamento con " + clients_list.getSelectedValue() + " rifiutato, tempo scaduto");
            }
        };

        String pair_usr = clients_list.getSelectedValue();
        if (!pair_usr.equals("") && !Connection.is_paired()) { //se è selezionato un client e non è appaiato con nessun altro client
            Connection.write("pair:" + pair_usr, false, pair_resp);
        }
        else if (Connection.is_paired()) {
            TempPanel.show_msg("impossibile collegarsi a più client");
            if (Database.DEBUG) { CentralTerminal_panel.terminal_write("tentativo di collegarsi ad un client mentre si è già collegati a: " + Connection.get_paired_usr() + "\n", true); }
        }
        else if (pair_usr.equals("")) {
            TempPanel.show_msg("selezionale il client a cui collegarsi");
            if (Database.DEBUG) { CentralTerminal_panel.terminal_write("tentativo di collegarsi ad un client senza aver selezionato nulla dalla lista\n", true); }
        }
    };

    public static ActionListener disconnect_list = e -> {
        Connection.unpair(true);
    };

    public static void setEnabled(boolean enabled) {
        if (Connection.is_paired()) { //se è appaiato il pulsante connect è disattivato, quindi non lo modifica qualsiasi sia il valore di enabled
            disconnect.setEnabled(enabled);
        }
        else { //se non è appaiato a nessun client il pulsante disconnect è disattivato, quindi non lo modifica qualsiasi sia il valore di enabled
            connect.setEnabled(enabled);
        }
        clients_list.setEnabled(enabled);
    }

    public static void update_buttons() {
        if (Godzilla_frame.enabled()) { //se i bottoni dovrebbero essere attivi (se non lo sono verranno attivati correttamente una volta chiuso il TempPanel)
            disconnect.setEnabled(Connection.is_paired());
            connect.setEnabled(!Connection.is_paired());
        }
    }

    public static void update_client_list(String list) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        //elimina la lista precedente
        clients_list.reset_list();

        //imposta la nuova lista di client
        Pattern p = Pattern.compile(";");
        String[] names = p.split(list);

        for (String name : names) {
            if (!name.equals("")) {
                clients_list.add(name);
            }
        }
    }
}