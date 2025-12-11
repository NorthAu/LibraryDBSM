package com.library;

import com.library.ui.LibraryFrame;

import javax.swing.SwingUtilities;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LibraryFrame().setVisible(true));
    }
}
