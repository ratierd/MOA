/*
 *    ClusterPanel.java
 *    Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Jansen (moa@cs.rwth-aachen.de)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    
 */

package moa.gui.visualization;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;
import moa.cluster.Cluster;
import moa.cluster.EllipsoidCluster;
import moa.cluster.SphereCluster;

public class ClusterPanel extends JPanel {
    private Cluster cluster;

    private double[] center;
    private final static int MIN_SIZE = 5;
    protected double decay_rate;

    protected int x_dim = 0;
    protected int y_dim = 1;
    protected Color col;
    protected Color default_color = Color.BLACK;
    protected double[] direction = null;

    protected StreamPanel streamPanel;

    protected double width;
    protected double height;
    protected int panel_width;
    protected int panel_height;
    protected int window_size;
    protected boolean highligted = false;



    /** Creates new form ObjectPanel */

    public ClusterPanel(Cluster cluster, Color color, StreamPanel sp) {
        this.cluster = cluster;
        center = cluster.getCenter();
        if (cluster instanceof SphereCluster) {
            double radius = ((SphereCluster)cluster).getRadius();
            width = radius * 2;
            height = radius * 2;
        }
        else if (cluster instanceof EllipsoidCluster){
            width = ((EllipsoidCluster)cluster).getRadius()[x_dim] * 2;
            height = ((EllipsoidCluster)cluster).getRadius()[y_dim] * 2;
        }
        streamPanel = sp;

        default_color = col = color;

        setVisible(true);
        setOpaque(false);
        setSize(new Dimension(1,1));
        setLocation(0,0);

        initComponents();
    }

    public void setDirection(double[] direction){
        this.direction = direction;
    }

    public void updateLocation(){
        x_dim = streamPanel.getActiveXDim();
        y_dim = streamPanel.getActiveYDim();

        if(cluster!=null && center==null)
            getParent().remove(this);
        else{
            //size of the parent
            window_size = Math.min(streamPanel.getWidth(),streamPanel.getHeight());

            //scale down to diameter
            panel_width = (int) (width * window_size);
            panel_height = (int) (height * window_size);
            if(panel_width < MIN_SIZE)
                panel_width = MIN_SIZE;
            if(panel_height < MIN_SIZE)
                panel_height = MIN_SIZE;

            setSize(new Dimension(panel_width+1,panel_height+1));
            setLocation((int)(center[x_dim]*window_size-(panel_width/2)),(int)(center[y_dim]*window_size-(panel_height/2)));
            
        }
    }

    public void updateTooltip(){
        setToolTipText(cluster.getInfo());
    }

    @Override
    public boolean contains(int x, int y) {
        //only react on the hull of the cluster
        return (Math.pow(x - center[x_dim],2)/(Math.pow(width/2, 2))) + (Math.pow(y - center[y_dim],2)/(Math.pow(height/2, 2))) <= 1;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                formMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 296, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 266, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void formMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseClicked
        streamPanel.setHighlightedClusterPanel(this);
    }//GEN-LAST:event_formMouseClicked

    @Override
    protected void paintComponent(Graphics g) {
        updateLocation();
        if(highligted){
            g.setColor(Color.BLUE);
        }
        else{
            g.setColor(default_color);
        }
        int cw = (int)(panel_width/2);
        int ch = (int)(panel_height/2);

        if(cluster.getId()>=0)
            g.drawString("C"+(int)cluster.getId(),cw,ch);
        
        g.drawOval(0, 0, panel_width, panel_height);

        if(direction!=null){
            double length = Math.sqrt(Math.pow(direction[0], 2) + Math.pow(direction[1], 2));
            g.drawLine(cw, ch, cw+(int)((direction[0]/length)*panel_width), ch+(int)((direction[1]/length)*panel_height));
        }

        updateTooltip();
        
    }

    public void highlight(boolean enabled){
        highligted = enabled;
        repaint();
    }

    public boolean isValidCluster(){
        return (center!=null);
    }

    public int getClusterID(){
        return (int)cluster.getId();
    }

    public int getClusterLabel(){
        return (int)cluster.getGroundTruth();
    }


    public String getSVGString(int width){
        StringBuffer out = new StringBuffer();

        int x = (int)(center[x_dim]*window_size);
        int y = (int)(center[y_dim]*window_size);
        
        out.append("<ellipse ");
        out.append("cx='"+x+"' cy='"+y+"' rx='"+(int)panel_width/2+"'"+"' ry='"+(int)panel_height/2+"'");
        out.append(" stroke='green' stroke-width='1' fill='white' fill-opacity='0' />");
        out.append("\n");
        return out.toString();
    }

    public void drawOnCanvas(Graphics2D imageGraphics){
        int x = (int)(center[x_dim]*window_size-(panel_width/2));
        int y = (int)(center[y_dim]*window_size-(panel_height/2));
        imageGraphics.drawOval(x, y, panel_width, panel_height);
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables


}