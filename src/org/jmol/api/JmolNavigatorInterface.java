package org.jmol.api;



import org.jmol.util.JmolList;
import org.jmol.util.P3;
import org.jmol.util.V3;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;

public interface JmolNavigatorInterface extends Runnable {

  void set(TransformManager transformManager, Viewer viewer);

  void navigateTo(float floatSecondsTotal, V3 axis, float degrees,
                  P3 center, float depthPercent, float xTrans, float yTrans);

  void navigate(float seconds, P3[][] pathGuide, P3[] path,
                float[] theta, int indexStart, int indexEnd);

  void zoomByFactor(float factor, int x, int y);

  void calcNavigationPoint();

  void setNavigationOffsetRelative();//boolean navigatingSurface);

  void navigateKey(int keyCode, int modifiers);

  void navigateList(JmolScriptEvaluator eval, JmolList<Object[]> list);

  void navigateAxis(V3 rotAxis, float degrees);

  void setNavigationDepthPercent(float percent);

  String getNavigationState();

  void navTranslatePercent(float seconds, float x, float y);

  void interrupt();


}
