package org.openscience.chimetojmol;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import org.jmol.util.JmolList;

import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;


public class ChimePanel extends JPanel implements ItemListener, ActionListener {

  private JTextField chimePath;
  private JButton goButton, browseButton;
  private JTextArea logArea;
  private JScrollPane logScrollPane;
  private JFileChooser chooser;
  private File oldDir;
  private JmolList<File> pages;
  private int nDir;
  private int nFiles;
  //private int nControls;

  private Checkbox checkSubs, checkFilenames, checkSigned;
  private boolean doSubdirectories;
  private boolean doFixFilenames;
  private boolean doUseSigned;
  private File myDir;

  ChimePanel() {

    chooser = new JFileChooser();
    chooser.setCurrentDirectory(new File("."));
    myDir = chooser.getCurrentDirectory();
    chooser.setDialogTitle("Select a Directory");
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    chooser.setAcceptAllFileFilterUsed(false);

    setLayout(new BorderLayout());

    chimePath = new JTextField(50);
    chimePath.addActionListener(this);
    chimePath.setText("c:/temp/Teaching");

    JPanel pathPanel = new JPanel();
    pathPanel.setLayout(new BorderLayout());
    pathPanel
        .setBorder(BorderFactory
            .createTitledBorder("Directory containing Chime-based HTML pages to convert"));
    pathPanel.add("West", chimePath);
    browseButton = new JButton("browse...");
    browseButton.addActionListener(this);
    pathPanel.add("East", browseButton);
    add("North", pathPanel);

    JPanel checkPanel = new JPanel();
    checkPanel.setLayout(new BorderLayout());
    checkSubs = new Checkbox("include subdirectories");
    checkSubs.addItemListener(this);
    checkFilenames = new Checkbox("fix file name case");
    checkFilenames.addItemListener(this);
    checkSigned = new Checkbox("use signed applet");
    checkSigned.addItemListener(this);
    checkPanel.add("North", checkSubs);
    checkPanel.add("Center", checkFilenames);
    checkPanel.add("South", checkSigned);
    add("Center", checkPanel);

    JPanel lowerPanel = new JPanel();
    lowerPanel.setLayout(new BorderLayout());
    JPanel goPane = new JPanel();
    goPane.setSize(30, 10);
    goButton = new JButton("Convert Page(s)");
    goButton.addActionListener(this);
    goPane.add(goButton);
    lowerPanel.add("North", goPane);

    logArea = new JTextArea(30, 20);
    logArea.setMargin(new Insets(5, 5, 5, 5));
    logArea.setEditable(false);
    logScrollPane = new JScrollPane(logArea);
    logScrollPane.setBorder(BorderFactory.createTitledBorder("0 pages"));

    lowerPanel.add("South", logScrollPane);

    add("South", lowerPanel);

  }

  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source == goButton) {
      doGo();
    } else if (source == browseButton) {
      doBrowse();
    }
  }

  public void itemStateChanged(ItemEvent e) {
    Object source = e.getSource();
    int stateChange = e.getStateChange();
    if (source == checkSubs) {
      doSubdirectories = (stateChange == ItemEvent.SELECTED);
      getFileList();
    }
    if (source == checkFilenames) {
      doFixFilenames = (stateChange == ItemEvent.SELECTED);
    }
    if (source == checkSigned) {
      doUseSigned = (stateChange == ItemEvent.SELECTED);
    }
  }

  private void log(String string) {
    logArea.setText(logArea.getText() + string + "\n");
  }

  void getFileList() {
    logArea.setText("");
    pages = new  JmolList<File>();
    String dir = chimePath.getText();
    dir = dir.replace('\\', '/');
    while (dir.endsWith("/"))
      dir = dir.substring(0, dir.length() - 1);
    if (dir.length() < 4)
      return;
    oldDir = new File(dir);
    try {
      copyDirectory("", oldDir, new File(oldDir + "_jmol"), true);
    } catch (IOException e) {
      log(e.getMessage());
    }
  }

  private void doGo() {
    getFileList();
    try {
      copyDirectory("", oldDir, new File(oldDir + "_jmol"), false);
    } catch (IOException e) {
      logArea.setText(e.getMessage());
    }
  }

  private void doBrowse() {
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      String dir = chooser.getSelectedFile().toString();
      chimePath.setText(dir);
      getFileList();
    }
  }

  private String rootDir;
  
  private void copyDirectory(String level, File sourceLocation,
                             File targetLocation, boolean justChecking)
      throws IOException {
    if (level.equals("")) {
      nDir = nFiles = 0;
      rootDir = targetLocation.getAbsolutePath();
      if (!justChecking) {
        deleteDirectory(targetLocation);
        targetLocation.mkdir();
        addJmolFiles(rootDir);
      }
    } else if (doFixFilenames) {
      targetLocation = new File(fixFileName(targetLocation));
    }
    if (sourceLocation.isDirectory()) {
      if (!doSubdirectories && !level.equals(""))
        return;
      nDir++;
      if (!targetLocation.exists() && !justChecking)
        targetLocation.mkdir();
      String[] children = sourceLocation.list();
      for (int i = 0; i < children.length; i++)
        copyDirectory((level.equals("") ? "." : level.equals(".") ? ".."
            : level + "/.."), new File(sourceLocation, children[i]), new File(
            targetLocation, children[i]), justChecking);
    } else {
      if (!copyFile(level, sourceLocation, targetLocation, justChecking))
        log("Hmm..." + sourceLocation + " --> " + targetLocation);
      nFiles++;
    }
    showProgress();
  }

  private void addJmolFiles(String rootDir) {
    File dir = myDir;
    if (!new File(dir, "Jmol.js").exists())
      dir = oldDir;
    File dest = new File(rootDir);

    String[] list = dir.list();
    for (int i = 0; i < list.length; i++) {
      String f = list[i];
      if (!f.equals("Jmol.js")) {
        if (!f.startsWith("JmolApplet") || !f.endsWith(".jar")
            || doUseSigned != (f.indexOf("AppletSigned") >= 0))
          continue;
      }
      justTransferFile(new File(dir, f), new File(dest, f), null);
    }
    transferResource(dir, "chimebtn16.bin", dest, "chimebtn16.png");
    transferResource(dir, "ChimeToJmol.js", dest, "ChimeToJmol.js");
  }

  private void transferResource(File dir, String name, File dest, String nameOut) {
    File file = new File(dir, name);
    File fileOut = new File(dest, nameOut);
    if (file.exists())
      justTransferFile(file, fileOut, null);
    else
      justTransferFile(null, fileOut, getResourceStream(name));
  }

  public static boolean deleteDirectory(File directory) {
    if (directory == null)
      return false;
    if (!directory.exists())
      return true;
    if (!directory.isDirectory())
      return false;
    String[] list = directory.list();
    if (list != null) {
      for (int i = 0; i < list.length; i++) {
        File entry = new File(directory, list[i]);
        if (entry.isDirectory()) {
          if (!deleteDirectory(entry))
            return false;
        } else {
          if (!entry.delete())
            return false;
        }
      }
    }
    return directory.delete();
  }
  private String fixFileName(File f) {
    return (rootDir + "/" + f.getAbsolutePath().substring(rootDir.length()).toLowerCase()).replace('\\','/');
  }

  private void showProgress() {
    String s = pages.size() + " pages/"
        + (nDir > 1 ? nDir + " directories/" : "") + nFiles + " files";
    logScrollPane.setBorder(BorderFactory.createTitledBorder(s));
  }

  private boolean copyFile(String level, File f1, File f2, boolean justChecking) {
    String name = f1.getName().toLowerCase();
    if (name.endsWith(".htm") || name.endsWith(".html")) {
      if (justChecking) {
        pages.addLast(f1);
        log(f1.getAbsolutePath());
        return true;
      }
      log("---\n" + f1.getAbsolutePath() + " --> " + f2.getAbsolutePath());
      return processFile(level, f1, f2, true, true);
    } 
    if (justChecking)
      return true;
    if (name.endsWith(".spt")) {
      log("---\n" + f1.getAbsolutePath() + " --> " + f2.getAbsolutePath());
      return processFile(level, f1, f2, false, true);
    }
    return justTransferFile(f1, f2, null);
  }

  private boolean justTransferFile(File f1, File f2, InputStream in) {
    try {
      if (f1 != null)
        in = new FileInputStream(f1);
      OutputStream out = new FileOutputStream(f2);

      byte[] buf = new byte[1024];
      int len;
      while ((len = in.read(buf)) > 0) {
        out.write(buf, 0, len);
      }
      in.close();
      out.close();
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  private static Pattern embed1 = Pattern.compile("<embed", Pattern.CASE_INSENSITIVE);
  private static Pattern embed2 = Pattern.compile("</embed", Pattern.CASE_INSENSITIVE);

  private boolean processFile(String level, File f1, File f2,
                              boolean processHtml, boolean processChime) {
    String data = getFileContents(f1);
    if (data == null) {
      log("?error reading " + f1.getAbsolutePath());
      return false;
    }
    if (doFixFilenames)
      data = fixFileNames(data, processHtml);
    if (processHtml && data.indexOf("Jmol.js") < 0) {
      String opener = "\n<script type=\"text/javascript\"";
      String s = opener + " src=\"" + level + "/Jmol.js\"></script>";
      s += opener + " src=\"" + level + "/ChimeToJmol.js\"></script>";
      s += opener + ">jmolInitialize('" + level + "'," + doUseSigned + ");chimebtn = '" + level
          + "/chimebtn16.png';</script>";
      int i = data.toLowerCase().indexOf("<head>");
      if (i < 0) {
        data = "<head></head>" + data;
        i = 0;
      }
      data = data.substring(0, i + 6) + s + "\n" + data.substring(i + 6);
      data = embed1.matcher(data).replaceAll("<xembed");
      data = embed2.matcher(data).replaceAll("</xembed");
    }
    if (processChime) {
      data = fixChime(data, processHtml);
    }
    if (!putFileContents(f2, data)) {
      log("?error creating " + f2);
      return false;
    }
    return true;
  }

  private String fixFileNames(String data, boolean isHtml) {
    if (isHtml) {
      data = fixFileNames(data, "src=", '\0');
      data = fixFileNames(data, "script=", '\0');
    }
    if (data.startsWith("load"))
      data = "\n" + data + "\n";
    data = fixFileNames(data, "\nload", '\n');
    return data;
  }

  private String fixFileNames(String data, String what, char term) {
    int i = -1;
    boolean isScript = what.equals("script=");
    String lcdata = data.toLowerCase();
    StringBuilder dataOut = new StringBuilder();
    int pt0 = 0;
    int i1 = 0;
    while ((i = lcdata.indexOf(what, i + 1)) >= 0) {
      if (term == '\n') {
        i1 = data.indexOf(term, i + 1);
      } else {
        i1 = i + what.length();
        if (i1 == data.length())
          break;
        boolean stopDQuote = data.charAt(i1) == '"';
        boolean stopSQuote = data.charAt(i1) == '\'';
        boolean stopSpace = (!stopDQuote && !stopSQuote);
        if (stopDQuote || stopSQuote)
          i1++;
        if (stopSpace) 
          while(i1 < data.length() && Character.isWhitespace(data.charAt(i1)))
            i1++;
        out: for (; i1 < data.length(); i1++) {
          if (stopSpace && Character.isWhitespace(data.charAt(i1)))
            break;
          switch (data.charAt(i1)) {
          case '"':
            if (stopDQuote)
              break out;
            break;
          case '\'':
            if (stopSQuote)
              break out;
            break;
          case '>':
            break out;
          }
        }
      }
      String s;
      if (isScript) {
        // slight potential for error here when ; is quoted
        s = fixFileNames(data.substring(i, i1).replace(';','\n'), false).replace('\n', ';'); 
      } else {
        s = lcdata.substring(i, i1);
      }        
      log(data.substring(i, i1) + " --> " + s);
      dataOut.append(data.substring(pt0, i)).append(s);
      pt0 = i1;
    }
    dataOut.append(data.substring(pt0, data.length()));
    return dataOut.toString();
  }

  /**
   * @param data 
   * @param isHtml  
   * @return  fixed Chime commands 
   */
  private String fixChime(String data, boolean isHtml) {
    // (JavaScript will do this)
    return data;
  }

  private String getFileContents(File f) {
    StringBuilder sb = new StringBuilder(8192);
    String line;
    try {
      BufferedReader br = new BufferedReader(new FileReader(f));
      while ((line = br.readLine()) != null)
        sb.append(line).append('\n');
      br.close();
    } catch (IOException e) {
      return null;
    }
    return sb.toString();
  }

  private boolean putFileContents(File f, String html) {
    FileWriter fstream;
    try {
      fstream = new FileWriter(f, false);
      BufferedWriter out = new BufferedWriter(fstream);
      out.write(html);
      out.close();
    } catch (IOException e) {
      return false;
    }
    return true;
  }

  static InputStream getResourceStream(String fileName) {
    URL url = null;
    fileName = "org/openscience/chimetojmol/" + fileName;
    try {
      if ((url = ClassLoader.getSystemResource(fileName)) == null)
        return null;
      return (InputStream) url.getContent();
    } catch (Exception e) {
      return null;
    }
  }

}
