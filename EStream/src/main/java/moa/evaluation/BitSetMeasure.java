/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.evaluation;

import java.util.ArrayList;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.cluster.FadingClusterWithBitVector;
import moa.core.AutoExpandVector;
import moa.gui.visualization.DataPoint;

/**
 *
 * @author David
 */
public class BitSetMeasure extends MeasureCollection {

    @Override
    protected String[] getNames() {
        String[] names = {"BitSetVisualization"};
        return names;
    }

    @Override
    protected void evaluateClustering(Clustering clustering, Clustering trueClustering, ArrayList<DataPoint> points) throws Exception {
        addValue("BitSetVisualization", 0.5);
    }
    
}
