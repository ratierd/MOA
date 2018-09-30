/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.evaluation;

import java.util.ArrayList;
import moa.cluster.Clustering;
import moa.gui.visualization.DataPoint;

/**
 *
 * @author David
 */
public class CustomFMeasure extends MeasureCollection{
    
    @Override
    protected String[] getNames() {
        String[] names = {"CustomFMeasure"};
        return names;
    }
    
    @Override
    public void evaluateClustering(Clustering clustering, Clustering trueClustering, ArrayList<DataPoint> points) {

        if (clustering.size()<0){
            addValue(0,0);
            addValue(1,0);
            return;
        }

        MembershipMatrix mm = new MembershipMatrix(clustering, points);

        int numClasses = mm.getNumClasses();
        if(mm.hasNoiseClass())
            numClasses--;

        /* custom fmeasure */
        double F1 = 0.0;
        double[] max_f1 = new double[numClasses];
        double sum = 0.;
        for (int i = 0; i < clustering.size(); i++) {
            for (int j = 0; j < numClasses; j++) {
                sum += (double)mm.getClusterClassWeight(i, j);
                if (mm.getClassSum(j) != 0. && mm.getClusterSum(i) != 0.) {
                    double recall = (double)mm.getClusterClassWeight(i, j)/(double)mm.getClassSum(j);
                    double precision = (double)mm.getClusterClassWeight(i, j)/(double)mm.getClusterSum(i);
                    double f1 = 0;
                    if(precision != 0 && recall != 0){
                        f1 = 2/(1/precision + 1/recall);
                    }
                    if(max_f1[j] < f1){
                        max_f1[j] = f1;
                    }
                }
            }
        }
        for (int j = 0; j < numClasses; j++) {
            F1 += ((double)mm.getClassSum(j)/sum) * max_f1[j];
        }
        if(F1 > 1){
            F1 = Double.NaN;
        }

        addValue("CustomFMeasure",F1);
    }

}
