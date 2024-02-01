package gui;

import file_database.Database;
import file_database.File_interface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ButtonTopBar_panel {
    public static String active_mod = "";

    private static JPanel buttons_list;
    private static JScrollPane buttons_scroller;
    private static Map<String, Runnable> stop_activities = new LinkedHashMap<>();

    private static JPanel buttons_panel = null;
    protected static JPanel init() throws IOException {
        if (buttons_list == null) {
            buttons_panel = new JPanel();
            buttons_panel.setLayout(new GridBagLayout());

            //inizializza tutti i componenti della gui
            JButton right_shift = new JButton();
            JButton left_shift = new JButton();
            JButton stop = new JButton();
            buttons_list = new JPanel();
            buttons_scroller = new JScrollPane(buttons_list, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            stop.setEnabled(false);
            stop.setPreferredSize(new Dimension(30, 30));
            buttons_list.add(stop);

            right_shift.setIcon(new ImageIcon(File_interface.jar_path + "/images/right_arrow.png"));
            right_shift.setRolloverIcon(new ImageIcon(File_interface.jar_path + "/images/right_arrow_sel.png"));
            right_shift.setPressedIcon(new ImageIcon(File_interface.jar_path + "/images/right_arrow_pres.png"));
            left_shift.setIcon(new ImageIcon(File_interface.jar_path + "/images/left_arrow.png"));
            left_shift.setRolloverIcon(new ImageIcon(File_interface.jar_path + "/images/left_arrow_sel.png"));
            left_shift.setPressedIcon(new ImageIcon(File_interface.jar_path + "/images/left_arrow_pres.png"));
            stop.setIcon(new ImageIcon(File_interface.jar_path + "/images/power_off.png"));
            stop.setRolloverIcon(new ImageIcon(File_interface.jar_path + "/images/power_off_sel.png"));
            stop.setPressedIcon(new ImageIcon(File_interface.jar_path + "/images/power_off_pres.png"));
            stop.setDisabledIcon(new ImageIcon(File_interface.jar_path + "/images/power_off_dis.png"));

            right_shift.setBorder(null);
            left_shift.setBorder(null);
            stop.setBorder(null);
            buttons_scroller.setBorder(null);

            right_shift.addActionListener(right_shift_listener);
            left_shift.addActionListener(left_shift_listener);
            stop.addActionListener(stop_listener);

            buttons_list.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
            buttons_list.setBackground(new Color(58, 61, 63));

            //aggiunge tutti i componenti al pannello organizzandoli nella griglia
            GridBagConstraints c = new GridBagConstraints();

            c.fill = GridBagConstraints.BOTH;
            c.gridy = 0;
            c.weightx = 0; //i due pulsanti non vengono ridimensionati

            c.gridx = 0;
            buttons_panel.add(left_shift, c);

            c.gridx = 2;
            buttons_panel.add(right_shift, c);

            c.weightx = 1;

            c.gridx = 1;
            buttons_panel.add(buttons_scroller, c);

            buttons_panel.setPreferredSize(new Dimension(0, 30));
        }
        return buttons_panel;
    }

    public static void setEnabled(boolean enabled) {
        buttons_list.setEnabled(enabled);
        for (Component c : buttons_list.getComponents()) { //disabilita tutti i bottoni registrati
            c.setEnabled(enabled);
        }
    }

    public static void init_buttons() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CentralTerminal_panel.terminal_write("inizializzo i bottoni nella gui:", false);
        File buttons_folder = new File(File_interface.jar_path + "/mod");
        String[] button_class_files = buttons_folder.list();

        class Button_class extends ClassLoader {
            public Class find_class(String class_name) throws IOException {
                byte[] class_data = new FileInputStream(File_interface.jar_path + "/mod/" + class_name + ".class").readAllBytes();
                return defineClass(class_name, class_data, 0, class_data.length); //define class
            }
        }
        Button_class class_gen = new Button_class();

        for (String file_name : button_class_files) {
            CentralTerminal_panel.terminal_write("\n   inizializzo - " + file_name + ": ", false);

            if (file_name.substring(file_name.length() - 6, file_name.length()).equals(".class")) { //se è un file .class
                String class_name = file_name.substring(0, file_name.length() - 6);
                Class button_class = class_gen.find_class(class_name);

                //imposta la funzione "public static void on_press(String name)" all'interno della classe per essere invocata una volta premuto un pulsante
                ButtonTopBar_panel.on_press = button_class.getDeclaredMethod("on_press", String.class);

                //all'interno della classe dovrà essere definita una funzione "public static void register_button()" che viene invocata ora per far registrare tutti i bottoni
                button_class.getDeclaredMethod("register_button").invoke(null);
            }
        }
        CentralTerminal_panel.terminal_write(" - finito\n", false);
    }

    private static Method on_press;
    public static void register_button(ButtonInfo info, Runnable stop) {
        stop_activities.put(info.name, stop); //registra il metodo per stoppare l'azione di questo pulsante
        CentralTerminal_panel.terminal_write("pulsante registrato!\n", false);

        JButton b = new JButton();
        b.setToolTipText(info.name);

        b.setIcon(info.default_icon);
        b.setRolloverIcon(info.rollover_icon);
        b.setPressedIcon(info.pressed_icon);
        b.setDisabledIcon(info.disabled_icon);

        b.addActionListener(new ActionListener() {
            private final Method on_press = ButtonTopBar_panel.on_press;
            private final String name = b.getToolTipText();
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (ButtonTopBar_panel.active_mod.equals("")) {
                        ButtonTopBar_panel.active_mod = name;
                        on_press.invoke(null, name);
                    }
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                } catch (InvocationTargetException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        b.setPreferredSize(new Dimension(info.default_icon.getIconWidth(), info.default_icon.getIconHeight()));
        b.setBorder(null);
        b.setEnabled(false);

        buttons_list.add(b);
        buttons_scroller.updateUI();
        buttons_scroller.setBorder(null); //altrimenti con updateUI() si mostra il bordo
    }

    public static void end_mod() {
        if (!ButtonTopBar_panel.active_mod.equals("")) { //se c'è effettivamente una mod attiva
            stop_activities.get(ButtonTopBar_panel.active_mod).run();
            ButtonTopBar_panel.active_mod = "";
            CentralTerminal_panel.reset_panel();
        }
    }

    private static ActionListener left_shift_listener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            buttons_scroller.getHorizontalScrollBar().setValue(
                    buttons_scroller.getHorizontalScrollBar().getValue() - 30
            );
        }
    };

    private static ActionListener right_shift_listener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            buttons_scroller.getHorizontalScrollBar().setValue(
                    buttons_scroller.getHorizontalScrollBar().getValue() + 30
            );
        }
    };

    private static ActionListener stop_listener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            end_mod();
        }
    };
}
