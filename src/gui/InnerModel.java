/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gui;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Locale;
import javax.media.j3d.VirtualUniverse;
import java.awt.*;
import javax.swing.*;
import javax.media.j3d.BranchGroup;
import javax.vecmath.*;

import com.sun.j3d.utils.behaviors.mouse.*;
import com.sun.j3d.utils.universe.ViewingPlatform;

class InnerModel
  {
    VirtualUniverse universe;
    Locale          locale;
    BranchGroup     scene;

    public InnerModel ()
    {
      universe = new VirtualUniverse ();
      locale   = new Locale (universe);
      
    }

    public void die(ViewingPlatform vp){
        universe.removeAllLocales();
    }
    
    public void setScene (BranchGroup scene)
    {
      if (this.scene != null)
        locale.removeBranchGraph (this.scene);

      this.scene = scene;
      locale.addBranchGraph (this.scene);
    }

    public void addViewingPlatform (ViewingPlatform viewingPlatform)
    {
      locale.addBranchGraph (viewingPlatform);
    }
  }
