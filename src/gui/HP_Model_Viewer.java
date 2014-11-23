/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gui;

import javax.media.j3d.Alpha;
import javax.media.j3d.RotationInterpolator;
import javax.media.j3d.BranchGroup;
import javax.vecmath.*;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Appearance;
import javax.media.j3d.Material;
import javax.media.j3d.Transform3D;

import javax.media.j3d.TransformGroup;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.universe.ViewingPlatform;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.media.j3d.Shape3D;
import javax.vecmath.Vector3f;
import javax.swing.JPanel;
import javax.swing.Timer;


public class HP_Model_Viewer extends JPanel implements ActionListener{
  private cords protien;
  private cords new_protien;
  private BranchGroup  group;
  private BranchGroup  root;
  private InnerModel model;
  private InnerView  camera;
  private TransformGroup objTrans;
  private float originalx,originaly,originalz;
  private String directions;
  private float divisions;

  
HP_Model_Viewer(){
    model = new InnerModel ();
    group = new BranchGroup ();
    root  = new BranchGroup ();
    
    originalx=0;
    originaly=0;
    originalz=0;
    //file_loaded = false;
    timer = new Timer(20, this);
    divisions = 40;
}


private float steps;
private Timer timer;

public void actionPerformed(ActionEvent e) {
      steps++;
      if (steps==1){
          new_protien= new cords(directions);
          new_protien.setCords(directions);      
      }
      protien.restart();
      new_protien.restart();
      float oldx = originalx,oldy=originaly,oldz=originalz;  
      float ox,oy,oz;
   
      for(int i =0; i<group.numChildren()-1 ; i++){
            TransformGroup obj =(TransformGroup)group.getChild(i);
            if(i%2==0){
              Transform3D transform = new Transform3D(); 
              obj.getTransform(transform);
              Vector3f vector = new Vector3f();
              transform.get(vector);
              Vector3f vector2 = new Vector3f( 
                        //new point                 new center           old center
                    vector.getX()+(((((new_protien.getX()/3f) - (new_protien.getAvgX()/3f)) + oldx) - vector.getX())*(1f/(divisions-steps)) ),
                    vector.getY()+(((((new_protien.getY()/3f) - (new_protien.getAvgY()/3f)) + oldy) - vector.getY())*(1f/(divisions-steps)) ),
                    vector.getZ()+(((((new_protien.getZ()/3f) - (new_protien.getAvgZ()/3f)) + oldz) - vector.getZ())*(1f/(divisions-steps)) )
                    );          
            transform.setTranslation(vector2);
            obj.setTransform(transform);    
        }
        else{
            ox=0;      oy=0;        oz=0;
            Transform3D transform = new Transform3D();
            Matrix3f rotation = new Matrix3f();
            Vector3f vector = new Vector3f();          
            obj.getTransform(transform);
            transform.get(rotation, vector);                       
            //handle offsets
            if(new_protien.getX()!=new_protien.seeNextX()){    
                transform.rotZ(Math.PI/2);
                if (new_protien.getX()> new_protien.seeNextX()){ ox = -.2f;}
                else{ ox= 0.2f;}
            }
            if(new_protien.getY()!= new_protien.seeNextY()){
                transform.rotY(Math.PI/2);
                if (new_protien.getY()> new_protien.seeNextY()){oy = -.2f;}
                else{oy= 0.2f;}
            }
            if(new_protien.getZ()!=new_protien.seeNextZ()){
                transform.rotX(Math.PI/2);
                if (new_protien.getZ()> new_protien.seeNextZ()){ oz = -.2f;}
                else{ oz= 0.2f;}
            }
            
            if (steps<(divisions-5)){
                Vector3f vector2 = new Vector3f(1000,1000,1000);
                transform.setTranslation(vector2);
            }
            else{
            Vector3f vector2 = new Vector3f( 
                        //new point                 new center           old center
                    vector.getX()+((ox+(((new_protien.getX()/3f) - (new_protien.getAvgX()/3f)) + oldx) - vector.getX())*(1f/(divisions-steps)) ),
                    vector.getY()+((oy+(((new_protien.getY()/3f) - (new_protien.getAvgY()/3f)) + oldy) - vector.getY())*(1f/(divisions-steps)) ),
                    vector.getZ()+((oz+(((new_protien.getZ()/3f) - (new_protien.getAvgZ()/3f)) + oldz) - vector.getZ())*(1f/(divisions-steps)) )
                    );
                    transform.setTranslation(vector2);
            }            
            obj.setTransform(transform); 
            protien.getNext();
            new_protien.getNext();
         }
    }   
      if(steps >divisions-2){
          timer.stop();
          protien=new_protien;   
      }   
  }


public void morph(String directions){
    //RUN A LENGTH CHECK!!!
    this.directions = directions;
    steps =0;
    timer.start();
}

public void build(JPanel panel, String directions, String charges){

    //creat cordinated for protein
    protien= new cords(directions);
    protien.setCords(directions);
    originalx =protien.getAvgX()/3;
    originaly =protien.getAvgY()/3;
    originalz =protien.getAvgZ()/3;
    camera = new InnerView (protien.getAvgX(),protien.getAvgY(),protien.getAvgZ());
    //setup camera around protein
    setCameraView(camera.getViewingPlatform());
    //build 3d model of protein
    BranchGroup contentBranch = createSceneGraph (charges);
    //place model in sceen
    camera.setModel (model);
    model.setScene (contentBranch);
    //panel.add ("Center", view1.getCanvas3D ());
    panel.add(camera.getCanvas3D ()); 
    //pack();
  }

public void die(){
    //System.out.println("in here1");
    model.die(camera.getViewingPlatform());
    //System.out.println("in here2");
    //group.detach();
    //System.out.println("in here3");
    //root.detach();
    //System.out.println("in here4");
    //for(int i =0; i<group.numChildren()-1; i++){
        //System.out.println("in here5");
        //TransformGroup obj =(TransformGroup)group.getChild(i);
        //if(i%2==0 && file_loaded==true){
        //    Shape3D box = (Shape3D)obj.getChild(0);
        //    box.removeAllGeometries();
        //}
        //else{
            //Primitive prim = (Primitive)obj.getChild(0);
            //prim.clearGeometryCache();
            //prim.removeAllChildren();
        //}
        //obj.removeAllChildren();
    //}
    //objTrans.removeAllChildren();
    //group.removeAllChildren();
    //root.removeAllChildren();
}

private BranchGroup createSceneGraph (String charges)  {
    objTrans = new TransformGroup ();
    objTrans.setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
    root.addChild (objTrans);

    buildModel(protien, charges);
    objTrans.addChild(group);
    
    Transform3D Axis    = new Transform3D ();
    Vector3f vCenter = new Vector3f(protien.getAvgX()/3,protien.getAvgY()/3,protien.getAvgZ()/3);
    Axis.setTranslation(vCenter);

    Alpha rotAlpha = new Alpha (-1, 80000);
    RotationInterpolator rotator = new RotationInterpolator (rotAlpha, objTrans, Axis, 0.0f, (float) Math.PI * 2.0f);

    BoundingSphere bounds = new BoundingSphere (new Point3d (0, 0, 0), 100);
    rotator.setSchedulingBounds (bounds);
    group.addChild (rotator);
    addLighting(root);
    root.compile ();
    return root;
  }

private void buildModel(cords protein, String charges){
    //loading a 3d object

    float ox,oy,oz;
    int a=0;
    while(protein.hasNext()){
        ox=0;
        oy=0;
        oz=0;
        
        TransformGroup obj = new TransformGroup ();
        obj.setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
        Appearance app = new Appearance();
        Material mat = new Material();
        if(charges.charAt(a)=='h'){
            mat.setDiffuseColor(new Color3f(1, 0, 0));
            mat.setEmissiveColor(0.6f, 0.0f, 0.0f);
        }
        else{
            mat.setDiffuseColor(new Color3f(0, 0, 1));
            mat.setEmissiveColor(0.0f, 0.0f, 0.6f);
        }
        app.setMaterial(mat);

            Sphere box = new Sphere(0.08f,1,20, app);
            Transform3D transform = new Transform3D();
            Vector3f vector = new Vector3f(protein.getX()/3,protein.getY()/3,protein.getZ()/3);
            transform.setTranslation(vector);
            obj.setTransform(transform);
            obj.addChild(box);
            group.addChild(obj);
  
        if (protein.haslookahead()){
            TransformGroup obj2 = new TransformGroup ();
            obj2.setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
            Appearance app2 = new Appearance();
            Material mat2 = new Material();
            mat2.setDiffuseColor(new Color3f(1, 1, 1));
            mat2.setEmissiveColor(0.4f, 0.4f, 0.4f);
            app2.setMaterial(mat2);
            Cylinder link = new Cylinder(0.015f,0.3f,app2);
            Transform3D transform2 = new Transform3D();

            if(protein.getX()!=protein.seeNextX()){
                transform2.rotZ(Math.PI/2);
                if (protein.getX()> protein.seeNextX()){ ox = -.2f;}
                else{ ox= 0.2f;}
            }
            if(protein.getY()!=protein.seeNextY()){
                if (protein.getY()> protein.seeNextY()){oy = -.2f;}
                else{oy= 0.2f;}
            }
            if(protein.getZ()!=protein.seeNextZ()){
                transform2.rotX(Math.PI/2);
                if (protein.getZ()> protein.seeNextZ()){ oz = -.2f;}
                else{ oz= 0.2f;}
            }
            Vector3f vector2 = new Vector3f((protein.getX()/3)+ox,(protein.getY()/3)+oy,(protein.getZ()/3)+oz);
            transform2.setTranslation(vector2);
            obj2.setTransform(transform2);
            obj2.addChild(link);
            group.addChild(obj2);
        }
        protein.getNext();
        a++;
    }
  }

protected void addLighting (BranchGroup root)  {
    BoundingSphere bounds = new BoundingSphere (new Point3d (0, 0, 0), 100);
    Color3f light1Color = new Color3f(1.0f, 1.0f, 1.0f);
    Vector3f light1Direction = new Vector3f(3,-3,-protien.getAvgZ());
    DirectionalLight light1 = new DirectionalLight(light1Color, light1Direction);
    light1.setInfluencingBounds(bounds);
    root.addChild(light1);

  }
        
private void setCameraView(ViewingPlatform camera){  
    Transform3D t3d = new Transform3D ();
    camera.getViewPlatformTransform().getTransform (t3d);
    t3d.setTranslation(new Vector3f (protien.getAvgX()/3f, protien.getAvgY()/3f, 4f) );
    camera.getViewPlatformTransform ().setTransform (t3d);
}    
}    

  