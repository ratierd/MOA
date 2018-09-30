/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.gui.visualization;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.cluster.FadingClusterWithBitVector;
import moa.core.AutoExpandVector;

/**
 *
 * @author David
 */
public class BitSetPanel extends javax.swing.JPanel {

    /**
     * Creates new form BitSetPanel
     */
    public BitSetPanel() {
        initComponents();
    }
    
    public void setTable(Clustering clustering) {
        try {
            AutoExpandVector<FadingClusterWithBitVector> entries = new AutoExpandVector<>();
            for (Cluster c : clustering.getClustering()) {
                entries.add((FadingClusterWithBitVector)c);
            }
            setTable(entries);
        } catch (Exception ex) {  }
    }
    
    public void setTable(AutoExpandVector<FadingClusterWithBitVector> entries) {
        List<String> columns = new ArrayList<String>(0);
        List<String[]> values = new ArrayList<String[]>(0);
        
        for (FadingClusterWithBitVector fc : entries) {
            columns.add("C" + Math.round(fc.getId()));
        }
        
        for (int i = 0; i < entries.get(0).getDimension(); i++) {
            String[] value = new String[columns.size()];
            for (int j = 0; j < columns.size(); j++) {
                if (entries.get(j).getBitVector().getAll()[i]) {
                    value[j] = "1";
                }
                else {
                    value[j] = "0";
                }
            }
            values.add(value);
        }
        
        /*for (String[] s : values) {
            System.out.print("[");
            for (String s2 : s) {
                System.out.print(s2 + ",");
            }
            System.out.println("]");
        }*/

        TableModel tableModel = new DefaultTableModel(values.toArray(new Object[][] {}), columns.toArray());
        jTable1.setModel(tableModel);
    }
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 418, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 209, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
