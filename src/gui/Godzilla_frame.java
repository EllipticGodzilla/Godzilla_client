package gui;

import file_database.Database;
import network.Server;
import network.Connection;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

public abstract class Godzilla_frame {
    //tutti i pannelli da aggiungere al frame
    private static JPanel server_list = null;
    private static JPanel client_list = null;
    private static JPanel button_topbar = null;
    private static JLayeredPane central_terminal = null;
    private static JPanel temp_panel = null;

    //ricorda quali pannelli erano attivi prima di aprire una temp window in modo da riattivarli una volta chiusa
    public static final int SERVER_LIST = 0;
    public static final int CLIENT_LIST = 1;
    public static final int BUTTON_TOPBAR = 2;

    private static boolean active = true;
    private static boolean[] active_panel = new boolean[] {true, false, false};

    //permette a tutti di comunicare con il server
    protected static Connection connected_server = null;

    private static JFrame godzilla_frame = null;

    public static JFrame init() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException { //inizializza la schermata e ritorna il JFrame
        if (godzilla_frame == null) {
            godzilla_frame = new JFrame("Godzilla - Client");
            godzilla_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            godzilla_frame.setMinimumSize(new Dimension(900, 500));

            //inizializza tutti i pannelli che formeranno la gui principale
            server_list = ServerList_panel.init();
            client_list = ClientList_panel.init();
            button_topbar = ButtonTopBar_panel.init();
            central_terminal = CentralTerminal_panel.init();
            temp_panel = TempPanel.init();

            //inizializza la gui principale (tutti i pannelli tranne Temp Panels)
            JPanel content_panel = new JPanel();
            content_panel.setBackground(new Color(58, 61, 63));
            content_panel.setLayout(new GridBagLayout());

            GridBagConstraints c = new GridBagConstraints();

            c.fill = GridBagConstraints.BOTH;
            c.weightx = 0.22; //i due pannelli sulla sinistra, devono essere più corti rispetto a quelli sulla destra

            c.gridx = 0;
            c.gridy = 2;
            c.gridheight = 1;
            c.weighty = 0.4; //deve compensare per selection panel
            c.insets = new Insets(5, 10, 10, 5);
            content_panel.add(server_list, c);

            c.gridx = 0;
            c.gridy = 0;
            c.gridheight = 2;
            c.weighty = 0.6; //selection panel deve essere un po' più alto rispetto a connection panel
            c.insets = new Insets(10, 10, 5, 5);
            content_panel.add(client_list, c);

            c.weightx = 0.78; //i due pannelli sulla destra, devono essere più lunghi e conpensare i due pannelli sulla sinistra

            c.gridx = 1;
            c.gridy = 0;
            c.gridheight = 1;
            c.weighty = 0; //la top bar non viene ridimensionata per le y
            c.insets = new Insets(10, 5, 5, 10);
            content_panel.add(button_topbar, c);

            c.gridx = 1;
            c.gridy = 1;
            c.weighty = 1; //per compensare la top bar
            c.insets = new Insets(5, 5, 10, 10);
            c.gridheight = 2;
            content_panel.add(central_terminal, c);

            FullscreenLayeredPane lp = new FullscreenLayeredPane();
            content_panel.setBounds(0, 0, 900, 663);
            lp.add_fullscreen(content_panel, JLayeredPane.DEFAULT_LAYER);
            lp.add(temp_panel, JLayeredPane.POPUP_LAYER);

            godzilla_frame.setLayeredPane(lp);
            godzilla_frame.setVisible(true);

            //mantiene TempPanel sempre al centro del frame
            godzilla_frame.addComponentListener(new ComponentListener() {
                @Override
                public void componentMoved(ComponentEvent e) {}
                @Override
                public void componentShown(ComponentEvent e) {}
                @Override
                public void componentHidden(ComponentEvent e) {}

                @Override
                public void componentResized(ComponentEvent e) {
                    recenter_temp_panel();
                }
            });
        }
        return godzilla_frame;
    }

    public static void set_title(String title) {
        Godzilla_frame.godzilla_frame.setTitle(title);
    }

    protected static boolean is_enabled(int panel) {
        return active_panel[panel];
    }

    protected static boolean enabled() {
        return active;
    }

    protected static void disable_panels() { //disabilita tutti i pannelli quando si apre TempPanel
        active = false;

        ServerList_panel.setEnabled(false);
        ClientList_panel.setEnabled(false);
        ButtonTopBar_panel.setEnabled(false);
    }

    public static void disable_panel(int panel) {
        active_panel[panel] = false;
    }

    public static void enable_panel(int panel) {
        active_panel[panel] = true;
    }

    protected static void enable_panels() { //riattiva i pannelli una volta chiusa TempPanel
        active = true;

        ServerList_panel.setEnabled(active_panel[SERVER_LIST]);
        ClientList_panel.setEnabled(active_panel[CLIENT_LIST]);
        ButtonTopBar_panel.setEnabled(active_panel[BUTTON_TOPBAR]);
    }

    protected static void recenter_temp_panel() {
        temp_panel.setLocation(
                godzilla_frame.getWidth() / 2 - temp_panel.getWidth() / 2,
                godzilla_frame.getHeight() / 2 - temp_panel.getHeight() / 2
        );
    }
}

class GScrollPane extends JScrollPane {
    public GScrollPane(Component c) {
        super(c);

        this.setBorder(BorderFactory.createLineBorder(new Color(72, 74, 75)));
    }

    @Override
    public JScrollBar createVerticalScrollBar() {
        JScrollBar scrollBar = super.createVerticalScrollBar();

        scrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(78, 81, 83);
                this.thumbDarkShadowColor = new Color(58, 61, 63);
                this.thumbHighlightColor = new Color(108, 111, 113);
            }

            class null_button extends JButton {
                public null_button() {
                    super();
                    this.setPreferredSize(new Dimension(0, 0));
                }
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return new null_button();
            }
            @Override
            protected JButton createIncreaseButton(int orientation) {
                return new null_button();
            }
        });

        scrollBar.setBackground(new Color(128, 131, 133));
        scrollBar.setBorder(BorderFactory.createLineBorder(new Color(72, 74, 75)));

        return scrollBar;
    }

    @Override
    public JScrollBar createHorizontalScrollBar() {
        JScrollBar scrollBar = super.createHorizontalScrollBar();

        scrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(78, 81, 83);
                this.thumbDarkShadowColor = new Color(58, 61, 63);
                this.thumbHighlightColor = new Color(108, 111, 113);
            }

            class null_button extends JButton {
                public null_button() {
                    super();
                    this.setPreferredSize(new Dimension(0, 0));
                }
            }

            @Override
            protected JButton createDecreaseButton(int orientation) { return new null_button(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return new null_button(); }

        });

        scrollBar.setBackground(new Color(128, 131, 133));
        scrollBar.setBorder(BorderFactory.createLineBorder(new Color(72, 74, 75)));

        return scrollBar;
    }
}

/*
* utilizzando un estensione di JList viene più semplice ma aggiungere e rimuovere elementi dalla lista in modo dinamico può provocare problemi grafici
* dove la lista viene mostrata vuota finché non le si dà un nuovo update, di conseguenza ho creato la mia versione di JList utilizzando varie JTextArea
* e partendo da un JPanel.
* Non so bene da che cosa sia dovuto il problema con JList ma sembra essere risolto utilizzando la mia versione
 */
class GList extends JPanel {
    private Map<String, ListCell> elements = new LinkedHashMap<>();
    private int selected_index = -1;

    private JPanel list_panel = new JPanel(); //pannello che contiene tutte le JTextArea della lista
    private JTextArea filler = new JTextArea(); //filler per rimepire lo spazio in basso

    private Constructor popupMenu = null;

    public GList() {
        super();
        this.setLayout(new GridBagLayout());

        this.setForeground(new Color(44, 46, 47));
        this.setBackground(new Color(98, 101, 103));
        this.setFont(new Font("custom_list", Font.BOLD, 11));

        filler.setBackground(this.getBackground());
        filler.setFocusable(false);
        filler.setEditable(false);

        list_panel.setLayout(new GridBagLayout());
        list_panel.setBackground(this.getBackground());

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0;
        c.weightx = 1;
        this.add(list_panel, c);

        c.gridy = 1;
        c.weighty = 1;
        this.add(filler, c);
    }

    public void set_popup(Class PopupMenu) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        this.popupMenu = PopupMenu.getDeclaredConstructor(String.class, GList.class);

        for (ListCell cell : elements.values()) {
            cell.setComponentPopupMenu((JPopupMenu) this.popupMenu.newInstance(cell.getText(), this));
        }
    }

    public void add(String name) throws InvocationTargetException, InstantiationException, IllegalAccessException { //aggiunge una nuova casella nell'ultima posizione
        ListCell cell = new ListCell(name, this, elements.size());
        elements.put(name, cell);
        if (this.popupMenu != null) {
            cell.setComponentPopupMenu((JPopupMenu) this.popupMenu.newInstance(name, this));
        }

        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        c.gridy = elements.size() - 1;
        c.gridx = 0;

        list_panel.add(cell, c);

        this.updateUI(); //aggiorna la gui
    }

    public void remove(String name) {
        ListCell cell = elements.get(name);

        elements.remove(name); //rimuove la cella dalla lista
        list_panel.remove(cell); //rimuove la casella dalla lista

        if (cell.my_index == selected_index) { //se questa casella era selezionata
            selected_index = -1;
        }

        this.updateUI(); //aggiorna la gui
    }

    public void rename_element(String old_name, String new_name) {
        for (ListCell cell : elements.values()) {
            if (cell.getText().equals(old_name)) {
                cell.setText(new_name);
                break;
            }
        }
    }

    public String getSelectedValue() {
        if (selected_index == -1) { //se non è selezionata nessuna casella
            return "";
        }
        else {
            return ((ListCell) list_panel.getComponent(selected_index)).getText();
        }
    }

    public void reset_list() {
        elements = new LinkedHashMap<>();
        list_panel.removeAll();
        this.repaint();

        selected_index = -1;
    }

    class ListCell extends JTextArea {
        private static final Color STD_BACKGROUND = new Color(98, 101, 103);
        private static final Color SEL_BACKGROUND = new Color(116, 121, 125);
        private static final Color SEL_BORDER = new Color(72, 74, 75);

        private final GList PARENT_LIST;
        private int my_index;

        public ListCell(String text, GList list, int index) {
            super(text);
            this.PARENT_LIST = list;
            this.my_index = index;

            //imposta tutti i colori
            this.setForeground(new Color(44, 46, 47));
            this.setBackground(STD_BACKGROUND);
            this.setFont(new Font("custom_list", Font.BOLD, 11));
            this.setBorder(null);

            this.setEditable(false);
            this.setCaretColor(STD_BACKGROUND);
            this.setCursor(null);

            this.addKeyListener(key_l);
            this.addMouseListener(mouse_l);
        }

        private KeyListener key_l = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyPressed(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case 40: //freccia in basso
                        try {
                            ListCell next_cell = (ListCell) PARENT_LIST.list_panel.getComponent(my_index + 1);

                            next_cell.set_selected();
                            next_cell.requestFocus();
                        } catch (Exception ex) {} //se non esiste un elemento ad index my_index + 1
                        break;

                    case 38: //freccia in alto
                        try {
                            ListCell prev_cell = (ListCell) PARENT_LIST.list_panel.getComponent(my_index - 1);

                            prev_cell.set_selected();
                            prev_cell.requestFocus();
                        } catch (Exception ex) {} //se non esiste un elemento ad index my_index - 1
                        break;

                    case 27: //esc
                        unselect();
                        break;

                    case 10: //invio, si collega a questo server
                        Server.start_connection_with(Database.serverList.get(getText()));
                        break;
                }
            }
        };

        private MouseListener mouse_l = new MouseListener() {
            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}
            @Override
            public void mouseClicked(MouseEvent e) {}

            @Override
            public void mousePressed(MouseEvent e) {
                set_selected();
            }
        };

        public void set_selected() {
            if (PARENT_LIST.selected_index != my_index) {
                //deseleziona la casella selezionata in precedenza, se ne era selezionata una
                if (PARENT_LIST.selected_index != -1) {
                    ((ListCell) PARENT_LIST.list_panel.getComponent(PARENT_LIST.selected_index)).unselect();
                }

                //imposta questa JTextArea come selezionata
                setBackground(SEL_BACKGROUND);
                setBorder(BorderFactory.createLineBorder(SEL_BORDER));
                setCaretColor(SEL_BACKGROUND);
                setSelectionColor(SEL_BACKGROUND);

                PARENT_LIST.selected_index = my_index;
            }
        }

        public void unselect() {
            setBackground(STD_BACKGROUND);
            setBorder(null);
            setCaretColor(STD_BACKGROUND);
            setSelectionColor(STD_BACKGROUND);

            PARENT_LIST.selected_index = -1;
        }
    }
}

class FullscreenLayeredPane extends JLayeredPane { //permette di ridimensionare componenti in modo che abbiamo sempre la sua stessa dimensione
    Vector<Component> resizable = new Vector<>();
    public FullscreenLayeredPane() {
        super();
        this.setBackground(new Color(58, 61, 63));
    }

    @Override
    public void setBounds(int x, int y, int width, int height) { //in questo modo si elimina il delay che si avrebbe utilizzando un component listener
        super.setBounds(x, y, width, height);

        for (Component cmp : resizable) {
            cmp.setBounds(0, 0, width, height);
        }
    }

    public void add_fullscreen(Component comp, int index) {
        super.add(comp, index);
        resizable.add(comp);
    }
}