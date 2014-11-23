/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-26 23:35:44 -0500 (Fri, 26 Jun 2009) $
 * $Revision: 11131 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.openscience.jmol.app.jmolpanel;

import org.jmol.api.Interface;
import org.jmol.api.JmolAbstractButton;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolViewer;
import org.jmol.awt.Platform;
import org.jmol.export.JmolFileDropper;
import org.jmol.export.dialog.Dialog;
import org.jmol.export.history.HistoryFile;
import org.jmol.export.image.AwtImageCreator;
import org.jmol.i18n.GT;
import org.openscience.jmol.app.jmolpanel.console.AppConsole;
import org.openscience.jmol.app.jmolpanel.console.ConsoleTextArea;
import org.openscience.jmol.app.jsonkiosk.BannerFrame;
import org.openscience.jmol.app.jsonkiosk.JsonNioClient;
import org.openscience.jmol.app.jsonkiosk.JsonNioServer;
import org.openscience.jmol.app.jsonkiosk.KioskFrame;
import org.openscience.jmol.app.surfacetool.SurfaceTool;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.Jmol;
import org.openscience.jmol.app.JmolApp;
import org.openscience.jmol.app.SplashInterface;
import org.openscience.jmol.app.webexport.WebExport;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.jmol.util.JmolList;
import java.util.Arrays;
import java.util.Hashtable;

import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

public class JmolPanel extends JPanel implements SplashInterface, JsonNioClient {

  static HistoryFile historyFile;

  public JmolViewer viewer;

  JmolAdapter modelAdapter;
  JmolApp jmolApp;
  StatusBar status;
  int startupWidth, startupHeight;
  JsonNioServer serverService;

  protected String appletContext;
  protected PropertyChangeSupport pcs = new PropertyChangeSupport(this);
  protected DisplayPanel display;
  protected GaussianDialog gaussianDialog;
  protected RecentFilesDialog recentFiles;
  protected AtomSetChooser atomSetChooser;
  protected JFrame frame;
  protected SplashInterface splash;
  protected JFrame consoleframe;  
  protected JsonNioServer service;
  protected int qualityJPG = -1;
  protected int qualityPNG = -1;
  protected String imageType;

  protected GuiMap guimap = new GuiMap();
  private ExecuteScriptAction executeScriptAction;
  private PreferencesDialog preferencesDialog;
  private StatusListener myStatusListener;
  private SurfaceTool surfaceTool;

  private Map<String, Action> commands;
  private Map<String, JMenuItem> menuItems;
  private JMenuBar menubar;
  private JToolBar toolbar;

  // --- action implementations -----------------------------------

  private ExportAction exportAction = new ExportAction();
  private PovrayAction povrayAction = new PovrayAction();
  private ToWebAction toWebAction = new ToWebAction();
  private WriteAction writeAction = new WriteAction();
  private PrintAction printAction = new PrintAction();
  private CopyImageAction copyImageAction = new CopyImageAction();
  private CopyScriptAction copyScriptAction = new CopyScriptAction();
  private SurfaceToolAction surfaceToolAction = new SurfaceToolAction();
  private PasteClipboardAction pasteClipboardAction = new PasteClipboardAction();
  private ViewMeasurementTableAction viewMeasurementTableAction = new ViewMeasurementTableAction();

  private Map<String, Object> viewerOptions;

  private static int numWindows = 0;
  private static KioskFrame kioskFrame;
  private static BannerFrame bannerFrame;
  
  // Window names for the history file
  private final static String EDITOR_WINDOW_NAME = "ScriptEditor";
  private final static String SCRIPT_WINDOW_NAME = "ScriptWindow";
  private final static String FILE_OPEN_WINDOW_NAME = "FileOpen";
  private final static String WEB_MAKER_WINDOW_NAME = "JmolWebPageMaker";
  private final static String SURFACETOOL_WINDOW_NAME = "SurfaceToolWindow";
  private final static Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
  
  // these correlate with items xxx in GuiMap.java 
  // that have no associated xxxScript property listed
  // in org.openscience.jmol.Properties.Jmol-resources.properties

  private static final String newwinAction = "newwin";
  private static final String openAction = "open";
  private static final String openurlAction = "openurl";
  private static final String openpdbAction = "openpdb";
  private static final String openmolAction = "openmol";
  private static final String newAction = "new";
  private static final String exportActionProperty = "export";
  private static final String closeAction = "close";
  private static final String exitAction = "exit";
  private static final String aboutAction = "about";
  private static final String whatsnewAction = "whatsnew";
  private static final String uguideAction = "uguide";
  private static final String printActionProperty = "print";
  private static final String recentFilesAction = "recentFiles";
  private static final String povrayActionProperty = "povray";
  private static final String writeActionProperty = "write";
  private static final String editorAction = "editor";
  private static final String consoleAction = "console";
  private static final String toWebActionProperty = "toweb";
  private static final String atomsetchooserAction = "atomsetchooser";
  private static final String copyImageActionProperty = "copyImage";
  private static final String copyScriptActionProperty = "copyScript";
  private static final String surfaceToolActionProperty = "surfaceTool";  private static final String pasteClipboardActionProperty = "pasteClipboard";
  private static final String gaussianAction = "gauss";
  private static final String resizeAction = "resize";
  //private static final String saveasAction = "saveas";
  //private static final String vibAction = "vibrate";

  public JmolPanel(JmolApp jmolApp, Splash splash, JFrame frame,
      JmolPanel parent, int startupWidth, int startupHeight,
      Map<String, Object> viewerOptions, Point loc) {
    super(true);
    this.jmolApp = jmolApp;
    this.frame = frame;
    this.startupWidth = startupWidth;
    this.startupHeight = startupHeight;
    historyFile = jmolApp.historyFile;
    numWindows++;

    try {
      say("history file is " + historyFile.getFile().getAbsolutePath());
    } catch (Exception e) {
    }

    frame.setTitle("Jmol");
    frame.getContentPane().setBackground(Color.lightGray);
    frame.getContentPane().setLayout(new BorderLayout());

    this.splash = splash;

    setBorder(BorderFactory.createEtchedBorder());
    setLayout(new BorderLayout());

    status = new StatusBar();
    say(GT._("Initializing 3D display..."));

    // only the SmarterJmolAdapter is allowed -- just send it in as null

    /*
     * String adapter = System.getProperty("model"); if (adapter == null ||
     * adapter.length() == 0) adapter = "smarter"; if
     * (adapter.equals("smarter")) { report("using Smarter Model Adapter");
     * modelAdapter = new SmarterJmolAdapter(); } else if
     * (adapter.equals("cdk")) {report(
     * "the CDK Model Adapter is currently no longer supported. Check out http://bioclipse.net/. -- using Smarter"
     * ); // modelAdapter = new CdkJmolAdapter(null); modelAdapter = new
     * SmarterJmolAdapter(); } else { report("unrecognized model adapter:" +
     * adapter + " -- using Smarter"); modelAdapter = new SmarterJmolAdapter();
     * }
     */

    /*
     * this version of Jmol needs to have a display so that it can 
     * construct JPG images -- if that is not needed, then you can
     * use JmolData.jar
     * 
     */
    display = new DisplayPanel(this);
    viewerOptions.put("display", display);
    myStatusListener = new StatusListener(this, display);
    viewerOptions.put("statusListener", myStatusListener);
    if (modelAdapter != null)
      viewerOptions.put("modelAdapter", modelAdapter);
    this.viewerOptions = viewerOptions;
    viewer = new Viewer(viewerOptions);
    display.setViewer(viewer);
    myStatusListener.setViewer(viewer);

    if (!jmolApp.haveDisplay)
      return;
    getDialogs();
    say(GT._("Initializing Script Window..."));
    viewer.getProperty("DATA_API", "getAppConsole", Boolean.TRUE);

    // Setup Plugin system
    // say(GT._("Loading plugins..."));
    // pluginManager = new CDKPluginManager(
    // System.getProperty("user.home") + System.getProperty("file.separator")
    // + ".jmol", new JmolEditBus(viewer)
    // );
    // pluginManager.loadPlugin("org.openscience.cdkplugin.dirbrowser.DirBrowserPlugin");
    // pluginManager.loadPlugin("org.openscience.cdkplugin.dirbrowser.DadmlBrowserPlugin");
    // pluginManager.loadPlugins(
    // System.getProperty("user.home") + System.getProperty("file.separator")
    // + ".jmol/plugins"
    // );
    // feature to allow for globally installed plugins
    // if (System.getProperty("plugin.dir") != null) {
    // pluginManager.loadPlugins(System.getProperty("plugin.dir"));
    // }

    // install the command table
    say(GT._("Building Command Hooks..."));
    commands = new Hashtable<String, Action>();
    if (display != null) {
      Action[] actions = getActions();
      for (int i = 0; i < actions.length; i++) {
        Action a = actions[i];
        commands.put(a.getValue(Action.NAME).toString(), a);
      }
    }

    if (jmolApp.isKiosk) {
      add("Center", display);
    } else {
      JPanel panel = new JPanel();
      menuItems = new Hashtable<String, JMenuItem>();
      say(GT._("Building Menubar..."));
      executeScriptAction = new ExecuteScriptAction();
      menubar = createMenubar();
      add("North", menubar);
      panel.setLayout(new BorderLayout());
      toolbar = createToolbar();
      panel.add("North", toolbar);
      JPanel ip = new JPanel();
      ip.setLayout(new BorderLayout());
      ip.add("Center", display);
      panel.add("Center", ip);
      add("Center", panel);
      add("South", status);
    }

    say(GT._("Starting display..."));
    display.start();

    if (jmolApp.isKiosk) {
      bannerFrame = new BannerFrame(jmolApp.startupWidth, 75);
    } else {
      // prevent new Jmol from covering old Jmol
      if (loc != null) {
        frame.setLocation(loc);
      } else if (parent == null) {
        loc = historyFile.getWindowPosition("Jmol");
        if (loc != null)
          frame.setLocation(loc);
      } else {
        loc = parent.frame.getLocationOnScreen();
        int maxX = screenSize.width - 50;
        int maxY = screenSize.height - 50;
        loc.x += 40;
        loc.y += 40;
        if (loc.x > maxX || loc.y > maxY)
          loc.setLocation(0, 0);
        frame.setLocation(loc);
      }
    }
    frame.getContentPane().add("Center", this);
    frame.addWindowListener(new AppCloser());
    frame.pack();
    frame.setSize(startupWidth, startupHeight);
    ImageIcon jmolIcon = JmolResourceHandler.getIconX("icon");
    Image iconImage = jmolIcon.getImage();
    frame.setIconImage(iconImage);

    // Repositioning windows

    //historyFile.repositionWindow("Jmol", getFrame(), 300, 300);

    AppConsole console = (AppConsole) viewer.getProperty("DATA_API",
        "getAppConsole", null);
    if (console != null && console.jcd != null) {
      historyFile.repositionWindow(SCRIPT_WINDOW_NAME, console.jcd, 200, 100,
          !jmolApp.isKiosk);
    }
    // this just causes problems
    //c = (Component) viewer.getProperty("DATA_API","getScriptEditor", null);
    //if (c != null)
    //historyFile.repositionWindow(EDITOR_WINDOW_NAME, c, 150, 50);

    say(GT._("Setting up Drag-and-Drop..."));
    new JmolFileDropper(myStatusListener, viewer);
    // it's important to set this up first, even though it consumes some memory
    // otherwise, loading a new model in a script that sets the vibration or vector parameters
    // can appear to skip those -- they aren't skipped, but creating the atomSetChooser
    // will run scripts as it loads.
    atomSetChooser = new AtomSetChooser(viewer, frame);
    pcs.addPropertyChangeListener(chemFileProperty, atomSetChooser);
    say(GT._("Launching main frame..."));
  }

  private void getDialogs() {
    say(GT._("Initializing Preferences..."));
    preferencesDialog = new PreferencesDialog(this, frame, guimap, viewer);
    say(GT._("Initializing Recent Files..."));
    recentFiles = new RecentFilesDialog(frame);
    if (jmolApp.haveDisplay) {
      if (display.measurementTable != null)
        display.measurementTable.dispose();
      display.measurementTable = new MeasurementTable(viewer, frame);
    }
  }

  protected static void startJmol(JmolApp jmolApp) {

    Dialog.setupUIManager();

    JFrame jmolFrame;

    if (jmolApp.isKiosk) {
      if (jmolApp.startupWidth < 100 || jmolApp.startupHeight < 100) {
        jmolApp.startupWidth = screenSize.width;
        jmolApp.startupHeight = screenSize.height - 75;
      }
      jmolFrame = kioskFrame = new KioskFrame(0, 75, jmolApp.startupWidth,
          jmolApp.startupHeight, null);
    } else {
      jmolFrame = new JFrame();
    }

    // now pass these to viewer

    Jmol jmol = null;

    try {
      if (jmolApp.jmolPosition != null) {
        jmolFrame.setLocation(jmolApp.jmolPosition);
      }

      jmol = getJmol(jmolApp, jmolFrame);

      // scripts are read and files are loaded now
      jmolApp.startViewer(jmol.viewer, jmol.splash, false);

    } catch (Throwable t) {
      Logger.error("uncaught exception: " + t);
      t.printStackTrace();
    }

    if (jmolApp.haveConsole)
      getJavaConsole(jmol);

    if (jmolApp.isKiosk) {
      kioskFrame.setPanel(jmol);
      bannerFrame.setLabel("click below and type exitJmol[enter] to quit");
      jmol.viewer.script("set allowKeyStrokes;set zoomLarge false;");
    }
    if (jmolApp.port > 0) {
      try {
        jmol.service = getJsonNioServer();
        jmol.service.startService(jmolApp.port, jmol, jmol.viewer, "-1", 1);
//        JsonNioService service2 = new JsonNioService();
//        service2.startService(jmolApp.port, jmol, null, "-2");
//        service2.sendMessage(null, "test", null);
      } catch (Throwable e) {
        e.printStackTrace();
        if (bannerFrame != null) {
          bannerFrame.setLabel("could not start NIO service on port " + jmolApp.port);
        }
        if (jmol.service != null)
          jmol.service.close();
      }

    }
  }

  private static void getJavaConsole(Jmol jmol) {
    // Adding console frame to grab System.out & System.err
    jmol.consoleframe = new JFrame(GT._("Jmol Java Console"));
    jmol.consoleframe.setIconImage(jmol.frame.getIconImage());
    try {
      final ConsoleTextArea consoleTextArea = new ConsoleTextArea(true);
      consoleTextArea.setFont(java.awt.Font.decode("monospaced"));
      jmol.consoleframe.getContentPane().add(new JScrollPane(consoleTextArea),
          java.awt.BorderLayout.CENTER);
        JButton buttonClear = jmol.guimap.newJButton("JavaConsole.Clear");
        buttonClear.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          consoleTextArea.setText("");
        }
        });
        jmol.consoleframe.getContentPane().add(buttonClear,
            java.awt.BorderLayout.SOUTH);
    } catch (IOException e) {
      JTextArea errorTextArea = new JTextArea();
      errorTextArea.setFont(java.awt.Font.decode("monospaced"));
      jmol.consoleframe.getContentPane().add(new JScrollPane(errorTextArea),
          java.awt.BorderLayout.CENTER);
      errorTextArea.append(GT._("Could not create ConsoleTextArea: ") + e);
    }

    
    Point location = jmol.frame.getLocation();
    Dimension size = jmol.frame.getSize();
 
    // String name = CONSOLE_WINDOW_NAME;     

    //Dimension consoleSize = historyFile.getWindowSize(name);
    //Point consolePosition = historyFile.getWindowPosition(name);
    //if (consoleSize != null && consolePosition != null) {
    //  location = consolePosition;
    //  size = consoleSize;
    //} else {
      location.y += size.height;
      size.height = 200;
    //}
    if (size.height < 200 || size.height > 800)
      size.height = 200;
    if (size.width < 300 || size.width > 800)
      size.width = 300;
    if (location.y < 0 || location.y + size.height > screenSize.height)
      location.y = screenSize.height - size.height;
    if (location.x < 0 || location.x + size.width > screenSize.width)
      location.x = 0;
    jmol.consoleframe.setBounds(location.x, location.y, size.width, size.height);

    //Boolean consoleVisible = historyFile.getWindowVisibility(name);
    //if ((consoleVisible != null) && (consoleVisible.equals(Boolean.TRUE))) {
      //jmol.consoleframe.setVisible(true);
   // }

  }

  public static Jmol getJmol(JmolApp jmolApp, JFrame frame) {

    Splash splash = null;
    if (jmolApp.haveDisplay && jmolApp.splashEnabled) {
      ImageIcon splash_image = JmolResourceHandler.getIconX("splash");
      if (!jmolApp.isSilent)
        Logger.info("splash_image=" + splash_image);
      splash = new Splash(frame, splash_image);
      splash.setCursor(new Cursor(Cursor.WAIT_CURSOR));
      splash.showStatus(GT._("Creating main window..."));
      splash.showStatus(GT._("Initializing Swing..."));
    }
    if (jmolApp.haveDisplay)
      try {
        UIManager.setLookAndFeel(UIManager
            .getCrossPlatformLookAndFeelClassName());
      } catch (Exception exc) {
        System.err.println("Error loading L&F: " + exc);
      }

    if (splash != null)
      splash.showStatus(GT._("Initializing Jmol..."));

    Jmol window = new Jmol(jmolApp, splash, frame, null, jmolApp.startupWidth,
        jmolApp.startupHeight, jmolApp.info, null);
    if (jmolApp.haveDisplay)
      frame.setVisible(true);
    return window;
  }

  public void showStatus(String message) {
    splash.showStatus(message);    
  }

  private void report(String str) {
    if (jmolApp.isSilent)
      return;
    Logger.info(str);
  }

  private void say(String message) {
    if (jmolApp.haveDisplay)
      if (splash == null) {
        report(message);
      } else {
        splash.showStatus(message);
      }
  }

  /**
   * @return A list of Actions that is understood by the upper level
   * application
   */
  public Action[] getActions() {

    JmolList<Action> actions = new  JmolList<Action>();
    actions.addAll(Arrays.asList(defaultActions));
    actions.addAll(Arrays.asList(display.getActions()));
    actions.addAll(Arrays.asList(preferencesDialog.getActions()));
    return actions.toArray(new Action[0]);
  }

  /**
   * To shutdown when run as an application.  This is a
   * fairly lame implementation.   A more self-respecting
   * implementation would at least check to see if a save
   * was needed.
   */
  protected final class AppCloser extends WindowAdapter {

    @Override
    public void windowClosing(WindowEvent e) {
      JmolPanel.this.doClose();
    }
  }

  protected void doClose() {
    dispose(this.frame);
  }

  private void dispose(JFrame f) {
    // Save window positions and status in the history
    if (webExport != null)
      WebExport.cleanUp();
    if (historyFile != null) {
      if (frame != null) {
        jmolApp.border.x = frame.getWidth() - display.dimSize.width;
        jmolApp.border.y = frame.getHeight() - display.dimSize.height;
        historyFile.addWindowInfo("Jmol", frame, jmolApp.border);
      }
      //historyFile.addWindowInfo(CONSOLE_WINDOW_NAME, consoleframe);
      AppConsole console = (AppConsole) viewer.getProperty("DATA_API","getAppConsole", null);
      if (console != null && console.jcd != null)
        historyFile.addWindowInfo(SCRIPT_WINDOW_NAME, console.jcd, null);
      Component c = (Component) viewer.getProperty("DATA_API","getScriptEditor", null);
      if (c != null)
        historyFile.addWindowInfo(EDITOR_WINDOW_NAME, c, null);
      if (historyFile.getProperty("clearHistory", "false").equals("true"))
        historyFile.clear();
    }
    if (service != null) {
      service.close();
      service = null;
    }
    if (serverService != null) {
      serverService.close();
      serverService = null;
    }
    if (numWindows <= 1) {
      // Close Jmol
      report(GT._("Closing Jmol..."));
      // pluginManager.closePlugins();
      System.exit(0);
    } else {
      numWindows--;
      viewer.setModeMouse(JC.MOUSE_NONE);
      try {
        f.dispose();
      } catch (Exception e) {
        Logger.error("frame disposal exception");
        // ignore
      }
    }
  }

//  protected void setupNewFrame(JmolViewer viewer) {
//    String state = viewer.getStateInfo();
//    JFrame newFrame = new JFrame();
//    JFrame f = this.frame;
//    Jmol j = new Jmol(jmolApp, null, newFrame, (Jmol) this, startupWidth, startupHeight,
//        "", (state == null ? null : f.getLocationOnScreen()));
//    newFrame.setVisible(true);
//    j.viewer.menuStructure = viewer.menuStructure;
//    if (state != null) {
//      dispose(f);
//      j.viewer.evalStringQuiet(state);
//    }
//  }

  /**
   * This is the hook through which all menu items are
   * created.  It registers the result with the menuitem
   * hashtable so that it can be fetched with getMenuItem().
   * @param cmd
   * @return Menu item created
   * @see #getMenuItem
   */
  protected JMenuItem createMenuItem(String cmd) {

    JMenuItem mi;
    if (cmd.endsWith("Check")) {
      mi = guimap.newJCheckBoxMenuItem(cmd, false);
    } else {
      mi = guimap.newJMenuItem(cmd);
    }

    ImageIcon f = JmolResourceHandler.getIconX(cmd + "Image");
    if (f != null) {
      mi.setHorizontalTextPosition(SwingConstants.RIGHT);
      mi.setIcon(f);
    }

    if (cmd.endsWith("Script")) {
      mi.setActionCommand(JmolResourceHandler.getStringX(cmd));
      mi.addActionListener(executeScriptAction);
    } else {
      mi.setActionCommand(cmd);
      Action a = getAction(cmd);
      if (a != null) {
        mi.addActionListener(a);
        a.addPropertyChangeListener(new ActionChangedListener(mi));
        mi.setEnabled(a.isEnabled());
      } else {
        mi.setEnabled(false);
      }
    }
    menuItems.put(cmd, mi);
    return mi;
  }

  /**
   * Fetch the menu item that was created for the given
   * command.
   * @param cmd  Name of the action.
   * @return item created for the given command or null
   *  if one wasn't created.
   */
  protected JMenuItem getMenuItem(String cmd) {
    return menuItems.get(cmd);
  }

  /**
   * Fetch the action that was created for the given
   * command.
   * @param cmd  Name of the action.
   * @return The action
   */
  protected Action getAction(String cmd) {
    return commands.get(cmd);
  }

  /**
   * Create the toolbar.  By default this reads the
   * resource file for the definition of the toolbars.
   * @return The toolbar
   */
  private JToolBar createToolbar() {

    toolbar = new JToolBar();
    String[] tool1Keys = tokenize(JmolResourceHandler.getStringX("toolbar"));
    for (int i = 0; i < tool1Keys.length; i++) {
      if (tool1Keys[i].equals("-")) {
        toolbar.addSeparator();
      } else {
        toolbar.add(createTool(tool1Keys[i]));
      }
    }

    //Action handler implementation would go here.
    toolbar.add(Box.createHorizontalGlue());

    return toolbar;
  }

  /**
   * Hook through which every toolbar item is created.
   * @param key
   * @return Toolbar item
   */
  protected Component createTool(String key) {
    return createToolbarButton(key);
  }

  /**
   * Create a button to go inside of the toolbar.  By default this
   * will load an image resource.  The image filename is relative to
   * the classpath (including the '.' directory if its a part of the
   * classpath), and may either be in a JAR file or a separate file.
   *
   * @param key The key in the resource file to serve as the basis
   *  of lookups.
   * @return Button
   */
  protected AbstractButton createToolbarButton(String key) {

    ImageIcon ii = JmolResourceHandler.getIconX(key + "Image");
    boolean isHoldButton = (key.startsWith("animatePrev") || key.startsWith("animateNext"));
    AbstractButton b = (isHoldButton ? new AnimButton(ii, JmolResourceHandler.getStringX(key)) : new JButton(ii));
    String isToggleString = JmolResourceHandler.getStringX(key + "Toggle");
    if (isToggleString != null) {
      boolean isToggle = Boolean.valueOf(isToggleString).booleanValue();
      if (isToggle) {
        b = new JToggleButton(ii);
        if (key.equals("rotateScript")) {
          display.buttonRotate = b;
        }
        if (key.equals("modelkitScript")) {
          display.buttonModelkit = b;
        }
        display.toolbarButtonGroup.add(b);
        String isSelectedString = JmolResourceHandler.getStringX(key
            + "ToggleSelected");
        if (isSelectedString != null) {
          boolean isSelected = Boolean.valueOf(isSelectedString).booleanValue();
          b.setSelected(isSelected);
        }
      }
    }
    b.setRequestFocusEnabled(false);
    b.setMargin(new Insets(1, 1, 1, 1));

    Action a = null;
    String actionCommand = null;
    if (isHoldButton) {
      
    } else if (key.endsWith("Script")) {
      actionCommand = JmolResourceHandler.getStringX(key);
      a = executeScriptAction;
    } else {
      actionCommand = key;
      a = getAction(key);
    }
    if (a != null) {
      b.setActionCommand(actionCommand);
      b.addActionListener(a);
      a.addPropertyChangeListener(new ActionChangedListener(b));
      b.setEnabled(a.isEnabled());
    } else {
      b.setEnabled(isHoldButton);
    }

    String tip = guimap.getLabel(key + "Tip");
    if (tip != null) {
      guimap.map.put(key + "Tip", b);
      b.setToolTipText(tip);
    }

    return b;
  }

  /**
   * Take the given string and chop it up into a series
   * of strings on whitespace boundries.  This is useful
   * for trying to get an array of strings out of the
   * resource file.
   * @param input String to chop
   * @return Strings chopped on whitespace boundaries
   */
  protected String[] tokenize(String input) {
    JmolList<String> v = new  JmolList<String>();
    StringTokenizer t = new StringTokenizer(input);
    while (t.hasMoreTokens())
      v.addLast(t.nextToken());
    return v.toArray(new String[v.size()]);
  }

  /**
   * Create the menubar for the app.  By default this pulls the
   * definition of the menu from the associated resource file.
   * @return Menubar
   */
  protected JMenuBar createMenubar() {
    JMenuBar mb = new JMenuBar();
    addNormalMenuBar(mb);
    // The Macros Menu
    addMacrosMenuBar(mb);
    // The Plugin Menu
    // if (pluginManager != null) {
    //     mb.add(pluginManager.getMenu());
    // }
    // The Help menu, right aligned
    //mb.add(Box.createHorizontalGlue());
    addHelpMenuBar(mb);
    return mb;
  }

  protected void addMacrosMenuBar(JMenuBar menuBar) {
    // ok, here needs to be added the funny stuff
    JMenu macroMenu = guimap.newJMenu("macros");
    File macroDir = new File(System.getProperty("user.home")
        + System.getProperty("file.separator") + ".jmol"
        + System.getProperty("file.separator") + "macros");
    report("User macros dir: " + macroDir);
    report("       exists: " + macroDir.exists());
    report("  isDirectory: " + macroDir.isDirectory());
    if (macroDir.exists() && macroDir.isDirectory()) {
      File[] macros = macroDir.listFiles();
      for (int i = 0; i < macros.length; i++) {
        // loop over these files and load them
        String macroName = macros[i].getName();
        if (macroName.endsWith(".macro")) {
          if (Logger.debugging) {
            Logger.debug("Possible macro found: " + macroName);
          }
          FileInputStream macro = null;
          try {
            macro = new FileInputStream(macros[i]);
            Properties macroProps = new Properties();
            macroProps.load(macro);
            String macroTitle = macroProps.getProperty("Title");
            String macroScript = macroProps.getProperty("Script");
            JMenuItem mi = new JMenuItem(macroTitle);
            mi.setActionCommand(macroScript);
            mi.addActionListener(executeScriptAction);
            macroMenu.add(mi);
          } catch (IOException exception) {
            System.err.println("Could not load macro file: ");
            System.err.println(exception);
          } finally {
            if (macro != null) {
              try {
                macro.close();
              } catch (IOException e) {
                // Nothing
              }
            }
          }
        }
      }
    }
    menuBar.add(macroMenu);
  }

  protected void addNormalMenuBar(JMenuBar menuBar) {
    String[] menuKeys = tokenize(JmolResourceHandler.getStringX("menubar"));
    for (int i = 0; i < menuKeys.length; i++) {
      if (menuKeys[i].equals("-")) {
        menuBar.add(Box.createHorizontalGlue());
      } else {
        JMenu m = createMenu(menuKeys[i]);
        if (m != null)
          menuBar.add(m);
      }
    }
  }

  protected void addHelpMenuBar(JMenuBar menuBar) {
    JMenu m = createMenu("help");
    if (m != null) {
      menuBar.add(m);
    }
  }

  /**
   * Create a menu for the app.  By default this pulls the
   * definition of the menu from the associated resource file.
   * @param key
   * @return Menu created
   */
  protected JMenu createMenu(String key) {

    // Get list of items from resource file:
    String[] itemKeys = tokenize(JmolResourceHandler.getStringX(key));
    // Get label associated with this menu:
    JMenu menu = guimap.newJMenu(key);
    ImageIcon f = JmolResourceHandler.getIconX(key + "Image");
    if (f != null) {
      menu.setHorizontalTextPosition(SwingConstants.RIGHT);
      menu.setIcon(f);
    }

    // Loop over the items in this menu:
    for (int i = 0; i < itemKeys.length; i++) {
      String item = itemKeys[i];
      if (item.equals("-")) {
        menu.addSeparator();
      } else if (item.endsWith("Menu")) {
        menu.add(createMenu(item));
      } else {
        JMenuItem mi = createMenuItem(item);
        menu.add(mi);
      }
    }
    menu.addMenuListener(new MenuListener() {
        public void menuSelected(MenuEvent e) {
          String menuKey = ((JmolAbstractButton) e.getSource()).getKey();
          if (menuKey.equals("display") || menuKey.equals("tools"))
            setMenuState();
        }
        public void menuDeselected(MenuEvent e) {
        }
        public void menuCanceled(MenuEvent e) {
        }
    });
    return menu;
  }

  void setMenuState() {
    guimap.setSelected("perspectiveCheck", viewer.getPerspectiveDepth());
    guimap.setSelected("hydrogensCheck", viewer.getShowHydrogens());
    guimap.setSelected("measurementsCheck", viewer.getShowMeasurements());
    guimap.setSelected("axesCheck", viewer.getShowAxes());
    guimap.setSelected("boundboxCheck", viewer.getShowBbcage());
    guimap.setEnabled("openJSpecViewScript", display.isRotateMode());
  }

  private static class ActionChangedListener implements PropertyChangeListener {

    AbstractButton button;

    ActionChangedListener(AbstractButton button) {
      super();
      this.button = button;
    }

    public void propertyChange(PropertyChangeEvent e) {

      String propertyName = e.getPropertyName();
      if (e.getPropertyName().equals(Action.NAME)) {
        String text = (String) e.getNewValue();
        if (button.getText() != null) {
          button.setText(text);
        }
      } else if (propertyName.equals("enabled")) {
        Boolean enabledState = (Boolean) e.getNewValue();
        button.setEnabled(enabledState.booleanValue());
      }
    }
  }

  /**
   * Actions defined by the Jmol class
   */
  private Action[] defaultActions = { new NewAction(), new NewwinAction(),
      new OpenAction(), new OpenUrlAction(), new OpenPdbAction(), new OpenMolAction(), printAction, exportAction,
      new CloseAction(), new ExitAction(), copyImageAction, copyScriptAction,
      pasteClipboardAction, new AboutAction(), new WhatsNewAction(),
      new UguideAction(), new ConsoleAction(),  
      new RecentFilesAction(), povrayAction, writeAction, toWebAction, 
      new ScriptWindowAction(), new ScriptEditorAction(),
      new AtomSetChooserAction(), viewMeasurementTableAction, 
      new GaussianAction(), new ResizeAction(), surfaceToolAction }
  ;

  class CloseAction extends AbstractAction {
    CloseAction() {
      super(closeAction);
    }

    public void actionPerformed(ActionEvent e) {
      JmolPanel.this.frame.setVisible(false);
      JmolPanel.this.doClose();
    }
  }

  class ConsoleAction extends AbstractAction {

    public ConsoleAction() {
      super("jconsole");
    }

    public void actionPerformed(ActionEvent e) {
      if (consoleframe != null)
        consoleframe.setVisible(true);
    }

  }

  class AboutAction extends AbstractAction {

    public AboutAction() {
      super(aboutAction);
    }

    public void actionPerformed(ActionEvent e) {
      AboutDialog ad = new AboutDialog(frame, viewer);
      ad.setVisible(true);
    }

  }

  class WhatsNewAction extends AbstractAction {

    public WhatsNewAction() {
      super(whatsnewAction);
    }

    public void actionPerformed(ActionEvent e) {
      WhatsNewDialog wnd = new WhatsNewDialog(frame);
      wnd.setVisible(true);
    }
  }

  class GaussianAction extends AbstractAction {
    public GaussianAction() {
      super(gaussianAction);
    }
    
    public void actionPerformed(ActionEvent e) {
      if (gaussianDialog == null)
        gaussianDialog = new GaussianDialog(frame, viewer);
      gaussianDialog.setVisible(true);
    }
  }
    
  class NewwinAction extends AbstractAction {

    NewwinAction() {
      super(newwinAction);
    }

    public void actionPerformed(ActionEvent e) {
      JFrame newFrame = new JFrame();
      new Jmol(jmolApp, null, newFrame, (Jmol) JmolPanel.this, startupWidth, startupHeight, null, null);
      newFrame.setVisible(true);
    }

  }

  class UguideAction extends AbstractAction {

    public UguideAction() {
      super(uguideAction);
    }

    public void actionPerformed(ActionEvent e) {
      (new HelpDialog(frame)).setVisible(true);
    }
  }

  class PasteClipboardAction extends AbstractAction {

    public PasteClipboardAction() {
      super(pasteClipboardActionProperty);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.loadInline(AwtImageCreator.getClipboardTextStatic(), false);
    }
  }

  /**
   * An Action to copy the current image into the clipboard. 
   */
  class CopyImageAction extends AbstractAction {

    public CopyImageAction() {
      super(copyImageActionProperty);
    }

    public void actionPerformed(ActionEvent e) {
      (new AwtImageCreator()).clipImage(viewer, null);
    }
  }

  class CopyScriptAction extends AbstractAction {

    public CopyScriptAction() {
      super(copyScriptActionProperty);
    }

    public void actionPerformed(ActionEvent e) {
      (new AwtImageCreator()).clipImage(viewer, (String) viewer.getProperty(
          "string", "stateInfo", null));
    }
  }

  class PrintAction extends AbstractAction {

    public PrintAction() {
      super(printActionProperty);
    }

    public void actionPerformed(ActionEvent e) {
      print();
    }

  }

  /**
   * added print command, so that it can be used by RasmolScriptHandler
   **/
  public void print() {

    PrinterJob job = PrinterJob.getPrinterJob();
    job.setPrintable(display);
    if (job.printDialog()) {
      try {
        job.print();
      } catch (PrinterException e) {
        Logger.errorEx("Error while printing", e);
      }
    }
  }

  class OpenAction extends NewAction {

    OpenAction() {
      super(openAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      openFile();
    }
  }

  class OpenUrlAction extends NewAction {

    String title;
    String prompt;

    OpenUrlAction() {
      super(openurlAction);
      title = GT._("Open URL");
      prompt = GT._("Enter URL of molecular model");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      String url = JOptionPane.showInputDialog(frame, prompt, title,
          JOptionPane.PLAIN_MESSAGE);
      if (url != null) {
        if (url.indexOf("://") < 0) {
          if (url.length() == 4 && url.indexOf(".") < 0)
            url = "=" + url;
          if (!url.startsWith("="))
            url = "http://" + url;
        }
        viewer.openFileAsync(url);
      }
    }
  }

  class OpenPdbAction extends NewAction {

    OpenPdbAction() {
      super(openpdbAction);
      script = "var x__id__ = _modelTitle; if (x__id__.length != 4) { x__id__ = '1crn'};x__id__ = prompt('" 
        + GT._("Enter a four-digit PDB model ID or \"=\" and a three-digit ligand ID") + "',x__id__);if (!x__id__) { quit }; load @{'=' + x__id__}";
    }
  }

  class OpenMolAction extends NewAction {

    OpenMolAction() {
      super(openmolAction);
      script = "var x__id__ = _smilesString; if (!x__id__) { x__id__ = 'tylenol'};x__id__ = prompt('" 
        + GT._("Enter the name or identifier (SMILES, InChI, CAS) of a compound. Preface with \":\" to load from PubChem; otherwise Jmol will use the NCI/NIH Resolver.")
        + "',x__id__);if (!x__id__) { quit }; load @{(x__id__[1]==':' ? x__id__ : '$' + x__id__)}";
    }

  }

  class NewAction extends AbstractAction {

    protected String script;
    
    NewAction() {
      super(newAction);
    }

    NewAction(String nm) {
      super(nm);
    }

    public void actionPerformed(ActionEvent e) {
      if (script == null)
        revalidate();
      else
        viewer.script(script);
    }
  }
  
  /**
   * Really lame implementation of an exit command
   */
  class ExitAction extends AbstractAction {

    ExitAction() {
      super(exitAction);
    }

    public void actionPerformed(ActionEvent e) {
      JmolPanel.this.doClose();
    }
  }

  final static String[] imageChoices = { "JPEG", "PNG", "GIF", "PPM", "PDF" };
  final static String[] imageExtensions = { "jpg", "png", "gif", "ppm", "pdf" };

  class ExportAction extends AbstractAction {

    ExportAction() {
      super(exportActionProperty);
    }

    public void actionPerformed(ActionEvent e) {

      Dialog sd = new Dialog();
      String fileName = sd.getImageFileNameFromDialog(viewer, null, imageType,
          imageChoices, imageExtensions, qualityJPG, qualityPNG);
      if (fileName == null)
        return;
      qualityJPG = sd.getQuality("JPG");
      qualityPNG = sd.getQuality("PNG");
      String sType = imageType = sd.getType();
      if (sType == null) {
        // file type changer was not touched
        sType = fileName;
        int i = sType.lastIndexOf(".");
        if (i < 0)
          return; // make no assumptions - require a type by extension
        sType = sType.substring(i + 1).toUpperCase();
      }
      if (fileName.indexOf(".") < 0)
        fileName += "." + (sType.equalsIgnoreCase("JPEG") ? "jpg" : sType.toLowerCase());
      Logger.info((String) viewer.createImage(fileName, sType, null, sd.getQuality(sType), 0, 0));
    }

  }

  class RecentFilesAction extends AbstractAction {

    public RecentFilesAction() {
      super(recentFilesAction);
    }

    public void actionPerformed(ActionEvent e) {

      recentFiles.setVisible(true);
      String selection = recentFiles.getFile();
      if (selection == null || selection.length() == 0)
        return;
      if (selection.endsWith(" (*)"))
        viewer.openFileAsyncPDB(selection.substring(0, selection.length() - 4), true);
      else
        viewer.openFileAsyncPDB(selection, false);
    }
  }

  class ScriptWindowAction extends AbstractAction {

    public ScriptWindowAction() {
      super(consoleAction);
    }

    public void actionPerformed(ActionEvent e) {
      AppConsole console = (AppConsole) viewer.getProperty("DATA_API","getAppConsole", null);
      if (console != null)
        console.setVisible(true);
    }
  }

  class ScriptEditorAction extends AbstractAction {

    public ScriptEditorAction() {
      super(editorAction);
    }

    public void actionPerformed(ActionEvent e) {
      Component c = (Component) viewer.getProperty("DATA_API","getScriptEditor", null);
      if (c != null)
        c.setVisible(true);
    }
  }

  class AtomSetChooserAction extends AbstractAction {
    public AtomSetChooserAction() {
      super(atomsetchooserAction);
    }

    public void actionPerformed(ActionEvent e) {
      atomSetChooser.setVisible(true);
    }
  }

  class PovrayAction extends AbstractAction {

    public PovrayAction() {
      super(povrayActionProperty);
    }

    public void actionPerformed(ActionEvent e) {
      new PovrayDialog(frame, viewer);
    }

  }

  class WriteAction extends AbstractAction {

    public WriteAction() {
      super(writeActionProperty);
    }

    public void actionPerformed(ActionEvent e) {
      String fileName = (new Dialog()).getSaveFileNameFromDialog(viewer,
          null, "SPT");
      if (fileName != null)
        Logger.info((String) viewer.createImage(fileName, "SPT", viewer.getStateInfo(),
            Integer.MIN_VALUE, 0, 0));
    }
  }

  /**
   * 
   * Starting with Jmol 11.8.RC5, this is just informational
   * if type == null and null is returned, then it means "Jmol, you handle it"
   * 
   * @param fileName
   * @param type
   * @param text_or_bytes
   * @param quality
   * @return          null (you do it) or a message starting with OK or an error message
   */
  String createImageStatus(String fileName, String type, Object text_or_bytes,
                           int quality) {
    if (fileName != null && text_or_bytes != null)
      return null; // "Jmol, you do it."
    String msg = fileName;
    if (msg != null && !msg.startsWith("OK") && status != null) {
      status.setStatus(1, GT._("IO Exception:"));
      status.setStatus(2, msg);
    }
    return msg;
  }

  WebExport webExport;
  void createWebExport() {
    webExport = WebExport.createAndShowGUI(viewer, historyFile, WEB_MAKER_WINDOW_NAME);
  }


  class ToWebAction extends AbstractAction {

    public ToWebAction() {
      super(toWebActionProperty);
    }

    public void actionPerformed(ActionEvent e) {
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          createWebExport();
        }
      });
    }
  }

  class ViewMeasurementTableAction extends AbstractAction {

    public ViewMeasurementTableAction() {
      super("viewMeasurementTable");
    }

    public void actionPerformed(ActionEvent e) {
      display.measurementTable.activate();
    }
  }
  
  void createSurfaceTool(){
    //TODO check to see if it already exists, if so bring to front.
    if(surfaceTool!=null){
      surfaceTool.toFront();
    }else{
    surfaceTool = new SurfaceTool(viewer, historyFile, SURFACETOOL_WINDOW_NAME, true);
    }
  }
  
  class SurfaceToolAction extends AbstractAction {
    
    public SurfaceToolAction(){
      super(surfaceToolActionProperty);
    }
    
    public void actionPerformed(ActionEvent e){
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          createSurfaceTool();
        }
      });
    }
  }
  

  
  /**
   * Returns a new File referenced by the property 'user.dir', or null
   * if the property is not defined.
   *
   * @return  a File to the user directory
   */
  public static File getUserDirectory() {
    String dir = System.getProperty("user.dir");
    return dir == null ? null : new File(System.getProperty("user.dir"));
  }

  void openFile() {
    String fileName = (new Dialog()).getOpenFileNameFromDialog(viewerOptions,
        viewer, null, historyFile, FILE_OPEN_WINDOW_NAME, true);
    if (fileName == null)
      return;
    boolean pdbCartoons = !fileName.startsWith("#NOC#;");
    if (!pdbCartoons)
      fileName = fileName.substring(6);
    if (fileName.startsWith("load append"))
      viewer.scriptWait(fileName);
    else
      viewer.openFileAsyncPDB(fileName, pdbCartoons);
  }

  static final String chemFileProperty = "chemFile";

  void notifyFileOpen(String fullPathName, String title) {
    if (fullPathName == null || !fullPathName.equals("file[]")) {
      recentFiles.notifyFileOpen(fullPathName);
      frame.setTitle(title);
    }
    if (atomSetChooser == null) {
      atomSetChooser = new AtomSetChooser(viewer, frame);
      pcs.addPropertyChangeListener(chemFileProperty, atomSetChooser);
    }
    pcs.firePropertyChange(chemFileProperty, null, null);
  }

  class ExecuteScriptAction extends AbstractAction {
    public ExecuteScriptAction() {
      super("executeScriptAction");
    }

    public void actionPerformed(ActionEvent e) {
      String script = e.getActionCommand();
      if (script.indexOf("#showMeasurementTable") >= 0)
        display.measurementTable.activate();
      //      viewer.script("set picking measure distance;set pickingstyle measure");
      viewer.evalStringQuiet(script);
    }
  }

  class ResizeAction extends AbstractAction {
    public ResizeAction() {
      super(resizeAction);
    }
    public void actionPerformed(ActionEvent e) {
      resizeInnerPanel(null);
    } 
  }
  
  void resizeInnerPanel(String data) {
    String info = viewer.getScreenWidth() + " " + viewer.getScreenHeight();
    if (data == null) {
      data = info;
   } else {
     int pt = data.indexOf("preferredWidthHeight ");
     int pt2 = data.indexOf(";", pt + 1);
     if (pt >= 0 && pt2 > pt)
       data = data.substring(pt + 21, pt2).trim();
     if (data.equals(info))
       return;
   }
    info = JOptionPane.showInputDialog(GT._("width height?"), data);
    if (info == null)
      return;
    float[] dims = new float[2];
    int n = Parser.parseStringInfestedFloatArray(info, null, dims);
    if (n < 2)
      return;
    resizeDisplay((int) dims[0], (int) dims[1]);
  }

  void resizeDisplay(int width, int height) {
    Dimension d = new Dimension(width, height);
    display.setPreferredSize(d);
    d = new Dimension(width, 30);
    status.setPreferredSize(d);
    toolbar.setPreferredSize(d);
    Platform.getWindow(this).pack();
  }

  void updateLabels() {
    if (atomSetChooser != null) {
      atomSetChooser.dispose();
      atomSetChooser = null;
    }
    if (gaussianDialog != null) {
      gaussianDialog.dispose();
      gaussianDialog = null;
    }
    boolean doTranslate = GT.setDoTranslate(true);
    getDialogs();
    GT.setDoTranslate(doTranslate);
    guimap.updateLabels();
  }

  ////////// JSON/NIO SERVICE //////////
  
  public void nioRunContent(JsonNioServer jns) {
    // ignore
  }
  
  public void nioClosed(JsonNioServer jns) {
    if (bannerFrame != null) {
      viewer.scriptWait("delay 2");
      bannerFrame.dispose();
      viewer.setModeMouse(JC.MOUSE_NONE);
      // would not nec. have to close this....
      System.exit(0);
    }
    if (jns.equals(service))
      service = null;
    else if (jns.equals(serverService))
      serverService = null;
    
  }

  public void setBannerLabel(String label) {
    if (bannerFrame != null)
      bannerFrame.setLabel(label);
  }

  void sendNioMessage(int port, String strInfo) {
    try {
      if (port < 0) {
        if (serverService != null && strInfo != null && strInfo.equalsIgnoreCase("STOP")) {
          serverService.close();
        } else {
          serverService = getJsonNioServer();
          if (serverService != null)
            serverService.startService(port, this, viewer, "-1", 1);
        }
        if (serverService != null && serverService.getPort() == -port && strInfo != null) {
          if (service == null) {
            service = getJsonNioServer();
            if (service != null)
              service.startService(-port, this, viewer, null, 1);
          }
          if (service != null)
            service.send(-port, strInfo);
          return;
        }
        return;
      }
      if (strInfo == null)
        return;
      if (strInfo.equalsIgnoreCase("STOP"))
        strInfo = "{\"type\":\"quit\"}";
      if (service == null && serverService != null && serverService.getPort() == port) {
        serverService.send(port, strInfo);
        return;
      }
      if (service == null) {
        service = getJsonNioServer();
        if (service != null)
          service.startService(port, this, viewer, null, 1);
      }
      if (service != null)
        service.send(port, strInfo);
    } catch (IOException e) {
      // TODO
    }
  }

  public static JsonNioServer getJsonNioServer() {
    return (JsonNioServer) Interface
        .getApplicationInterface("jsonkiosk.JsonNioService");
  }

  private class AnimButton extends JButton implements MouseListener {

    private String script;

    protected AnimButton(ImageIcon ii, String script) {
      super(ii);
      this.script = script;
      addMouseListener(this);
    }

    public void mousePressed(MouseEvent e) {
      viewer.evalStringQuiet(script);
      viewer.evalStringQuiet("timeout '__animBtn' -100 \"" + script + "\"");
    }

    public void mouseReleased(MouseEvent e) {
      viewer.evalStringQuiet("timeout '__animBtn' OFF");
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

  }

  public void syncScript(String script) {
    viewer.syncScript(script, "~", 0);
  }

}
