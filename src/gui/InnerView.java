/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


package gui;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Canvas3D;
import javax.vecmath.Point3d;
import java.awt.*;
import javax.swing.*;
import javax.media.j3d.Canvas3D;
import com.sun.j3d.utils.universe.SimpleUniverse;
import javax.vecmath.*;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.GraphicsConfigTemplate3D;
import com.sun.j3d.utils.behaviors.mouse.*;
import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.Viewer;
import com.sun.j3d.utils.universe.ViewingPlatform;
import javax.media.j3d.Transform3D;


class InnerView
  {
    Canvas3D        canvas;
    Viewer          viewer;
    ViewingPlatform viewingPlatform;
    InnerModel      model;

    public InnerView (float x, float y, float z)
    {
      //  GraphicsConfigTemplate3D template = new GraphicsConfigTemplate3D();
      //  template.setSceneAntialiasing(GraphicsConfigTemplate3D.PREFERRED);
      //  GraphicsConfiguration config =
      //      GraphicsEnvironment.getLocalGraphicsEnvironment().
      //      getDefaultScreenDevice().getBestConfiguration(template);
      canvas          = new Canvas3D (SimpleUniverse.getPreferredConfiguration ());
      viewer          = new Viewer (canvas);
      //viewer.getView().setSceneAntialiasingEnable(true);
      viewingPlatform = new ViewingPlatform ();

      addOrbitBehaviour (x, y, z);
      viewer.setViewingPlatform (viewingPlatform);
      viewingPlatform.setNominalViewingTransform ();
    }

    public Canvas3D getCanvas3D ()
    {
      return canvas;
    }

    public ViewingPlatform getViewingPlatform ()
    {
      return viewingPlatform;
    }

    public void setModel (InnerModel model)
    {
      this.model = model;
      model.addViewingPlatform (viewingPlatform);
    }

    
    private void addOrbitBehaviour (float x, float y, float z)
    {
      BoundingSphere bounds = new BoundingSphere (new Point3d (0.0, 0.0, 0.0), 1000.0);
      OrbitBehavior  orbit  = new OrbitBehavior (canvas, OrbitBehavior.REVERSE_ALL);
      orbit.setSchedulingBounds (bounds);
      
      //set the center of spin
      Point3d[] center = new Point3d[1];
      center[0] = new Point3d (x/3, y/3, z/3);
      orbit.RotationCenter(center);
      viewingPlatform.setViewPlatformBehavior (orbit);
    }
  }


