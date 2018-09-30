/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.cluster;

import com.yahoo.labs.samoa.instances.Instance;
import moa.core.AutoExpandVector;

/**
 *
 * @author David
 */
public class FadingClusterWithBitVector extends FadingCluster {
    
    /*------------------------------------------------------------------------*/
    // Attributes
    /*------------------------------------------------------------------------*/
    private static final long serialVersionUID = 1L;
    private static int idCounter = 0;
    
    //bitVector Methods----------------------------------------------

    public static class BitVector {

        private final boolean[] bitVector;
        private final int length;

        public BitVector(int length) {
            this.bitVector = new boolean[length];
            for (int i = 0; i < length; i++) {
                this.bitVector[i] = false;
            }
            this.length = length;
        }

        public int length() {
            return length;
        }

        public boolean get(int index) {
            return bitVector[index];
        }

        public void set(int index, boolean value) {
            bitVector[index] = value;
        }

        public boolean[] getAll() {
            return bitVector;
        }

        public void setAll(boolean value) {
            for (int i = 0; i < length; i++) {
                bitVector[i] = value;
            }
        }

        public BitVector copy(){
            BitVector copy = new BitVector(length);
            for (int i = 0; i < length; i++) {
                copy.bitVector[i] = bitVector[i];
            }
            return copy;
        }

        public void or(BitVector other) {
            int minLength = Math.min(length, other.length);
            for (int i = 0; i < minLength; i++) {
                bitVector[i] |= other.bitVector[i];
            }
        }

        public void and(BitVector other) {
            int minLength = Math.min(length, other.length);
            for (int i = 0; i < minLength; i++) {
                bitVector[i] |= other.bitVector[i];
            }
        }

        public void flip(int index) {
            bitVector[index] = !bitVector[index];
        }

        public void flipAll() {
            for (int i = 0; i < length; i++) {
                flip(i);
            }
        }

        public void flip(int start, int end) {
            for(int i = start; i <= end; i++) {
                flip(i);
            }
        }

        public int cardinality() {
            int cpt = 0;
            for (int i = 0; i < length; i++) {
                if (bitVector[i]) {
                    cpt++;
                }
            }
            return cpt;
        }

    }

    private BitVector bitVector;

    /*------------------------------------------------------------------------*/
    // Constructors, setters and getters
    /*------------------------------------------------------------------------*/
    /**
     * Constructor : set the cluster id and init the cluster
     * @param instance
     */
    public FadingClusterWithBitVector(Instance instance) {
        super(instance);
        idCounter++;
        
        double[] datapoint = instance.toDoubleArray();
        bitVector = new BitVector(datapoint.length);
        bitVector.flipAll();
    }
    
    /**
     * Constructor by copy
     * @param fc 
     */
    public FadingClusterWithBitVector(FadingClusterWithBitVector fc) {
        super(fc);
        idCounter++;
        
        bitVector = fc.bitVector.copy();
    }
    
    public double[] getCenter(BitVector projectionBitVector) {
        double[] center = new double[getDimension()];
        for (int i = 0; i < center.length; i++) {
            if(projectionBitVector.get(i)) {
                center[i] = LS[i] / getWeight();
            }
            else {
                center[i] = 0;
            }
        }
        return center;
    }
    
    public double[] sd(BitVector projectionBitVector) {
        double[] sd = new double[LS.length];
        for (int i = 0; i < sd.length; i++) {
            if (projectionBitVector.get(i)) {
                sd[i] = SS[i] / getWeight() - (Math.pow(LS[i] / getWeight(), 2));
                if(sd[i] < 0.00001){
                    sd[i] = 0.00001;
                }
                else {
                    sd[i] = Math.sqrt(sd[i]);
                }
            }
            else {
                sd[i] = 0;
            }
        }
        return sd;
    }
    
    public BitVector getBitVector() {
        return bitVector;
    }
    
    public void setBitVector(BitVector bitVector) {
        this.bitVector = bitVector;
    }
    
    @Override
    public void setClusterFlag(boolean value) {
        super.setClusterFlag(value);
        if(!clusterFlag) {
            BitVector bitVector = new BitVector(getDimension());
            bitVector.flipAll();
            setBitVector(bitVector);
        }
    }
    
    /*------------------------------------------------------------------------*/
    // Static Methods
    /*------------------------------------------------------------------------*/
    
    public static BitVector getMergedBitVector(FadingClusterWithBitVector fc1, FadingClusterWithBitVector fc2) {
        BitVector result = fc1.bitVector.copy();
        result.or(fc2.bitVector);
        return result;
    }
    
    public static double getCenterDistance(FadingClusterWithBitVector fc, Instance instance) {
        assert fc.getDimension() == instance.toDoubleArray().length;
        double distance = 0.;
        double[] fcCenter = fc.getCenter();
        double[] datapoint = instance.toDoubleArray();
        BitVector bitVector = fc.bitVector;
        for (int i = 0; i < fc.getDimension(); i++) {
            if(bitVector.get(i)) {
                distance += Math.abs(fcCenter[i] - datapoint[i]);
            }
        }
        distance /= bitVector.cardinality();
        return distance;
    }
    
    public static double getCenterDistance(FadingClusterWithBitVector fc1, FadingClusterWithBitVector fc2) {
        assert fc1.getDimension() == fc2.getDimension();
        double distance = 0.;
        double[] fc1Center = fc1.getCenter();
        double[] fc2Center = fc2.getCenter();
        BitVector mergedBitVector = getMergedBitVector(fc1, fc2);
        for (int i = 0; i < fc1.getDimension(); i++) {
            if(mergedBitVector.get(i)) {
                distance += Math.abs(fc1Center[i] - fc2Center[i]);
            }
        }
        distance /= mergedBitVector.cardinality();
        return distance;
    }
    
    public static double getNormalizedDistance(FadingClusterWithBitVector fc, Instance instance) {
        assert fc.getDimension() == instance.toDoubleArray().length;
        double distance = 0.;
        double[] fcCenter = fc.getCenter();
        double[] fcSd = fc.sd();
        double[] datapoint = instance.toDoubleArray();
        BitVector bitVector = fc.bitVector;
        for (int i = 0; i < fc.getDimension(); i++) {
            if(bitVector.get(i)) {
                distance += Math.abs(fcCenter[i] - datapoint[i]) / (fcSd[i]);
            }
        }
        distance /= bitVector.cardinality();
        return distance;
    }
    
    public static double getNormalizedDistance(FadingClusterWithBitVector fc1, FadingClusterWithBitVector fc2) {
        assert fc1.getDimension() == fc2.getDimension();
        double distance = 0.;
        double[] fc1Center = fc1.getCenter();
        double[] fc1Sd = fc1.sd();
        double[] fc2Center = fc2.getCenter();
        double[] fc2Sd = fc2.sd();
        BitVector mergedBitVector = getMergedBitVector(fc1, fc2);
        for (int i = 0; i < fc1.getDimension(); i++) {
            if(mergedBitVector.get(i)) {
                distance += Math.abs(fc1Center[i] - fc2Center[i]) / (fc1Sd[i] + fc2Sd[i]);
            }
        }
        distance /= mergedBitVector.cardinality();
        return distance;
    }
    
    public static double[] normalize(double[] sd, double[] datapoint){
        double[] normalizedDatapoint = datapoint.clone();
        for (int i = 0; i < normalizedDatapoint.length; i++) {
            if (sd[i] != 0) {
                normalizedDatapoint[i] /= sd[i];
            }
            else {
                normalizedDatapoint[i] = 0;
            }
        }
        return normalizedDatapoint;
    }
    
    public static AutoExpandVector<FadingClusterWithBitVector> normalize(double[] globalSd, AutoExpandVector<FadingClusterWithBitVector> clusters) {
        AutoExpandVector<FadingClusterWithBitVector> normalizedClusters = new AutoExpandVector<>();
        for (FadingClusterWithBitVector fc : clusters) {
            normalizedClusters.add(fc);
        }
        double[] ss  = new double[globalSd.length];
        for (int i = 0; i < ss.length; i++) {
            ss[i] = Math.pow(globalSd[i], 2);
        }
        for (int i = 0; i < normalizedClusters.size(); i++) {
            for (int j = 0; j < ss.length; j++) {
                if (globalSd[j] != 0) {
                    normalizedClusters.get(i).LS[j] = clusters.get(i).LS[j] / globalSd[j];
                    normalizedClusters.get(i).SS[j] = clusters.get(i).SS[j] / ss[j];
                }
            }
        }
        return normalizedClusters;
    }
    
    public static void deleteZeroBitVectorCluster(AutoExpandVector<FadingClusterWithBitVector> fcSet) {
        for (int i = fcSet.size() - 1; i >= 0; i--) {
            BitVector bitVector = fcSet.get(i).getBitVector();
            boolean result = false;
            for (int j = 0; j < fcSet.get(i).getDimension(); j++) {
                result |= bitVector.get(j);
            }
            if (!result) {
                fcSet.remove(fcSet.get(i));
            }
        }
    }

    /*------------------------------------------------------------------------*/
    // Methods
    /*------------------------------------------------------------------------*/
    
    /**
     * Split the cluster
     *
     * @return return the other part of the splited cluster
     */
    public FadingClusterWithBitVector split() {
        //split itself and return one of the splited by value
        FadingClusterWithBitVector lowerFc = new FadingClusterWithBitVector(this);
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
    public void merge(FadingClusterWithBitVector other) {
        super.merge(other);
        //Hull
        if (isCluster() && other.isCluster()) {
            bitVector.or(other.bitVector);
        }
        else if (other.isCluster()) {
            bitVector = other.bitVector.copy();
        }
        else {
            bitVector.setAll(true);
        }
    }
    
    /**
     * Check wether or not the cluster overlaps with another
     * 
     * @param fc2 other cluster
     * @param mergeThreshold merging threshold value
     * @return 
     */
    public boolean isOverlapping(FadingClusterWithBitVector fc2, double mergeThreshold){
        double distance = 0;
        double[] fc1Center = getCenter();
        double[] fc1Sd = sd();
        double[] fc2Center = fc2.getCenter();
        double[] fc2Sd = fc2.sd();
        BitVector mergedBitVector = getMergedBitVector(this, fc2);
        for (int i = 0; i < getDimension(); i++) {
            if(mergedBitVector.get(i)) {
                distance += Math.abs(fc1Center[i] - fc2Center[i]) - mergeThreshold * (fc1Sd[i] + fc2Sd[i]);
            }
        }
        distance /= mergedBitVector.cardinality();
        return distance <= 1;
    }

}
