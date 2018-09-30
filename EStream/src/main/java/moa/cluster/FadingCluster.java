/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.cluster;

import com.yahoo.labs.samoa.instances.Instance;
import java.util.ArrayList;

/**
 *
 * @author David
 */
public class FadingCluster extends EllipsoidCluster {

    /*------------------------------------------------------------------------*/
    // Attributes
    /*------------------------------------------------------------------------*/
    private static final long serialVersionUID = 1L;
    private static int idCounter = 0;
    
    protected final double[] LS;
    protected final double[] SS;
    protected final Histogram[] histograms;
    protected boolean clusterFlag;
    
    protected int splitIndex = -1;
    protected int splitAttribute = -1;

    /*------------------------------------------------------------------------*/
    // Constructors, setters and getters
    /*------------------------------------------------------------------------*/
    /**
     * Constructor : set the cluster id and init the cluster
     * @param instance
     */
    public FadingCluster(Instance instance) {
        super.setId(idCounter);
        idCounter++;
        
        double[] datapoint = instance.toDoubleArray();
        
        weight = instance.weight();
        LS = datapoint.clone();
        SS = new double[datapoint.length];
        for (int i = 0; i < SS.length; i++) {
            SS[i] = Math.pow(LS[i], 2);
        }
        histograms = new Histogram[datapoint.length];
        for (int i = 0; i < histograms.length; i++) {
            histograms[i] = new Histogram();
            histograms[i].add(datapoint[i]);
        }
        clusterFlag = false;
    }
    
    /**
     * Constructor by copy
     * @param fc 
     */
    public FadingCluster(FadingCluster fc) {
        super.setId(idCounter);
        idCounter++;
        
        weight = fc.weight;
        LS = fc.LS.clone();
        SS = fc.SS.clone();
        this.histograms = new Histogram[fc.histograms.length];
        for (int i = 0; i < this.histograms.length; i++) {
            this.histograms[i] = new Histogram(fc.histograms[i]);
        }
        clusterFlag = fc.clusterFlag;
    }
    
    public int getDimension(){
        return LS.length;
    }
    
    @Override
    public double[] getCenter() {
        double[] center = new double[getDimension()];
        for (int i = 0; i < center.length; i++) {
            center[i] = LS[i] / getWeight();
        }
        return center;
    }

    @Override
    public double[] getRadius() {
        double[] sd = sd();
        setRadius(sd);
        return sd;
    }
    
    public double[] sd() {
        double[] sd = new double[LS.length];
        for (int i = 0; i < sd.length; i++) {
            sd[i] = SS[i] / getWeight() - (Math.pow(LS[i] / getWeight(), 2));
            if(sd[i] < 0.00001){
                sd[i] = 0.00001;
            }
            else {
                sd[i] = Math.sqrt(sd[i]);
            }
        }
        return sd;
    }
    
    public boolean isCluster() {
        return clusterFlag;
    }
    
    public void setClusterFlag(boolean value) {
        clusterFlag = value;
    }
    
    /*------------------------------------------------------------------------*/
    // Static Methods
    /*------------------------------------------------------------------------*/
    
    public static double getCartesianDistance(FadingCluster fc, Instance instance) {
        assert fc.getDimension() == instance.toDoubleArray().length;
        double distance = 0.;
        double[] fcCenter = fc.getCenter();
        double[] datapoint = instance.toDoubleArray();
        for (int i = 0; i < fc.getDimension(); i++) {
            distance += Math.pow(fcCenter[i] - datapoint[i], 2);
        }
        distance = Math.sqrt(distance);
        return distance;
    }
    
    public static double getCartesianDistance(FadingCluster fc1, FadingCluster fc2) {
        assert fc1.getDimension() == fc2.getDimension();
        double distance = 0.;
        double[] fc1Center = fc1.getCenter();
        double[] fc2Center = fc2.getCenter();
        for (int i = 0; i < fc1.getDimension(); i++) {
            distance += Math.pow(fc1Center[i] - fc2Center[i], 2);
        }
        distance = Math.sqrt(distance);
        return distance;
    }
    
    public static double getCenterDistance(FadingCluster fc, Instance instance) {
        assert fc.getDimension() == instance.toDoubleArray().length;
        double distance = 0.;
        double[] fcCenter = fc.getCenter();
        double[] datapoint = instance.toDoubleArray();
        for (int i = 0; i < fc.getDimension(); i++) {
            distance += Math.abs(fcCenter[i] - datapoint[i]);
        }
        distance /= fc.getDimension();
        return distance;
    }
    
    public static double getCenterDistance(FadingCluster fc1, FadingCluster fc2) {
        assert fc1.getDimension() == fc2.getDimension();
        double distance = 0.;
        double[] fc1Center = fc1.getCenter();
        double[] fc2Center = fc2.getCenter();
        for (int i = 0; i < fc1.getDimension(); i++) {
            distance += Math.abs(fc1Center[i] - fc2Center[i]);
        }
        distance /= fc1.getDimension();
        return distance;
    }
    
    public static double getNormalizedDistance(FadingCluster fc, Instance instance) {
        assert fc.getDimension() == instance.toDoubleArray().length;
        double distance = 0.;
        double[] fcCenter = fc.getCenter();
        double[] fcSd = fc.sd();
        double[] datapoint = instance.toDoubleArray();
        for (int i = 0; i < fc.getDimension(); i++) {
            distance += Math.abs(fcCenter[i] - datapoint[i]) / (fcSd[i]);
        }
        distance /= fc.getDimension();
        return distance;
    }
    
    public static double getNormalizedDistance(FadingCluster fc1, FadingCluster fc2) {
        assert fc1.getDimension() == fc2.getDimension();
        double distance = 0.;
        double[] fc1Center = fc1.getCenter();
        double[] fc1Sd = fc1.sd();
        double[] fc2Center = fc2.getCenter();
        double[] fc2Sd = fc2.sd();
        for (int i = 0; i < fc1.getDimension(); i++) {
            distance += Math.abs(fc1Center[i] - fc2Center[i]) / (fc1Sd[i] + fc2Sd[i]);
        }
        distance /= fc1.getDimension();
        return distance;
    }

    /*------------------------------------------------------------------------*/
    // Methods
    /*------------------------------------------------------------------------*/
    /**
     * Fade the entitie
     *
     * @param fadingFactor fading factor
     */
    public void fading(double fadingFactor) {
        if(weight > 0.00001) {
            weight *= fadingFactor;
            for (int i = 0; i < getDimension(); i++) {
                this.LS[i] *= fadingFactor;
                this.SS[i] *= fadingFactor;
                for (int j = 0; j < Histogram.SIZE; j++) {
                    histograms[i].height[j] *= fadingFactor;
                }
            }
        }
    }
    
    /**
     * Check whether or not the cluster needs to be splited
     *
     * @return true if the cluster needs to be splited, false otherwise
     */
    public boolean checkSplit() {     
        //find split attribute and split point
        for (int i = 0; i < getDimension(); i++) {
            splitIndex = histograms[i].findSplitIndex();
            if (splitIndex != -1) {
                splitAttribute = i;
                return true;
            }
        }
        return false;
    }
    
    /**
     * Split the cluster
     *
     * @return return the other part of the splited cluster
     */
    public FadingCluster split() {
        //split itself and return one of the splited by value
        FadingCluster lowerFc = new FadingCluster(this);
        if (splitIndex != -1) {
            double greaterW = 0;
            double lowerW = 0;
            double height;
            double position;

            //cal weight of left and right histogram at split attribute
            for (int i = 0; i < Histogram.SIZE; i++) {
                height = histograms[splitAttribute].height[i];
                if (i <= splitIndex) {
                    greaterW += height;
                } else {
                    lowerW += height;
                }
            }
            if (lowerW > greaterW) {
                double temp = lowerW;
                lowerW = greaterW;
                greaterW = temp;
            }

            double lowerFactor = lowerW / this.getWeight();
            double greaterFactor = greaterW / this.getWeight();
            for (int i = 0; i < getDimension(); i++) {
                if (i == splitAttribute) {//split attribute
                    LS[splitAttribute] = 0;
                    SS[splitAttribute] = 0;
                    lowerFc.LS[splitAttribute] = 0;
                    lowerFc.SS[splitAttribute] = 0;
                    //split histogram
                    lowerFc.histograms[splitAttribute] = histograms[splitAttribute].split(splitIndex);
                    //cal fc1 and fc2
                    for (int hIndex = 0; hIndex < Histogram.SIZE; hIndex++) {
                        double halfWidth = histograms[splitAttribute].getWidth() * 0.5;
                        height = histograms[splitAttribute].height[hIndex];
                        position = histograms[splitAttribute].upperBound[hIndex] - halfWidth;
                        LS[splitAttribute] += height * position;
                        SS[splitAttribute] += height * Math.pow(position, 2);

                        halfWidth = lowerFc.histograms[splitAttribute].getWidth() * 0.5;
                        height = lowerFc.histograms[splitAttribute].height[hIndex];
                        position = lowerFc.histograms[splitAttribute].upperBound[hIndex] - halfWidth;
                        lowerFc.LS[splitAttribute] += height * position;
                        lowerFc.SS[splitAttribute] += height * Math.pow(position, 2);
                    }
                } else {//other attribute
                    //decrease fc1 and fc2
                    lowerFc.LS[i] *= lowerFactor;
                    lowerFc.SS[i] *= lowerFactor;
                    LS[i] *= greaterFactor;
                    SS[i] *= greaterFactor;
                    //decrease histogram
                    for (int hIndex = 0; hIndex < Histogram.SIZE; hIndex++) {
                        lowerFc.histograms[i].height[hIndex] *= lowerFactor;
                        histograms[i].height[hIndex] *= greaterFactor;
                    }
                }
            }
            lowerFc.weight = lowerW;
            weight = greaterW;
        }
        //reset splitIndex and splitAttribute
        splitIndex = -1;
        splitAttribute = -1;

        return lowerFc;
    }
    
    /**
     * Merge this cluster with an other entitie
     *
     * @param other entitie
     */
    public void merge(FadingCluster other) {
        assert getDimension() == other.getDimension();
        weight += other.weight;
        for (int i = 0; i < getDimension(); i++) {
            this.LS[i] += other.LS[i];
            this.SS[i] += other.SS[i];
            this.histograms[i].merge(other.histograms[i]);
        }
    }
    
    /**
     * Add a datapoint to this cluster
     * 
     * @param instance datapoint
     */
    public void addDatapoint(Instance instance) {
        weight++;
        double[] datapoint = instance.toDoubleArray();
        for (int i = 0; i < getDimension(); i++) {
            LS[i] += datapoint[i];
            SS[i] += Math.pow(datapoint[i], 2);
        }
        for (int i = 0; i < getDimension(); i++) {
            histograms[i].add(datapoint[i]);
        }
    }
    
    /**
     * Check wether or not the cluster overlaps with another
     * 
     * @param fc2 other cluster
     * @param mergeThreshold merging threshold value
     * @return 
     */
    public boolean isOverlapping(FadingCluster fc2, double mergeThreshold){
        double d = 0;
        double[] fc1Center = getCenter();
        double[] fc1Sd = sd();
        double[] fc2Center = fc2.getCenter();
        double[] fc2Sd = fc2.sd();
        for (int i = 0; i < getDimension(); i++) {
            d += Math.abs(fc1Center[i] - fc2Center[i]) - mergeThreshold * (fc1Sd[i] + fc2Sd[i]);
        }
        d /= getDimension();
        return d <= 1;
    }
    
    @Override
    public double getInclusionProbability(Instance instance) {
        double[] center = getCenter();
        double[] radius = getRadius();
        assert instance.numAttributes() == center.length;
        assert center.length > 1;
        double result = 0.0;
        double[] datapoint = instance.toDoubleArray();
        for (int i = 0; i < center.length; i++) {
            result += Math.pow(datapoint[i] - center[i], 2) / Math.pow(radius[i], 2);
        }
        if (result <= 1.0) {
            return 1.0;
        }
        return 0.0;
    }

    @Override
    protected void getClusterSpecificInfo(ArrayList<String> infoTitle, ArrayList<String> infoValue) {
        super.getClusterSpecificInfo(infoTitle, infoValue);

        double ls[] = LS.clone();
        for (int i = 0; i < ls.length; i++) {
            infoTitle.add("LS[" + i + "]");
            infoValue.add(Double.toString(ls[i]));
        }

        double ss[] = SS.clone();
        for (int i = 0; i < ss.length; i++) {
            infoTitle.add("SS[" + i + "]");
            infoValue.add(Double.toString(ss[i]));
        }
    }

}
