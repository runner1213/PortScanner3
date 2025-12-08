package org.cats;

import org.cats.gui.Initialize;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }


        javax.swing.SwingUtilities.invokeLater(Initialize::new);
    }
}