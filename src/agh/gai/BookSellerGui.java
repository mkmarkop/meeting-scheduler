package agh.gai;

import jade.core.AID;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

class BookSellerGui extends JFrame {
    private BookSellerAgent myAgent;

    private JTextField titleField, priceField, shippingField;

    BookSellerGui(BookSellerAgent a) {
        super(a.getLocalName());

        myAgent = a;

        JPanel p = new JPanel();
        p.setLayout(new GridLayout(3, 2));
        p.add(new JLabel("Title:"));
        titleField = new JTextField(15);
        p.add(titleField);
        p.add(new JLabel("Price:"));
        priceField = new JTextField(15);
        p.add(priceField);
        p.add(new JLabel("Shipping cost:"));
        shippingField = new JTextField(15);
        p.add(shippingField);
        getContentPane().add(p, BorderLayout.CENTER);

        JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    String title = titleField.getText().trim();
                    String price = priceField.getText().trim();
                    String shippingCost = shippingField.getText().trim();
                    myAgent.updateCatalogue(
                            new Book(title,
                                    Integer.parseInt(price),
                                    Integer.parseInt(shippingCost)));
                    titleField.setText("");
                    priceField.setText("");
                    shippingField.setText("");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(BookSellerGui.this, "Invalid values. " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        p = new JPanel();
        p.add(addButton);
        getContentPane().add(p, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                myAgent.doDelete();
            }
        });

        setResizable(false);
    }

    public void display() {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int) screenSize.getWidth() / 2;
        int centerY = (int) screenSize.getHeight() / 2;
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
        setVisible(true);
    }
}