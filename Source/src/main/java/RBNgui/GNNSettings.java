package RBNgui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GNNSettings extends JFrame implements ActionListener, MouseListener, KeyListener {

    private JTextField pythonHomePath;
    private JTextField modelPath;
    private JTextField pathField3;
    private JButton saveButton;
    private JButton cancelButton;
    private JButton browseButton1;
    private JButton browseButton2;
    private JButton browseButton3;
    private Primula myprimula;


    public GNNSettings(Primula primula) {
        myprimula = primula;
        this.setVisible(true);

        setTitle("GNN Settings");
        setSize(600, 250);
        setLayout(new GridLayout(4, 3));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        pythonHomePath = new JTextField();
        modelPath = new JTextField();
        pathField3 = new JTextField();
        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");
        browseButton1 = new JButton("Browse...");
        browseButton2 = new JButton("Browse...");
        browseButton3 = new JButton("Browse...");

        add(new JLabel("Python home:"));
        add(pythonHomePath);
        add(browseButton1);
        add(new JLabel("Model path:"));
        add(modelPath);
        add(browseButton2);
        add(new JLabel("Path 3:"));
        add(pathField3);
        add(browseButton3);
        add(saveButton);
        add(cancelButton);

        saveButton.addActionListener(this);
        cancelButton.addActionListener(this);
        browseButton1.addActionListener(this);
        browseButton2.addActionListener(this);
        browseButton3.addActionListener(this);

        pythonHomePath.addKeyListener(this);
        modelPath.addKeyListener(this);
        pathField3.addKeyListener(this);
        saveButton.addMouseListener(this);
        cancelButton.addMouseListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == saveButton) {
            String path1 = pythonHomePath.getText();
            String path2 = modelPath.getText();
            String path3 = pathField3.getText();
            System.out.println("Saved paths");

            // Add logic
        } else if (e.getSource() == cancelButton) {
            System.out.println("Operation cancelled.");
            pythonHomePath.setText("");
            modelPath.setText("");
            pathField3.setText("");
        } else if (e.getSource() == browseButton1) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                pythonHomePath.setText(fileChooser.getSelectedFile().getPath());
            }
        } else if (e.getSource() == browseButton2) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                modelPath.setText(fileChooser.getSelectedFile().getPath());
            }
        } else if (e.getSource() == browseButton3) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                pathField3.setText(fileChooser.getSelectedFile().getPath());
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}
}
