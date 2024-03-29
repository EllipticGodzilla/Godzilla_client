package gui;

import file_database.Database;
import file_database.File_interface;
import network.Connection;
import network.Server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

public abstract class ServerList_panel extends Database {

    private static JButton connect;
    private static JButton disconnect;
    private static JButton add_server;
    private static GList server_list; //rispetto a JList viene modificata la grafica ed inserito un popup per rinominare ed eliminare server dalla lista

    private static JPanel serverL_panel = null;
    protected static JPanel init() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        if (serverL_panel == null) {
            serverL_panel = new JPanel();
            serverL_panel.setBackground(new Color(58, 61, 63));
            serverL_panel.setLayout(new GridBagLayout());

            connect = new JButton();
            disconnect = new JButton();
            add_server = new JButton();
            server_list = new GList();
            GScrollPane server_scroller = new GScrollPane(server_list); //rispetto a JScrollPane viene modificata la grafica
            JTextArea filler = new JTextArea();

            disconnect.setEnabled(false);

            server_scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //aggiunge i popup menu ai componenti della lista
            server_list.set_popup(CellPopupMenu.class);

            //inizializza tutti i componenti della gui
            filler.setBackground(new Color(58, 61, 63));
            filler.setFocusable(false);

            connect.setBorder(null);
            disconnect.setBorder(null);
            add_server.setBorder(null);
            filler.setBorder(null);

            connect.setIcon(new ImageIcon(File_interface.jar_path + "/images/power_on.png"));
            connect.setRolloverIcon(new ImageIcon(File_interface.jar_path + "/images/power_on_sel.png"));
            connect.setPressedIcon(new ImageIcon(File_interface.jar_path + "/images/power_on_pres.png"));
            connect.setDisabledIcon(new ImageIcon(File_interface.jar_path + "/images/power_on_dis.png"));
            disconnect.setIcon(new ImageIcon(File_interface.jar_path + "/images/power_off.png"));
            disconnect.setRolloverIcon(new ImageIcon(File_interface.jar_path + "/images/power_off_sel.png"));
            disconnect.setPressedIcon(new ImageIcon(File_interface.jar_path + "/images/power_off_pres.png"));
            disconnect.setDisabledIcon(new ImageIcon(File_interface.jar_path + "/images/power_off_dis.png"));
            add_server.setIcon(new ImageIcon(File_interface.jar_path + "/images/add_server.png"));
            add_server.setRolloverIcon(new ImageIcon(File_interface.jar_path + "/images/add_server_sel.png"));
            add_server.setPressedIcon(new ImageIcon(File_interface.jar_path + "/images/add_server_pres.png"));
            add_server.setDisabledIcon(new ImageIcon(File_interface.jar_path + "/images/add_server_dis.png"));

            connect.setPreferredSize(new Dimension(30, 30));
            disconnect.setPreferredSize(new Dimension(30, 30));
            add_server.setPreferredSize(new Dimension(95, 30));

            connect.addActionListener(connect_listener);
            disconnect.addActionListener(disconnect_listener);
            add_server.addActionListener(add_server_listener);

            //aggiunge tutti i componenti al pannello organizzando la gui
            GridBagConstraints c = new GridBagConstraints();

            c.fill = GridBagConstraints.BOTH;

            c.weightx = 0; //i tre pulsanti per connettersi, disconnettersi, aggiungere un server non si allungano ne sulle x ne sulle y
            c.weighty = 0;

            c.gridx = 0;
            c.gridy = 0;
            c.insets = new Insets(0, 0, 5, 5);
            serverL_panel.add(disconnect, c);

            c.gridx = 2;
            c.gridy = 0;
            c.insets = new Insets(0, 5, 5, 5);
            serverL_panel.add(add_server, c);

            c.gridx = 3;
            c.gridy = 0;
            c.insets = new Insets(0, 5, 5, 0);
            serverL_panel.add(connect, c);

            c.weightx = 1; //spacing fra i bottoni per disconnettersi ed aggiungere un pulsante

            c.gridx = 1;
            c.gridy = 0;
            c.insets = new Insets(0, 5, 5, 5);
            serverL_panel.add(filler, c);

            c.weighty = 1;

            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 4;
            c.insets = new Insets(5, 0, 0, 0);
            serverL_panel.add(server_scroller, c);

            serverL_panel.setPreferredSize(new Dimension(0, 0));

        }
        return serverL_panel;
    }

    public static void setEnabled(boolean enabled) {
        if (Connection.isClosed()) { //se non è connesso a nessun server il pulsante disconnect è disattivato, quindi non lo modifica qualsiasi sia il valore di enabled
            connect.setEnabled(enabled);
        }
        else { //se è connesso ad un server il pulsante connect è disattivato, quindi non lo modifica qualsiasi sia il valore di enabled
            disconnect.setEnabled(enabled);
        }
        serverL_panel.setEnabled(enabled);
        add_server.setEnabled(enabled);
        server_list.setEnabled(enabled);
    }

    public static void update_gui() throws InvocationTargetException, InstantiationException, IllegalAccessException {
        for (String name : Database.serverList.keySet()) { //aggiunge alla lista tutti i server caricati sul database
            server_list.add(name);
        }
    }

    public static void update_button() {
        if (Godzilla_frame.enabled()) { //se i pulsanti dovrebbero essere attivi
            connect.setEnabled(Connection.isClosed());
            disconnect.setEnabled(!Connection.isClosed());
        }
    }

    private static ActionListener add_server_listener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            TempPanel.show(new TempPanel_info("inserisci il link al server: ", "inserisci il nome del server: "), name_and_ip);
        }

        private StringVectorOperator name_and_ip = new StringVectorOperator() {
            @Override
            public void success()  {
                try {
                    if (ServerList_panel.is_valid(input.elementAt(0), false) && ServerList_panel.is_valid(input.elementAt(1), true)) {
                        Database.serverList.put(input.elementAt(1), input.elementAt(0)); //aggiunge indirizzo e nome alla mappa serverList
                        server_list.add(input.elementAt(1)); //aggiunge il nome del server alla JList rendendolo visibile
                    } else {
                        TempPanel.show(new TempPanel_info("il nome o indirizzo inseriti non sono validi, inserire nome ed indirizzo validi", false), error_name_ip);
                    }
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void fail() {} //non si vuole più aggiungere un server
        };

        private StringVectorOperator error_name_ip = new StringVectorOperator() {
            @Override
            public void success() {

                TempPanel.show(new TempPanel_info("inserisci il link al server: ", "inserisci il nome del server: "), name_and_ip);
            }

            @Override
            public void fail() {} //essendo un messaggio non può essere premuto il tasto "annulla"
        };

    };

    private static boolean is_valid(String name, boolean is_a_name) { //controlla che il nome o indirizzo inserito sia valido
        return true;
    }

    private static ActionListener connect_listener = e -> {
        String link = Database.serverList.get(server_list.getSelectedValue());

        CentralTerminal_panel.terminal_write("connessione con il server: " + link + "\n", false);
        Server.start_connection_with(link);
    };

    private static ActionListener disconnect_listener = e -> {
        Server.disconnect(true);
    };

    public static class CellPopupMenu extends JPopupMenu {
        private String cell_name;
        private final GList PARENT_LIST;

        public CellPopupMenu(String name, GList list) {
            super();
            this.cell_name = name;
            this.PARENT_LIST = list;

            UIManager.put("MenuItem.selectionBackground", new Color(108, 111, 113));
            UIManager.put("MenuItem.selectionForeground", new Color(158, 161, 163));

            JMenuItem rename = new JMenuItem("rename");
            JMenuItem remove = new JMenuItem("remove");

            this.setBorder(BorderFactory.createLineBorder(new Color(28, 31, 33)));
            rename.setBorder(BorderFactory.createLineBorder(new Color(78, 81, 83)));
            remove.setBorder(BorderFactory.createLineBorder(new Color(78, 81, 83)));

            rename.setBackground(new Color(88, 91, 93));
            remove.setBackground(new Color(88, 91, 93));
            rename.setForeground(new Color(158, 161, 163));
            remove.setForeground(new Color(158, 161, 163));

            rename.addActionListener(rename_listener);
            remove.addActionListener(remove_listener);

            this.add(rename);
            this.add(remove);
        }

        private StringVectorOperator rename_action = new StringVectorOperator() {
            @Override
            public void success() {
                if (is_valid(input.elementAt(0), true) && !input.elementAt(0).equals(cell_name)) {
                    CentralTerminal_panel.terminal_write("rinomino il server \"" + cell_name + "\" in \"" + input.elementAt(0) + "\"\n", false);
                    PARENT_LIST.rename_element(cell_name, input.elementAt(0)); //modifica il nome nella lista visibile

                    Database.serverList.put( //modifica il nome nel database
                            input.elementAt(0),
                            Database.serverList.get(cell_name)
                    );
                    Database.serverList.remove(cell_name);

                    cell_name = input.elementAt(0); //modifica il nome per questo popup
                } else {
                    TempPanel.show(new TempPanel_info("inserisci un nome valido ed unico fra tutti i server", false), rename_fail);
                }
            }

            @Override
            public void fail() {} //non si vuole più rinominare il server
        };

        private StringVectorOperator rename_fail = new StringVectorOperator() {
            @Override
            public void success() {
                TempPanel.show(new TempPanel_info("inserisci il nuovo nome per il server: " + cell_name), rename_action);
            }

            @Override
            public void fail() {} //essendo un messaggio non può "fallire"
        };

        private ActionListener rename_listener = (e) -> {
            TempPanel.show(new TempPanel_info("inserisci il nuovo nome per il server: " + cell_name), rename_action);
        };

        private StringVectorOperator remove_confirm = new StringVectorOperator() {
            @Override
            public void success() {
                CentralTerminal_panel.terminal_write("rimuovo il server \"" + cell_name + "\"\n", false);

                Database.serverList.remove(cell_name); //rimuove il server dal database
                PARENT_LIST.remove(cell_name); //rimuove il server dalla lista visibile
            }

            @Override
            public void fail() {} //non vuole più rimuovere il server
        };

        private ActionListener remove_listener = (e) -> {
            TempPanel.show(new TempPanel_info("il server \"" + cell_name + "\" verrà rimosso", true), remove_confirm);
        };

    }
}