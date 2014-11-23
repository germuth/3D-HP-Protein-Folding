package org.jmol.api;

import org.jmol.script.ScriptContext;
import org.jmol.viewer.Viewer;

public interface JmolParallelProcessor {

  Object getExecutor();

  void runAllProcesses(Viewer viewer);

  void addProcess(String name, ScriptContext context);

  void set(String name, int tok);

}
