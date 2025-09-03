package RBNgui;

import RBNLearning.RelData;
import RBNLearning.RelDataForOneInput;
import RBNLearning.Sampler;
import RBNio.ParamListReader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class LearnModuleGUI extends JFrame implements ActionListener, MouseListener, KeyListener {

    LearnModule learnModule;
    Primula myprimula;
    PrimulaGUI myprimulaGUI;

    private boolean settingswindowopen;
    /**
     * @uml.property  name="settingswindow"
     * @uml.associationEnd  inverse="learnmodule:RBNgui.SettingsLearn"
     */
    private SettingsLearn settingswindow;

    private JTabbedPane tabbedPane = new JTabbedPane();

    private JPanel dataPanel = new JPanel(new GridLayout(2, 1));

    private JPanel numRelPanel = new JPanel(new BorderLayout());

    private JPanel lowerlearnPanel = new JPanel(new GridLayout(2, 1));

    private JPanel learnButtons = new JPanel(new FlowLayout());

    private JPanel sampleoptions = new JPanel(new GridLayout(2, 1));

    private JPanel samplesizepanel = new JPanel(new FlowLayout());

    private JPanel percmisspanel = new JPanel(new FlowLayout());

    private JPanel restartspanel = new JPanel(new FlowLayout());

    private JPanel datasrcPanel = new JPanel(new FlowLayout());

    private JPanel paramInputFields = new JPanel(new BorderLayout());

    private JFileChooser fileChooser = new JFileChooser(".");

    private javax.swing.filechooser.FileFilter myFilterRDEF;

    private JLabel samplesizelabel = new JLabel("Sample size");

    private JLabel percmisslabel = new JLabel("Percent missing");

    private JLabel restartlabel = new JLabel("Restarts");

    private JLabel paramfilelabel = new JLabel("Read from File:");

    private JTextField paramsrcfilename = new JTextField(15);

    private JButton paramsrcBrowseButton = new JButton("Browse");

    protected File paramfile;

    private JTextField textsamplesize = new JTextField(15);

    private JTextField textpercmiss = new JTextField(3);

    private JTextField textnumrestarts = new JTextField(5);

    private JButton sampleDataButton = new JButton("Sample");

    private JButton learnButton = new JButton("Learn");

    private JButton stoplearnButton = new JButton("Stop");

    private JButton setParamButton = new JButton("Set");

    private JButton learnSettingButton = new JButton("Settings");

    private JTable parametertable = new JTable();

    private JList numrellist = new JList();

    private JScrollPane parameterScrollList = new JScrollPane();

    private JScrollPane numRelScrollList = new JScrollPane();

    private JSplitPane learnsplitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, parameterScrollList, lowerlearnPanel);

    public void setupGUI() {
        fileChooser.addChoosableFileFilter(myFilterRDEF = new Filter_rdef());

        sampleDataButton.addActionListener(this);
        sampleDataButton.setBackground(PrimulaGUI.COLOR_RED);

        textsamplesize.addKeyListener(this);
        textpercmiss.addKeyListener(this);

        datasrcPanel.add(sampleDataButton);

        samplesizepanel.add(samplesizelabel);
        samplesizepanel.add(textsamplesize);
        percmisspanel.add(percmisslabel);
        percmisspanel.add(textpercmiss);

        sampleoptions.setBorder(BorderFactory.createTitledBorder("Sampling Options"));
        textsamplesize.setText("" + learnModule.getSamplesize());
        textpercmiss.setText("" + learnModule.getPercmiss());

        dataPanel.add(datasrcPanel);
        sampleoptions.add(samplesizepanel);
        sampleoptions.add(percmisspanel);
        dataPanel.add(sampleoptions);

        /* Num Rel Tab*/
        numrellist.addMouseListener(this);
        numrellist.setModel(this.learnModule.getNumRelListModel());
        numrellist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        numRelScrollList.getViewport().add(numrellist);
        numRelPanel.add(numRelScrollList, BorderLayout.NORTH);
        numRelPanel.add(paramfilelabel, BorderLayout.CENTER);
        paramInputFields.add(paramsrcfilename, BorderLayout.CENTER);
        paramInputFields.add(paramsrcBrowseButton, BorderLayout.EAST);
        numRelPanel.add(paramInputFields, BorderLayout.SOUTH);
        paramsrcfilename.addKeyListener(this);
        paramsrcBrowseButton.addActionListener(this);

        /* Learn Tab */
        /* Parameter Table */
        parametertable.setModel(this.learnModule.getParammodel());
        parametertable.getColumnModel().getColumn(0).setHeaderValue("Parameter");
        parametertable.getColumnModel().getColumn(1).setHeaderValue("Value");
        parameterScrollList.getViewport().add(parametertable);
        parameterScrollList.setMinimumSize(new Dimension(0, 100));
        learnButton.addActionListener(this);
        learnButton.setBackground(PrimulaGUI.COLOR_GREEN);
        stoplearnButton.addActionListener(this);
        stoplearnButton.setBackground(PrimulaGUI.COLOR_RED);
        setParamButton.addActionListener(this);
        setParamButton.setBackground(PrimulaGUI.COLOR_YELLOW);
        learnSettingButton.addActionListener(this);
        learnSettingButton.setBackground(PrimulaGUI.COLOR_BLUE);
        learnButtons.add(learnButton);
        learnButtons.add(stoplearnButton);
        learnButtons.add(setParamButton);
        learnButtons.add(learnSettingButton);

        textnumrestarts.setEditable(false);
        restartspanel.add(restartlabel);
        restartspanel.add(textnumrestarts);

        lowerlearnPanel.add(learnButtons);
        lowerlearnPanel.add(restartspanel);

        /* Main Pane */
        tabbedPane.add("Learning", learnsplitpane);
        tabbedPane.add("Relation Parameters", numRelPanel);
        tabbedPane.add("Sample Data", dataPanel);

        //Inner class for closing the window
        this.addWindowListener(
                new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        dispose();
                        Primula.setIsLearnModuleOpen(false);
                    }
                }
        );

        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.add(tabbedPane);

        ImageIcon icon = new ImageIcon(getClass().getResource(Primula.STR_FILENAME_LOGO));
        if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) //image ok
            this.setIconImage(icon.getImage());
        this.setTitle("Learn Module");
        this.setSize(400, 300);
        this.setVisible(true);
    }

    public LearnModuleGUI(LearnModule learnModule) {
        this.learnModule = learnModule;
        this.learnModule.setLearnModuleGUI(this);
        myprimula = learnModule.myprimula;
        myprimulaGUI = learnModule.myprimula.myprimulaGUI;
        setupGUI();
    }

    public void disableDataTab(){
        tabbedPane.setSelectedIndex(tabbedPane.indexOfComponent(learnsplitpane));
        tabbedPane.setEnabledAt(tabbedPane.indexOfComponent(dataPanel),false);
    }


    public boolean confirm(String text) {
        int result = JOptionPane.showConfirmDialog(this, text, "Confirmation", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION)
            return true;
        else //result == JOptionPane.NO_OPTION
            return false;
    }

    public String[][] getSelectedNumRels(){
        if (learnModule.isNumrelsfromfile())
            return learnModule.getNumrelblocks();
        else{
            int[] selindices = numrellist.getSelectedIndices();
            String[][] result = new String[1][selindices.length];
            for (int i=0;i<selindices.length;i++)
                result[0][i]=(String)learnModule.getNumRelListModel().elementAt(selindices[i]);
            return result;
        }
    }

    public void setSettingsOpen(boolean b) {
        settingswindowopen = b;
    }

    public boolean isSettingswindowopen() {
        return settingswindowopen;
    }

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (source == sampleDataButton) {
            if (!myprimula.getReldata().hasProbData() || confirm("Unsaved sampled data or evidence will be lost. Continue?")) {
                Sampler sampl = new Sampler();
                sampl.makeSampleStruc(myprimulaGUI);
                myprimulaGUI.showMessage("Sampling ... 0% ");
                System.out.println("Sampling ... 0% ");
                this.learnModule.setData(new RelData());
                RelDataForOneInput dataforinput = new RelDataForOneInput(myprimula.getRels());
                int completion = 0;
                for (int i = 0; i < this.learnModule.getSamplesize(); i++) {
                    dataforinput.addCase(sampl.sampleOneStrucData(this.learnModule.getPercmiss()));
                    if (10 * i / this.learnModule.getSamplesize() > completion) {
                        myprimulaGUI.appendMessage("X");
                        completion++;
                    }
                }
                this.learnModule.getData().add(dataforinput);
                myprimulaGUI.appendMessage("100%");
                myprimula.setRelData(this.learnModule.getData());
                myprimula.getInstFromReldata();
                myprimula.updateBavaria();
                myprimula.updateInstantiationInEM();
            }
        }

        if (source == learnButton) {
            this.learnModule.startLearning();
        }
        if (source == stoplearnButton) {
            this.learnModule.getLearnThread().setStopped();
        }
        if (source == setParamButton) {
            this.learnModule.setParametersPrimula();
        }

        if (source == paramsrcBrowseButton) {
            int value = fileChooser.showDialog(LearnModuleGUI.this, "Select");
            if (value == JFileChooser.APPROVE_OPTION) {
                paramfile = fileChooser.getSelectedFile();
                ParamListReader plr = new ParamListReader();
                this.learnModule.setNumrelblocks(plr.readPList(paramfile));
                paramsrcfilename.setText(paramfile.getName());
                this.learnModule.setNumrelsfromfile(true);
            }
        }

        if (source == learnSettingButton) {
            if (!isSettingswindowopen()) {
                this.settingswindow = new RBNgui.SettingsLearn(this);
                setSettingsOpen(true);
            }
        }
    }

    //  Invoked when the mouse button has been clicked (pressed and released) on a component.
    public void mouseClicked(MouseEvent e) {
        Object source = e.getSource();
    }

    //  Invoked when the mouse enters a component.
    public void mouseEntered(MouseEvent e) {
        Object source = e.getSource();
    }

    public void mouseExited(MouseEvent e) {
        Object source = e.getSource();
    }

    //           Invoked when a mouse button has been pressed on a component.
    public void mousePressed(MouseEvent e) {
        Object source = e.getSource();
        if (source == numrellist) {
            int index = numrellist.locationToIndex(e.getPoint());
//			System.out.println("current: " + StringOps.arrayToString(numrellist.getSelectedIndices(), "[", "]")  +" index: " + index);
//			if(index >= 0){
//				if (numrellist.isSelectedIndex(index)){
//					System.out.println("removing");
//					numrellist.removeSelectionInterval(index,index);
//				}
//				else{
//					System.out.println("adding");
//					numrellist.addSelectionInterval(index,index);
//				}
//			}

//			System.out.println("selection: " + StringOps.arrayToString(numrellist.getSelectedIndices(), "[", "]"));

        }
    }


    //          Invoked when a mouse button has been pressed on a component.
    public void mouseReleased(MouseEvent e) {
        Object source = e.getSource();
    }
    //          Invoked when a mouse button has been released on a component.


    public void keyPressed(KeyEvent e) {
        //Invoked when a key has been pressed.
        Object source = e.getSource();
        if (source == paramsrcfilename) {
            char c = e.getKeyChar();
            if (c == KeyEvent.VK_ENTER) {
                //(new File(paramsrcfilename.getText()));
            }
        }
    }

    public void keyTyped(KeyEvent e) {
        //Invoked when a key has been released.
    }

    public void keyReleased(KeyEvent e) {
        Object source = e.getSource();

        if (source == textsamplesize) {
            try {
                this.learnModule.setSamplesize(Integer.parseInt(textsamplesize.getText()));
            } catch (NumberFormatException exception) {
            }
        } else if (source == textpercmiss) {
            try {
                this.learnModule.setPercmiss(Double.parseDouble(textpercmiss.getText()));
            } catch (NumberFormatException exception) {
            }
        }
    }

    public JTable getParametertable() {
        return parametertable;
    }

    public JTextField getTextnumrestarts() {
        return textnumrestarts;
    }
}
