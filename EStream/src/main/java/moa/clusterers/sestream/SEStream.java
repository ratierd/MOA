/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.clusterers.sestream;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import java.util.ArrayList;
import java.util.Comparator;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.cluster.FadingClusterWithBitVector;
import moa.cluster.FadingClusterWithBitVector.BitVector;
import moa.clusterers.AbstractClusterer;
import moa.core.AutoExpandVector;
import moa.core.Measurement;
import scala.Tuple3;

/**
 * E-Stream Clusterer
 *
 * @author david
 */
public final class SEStream extends AbstractClusterer {

    /*------------------------------------------------------------------------*/
    // Attributes
    /*------------------------------------------------------------------------*/
    public IntOption streamSpeedOption = new IntOption("streamSpeed", 's', "Speed of datastream", 100);
    private double constDiffTime;
    public IntOption maximumClustersOption = new IntOption("maxClusters", 'k', "Maximum number of clusters", 7);
    private int maximumClusters;
    public FloatOption radiusFactorOption = new FloatOption("radiusFactor", 'r', "Radius factor", 3);
    private double radiusFactor;
    public FloatOption decayRateOption = new FloatOption("decayRate", 'f', "Decay rate for fadding", 0.1);
    private double decayRate;
    public FloatOption fadeThresholdOption = new FloatOption("fadeThreshold", 't', "Fade Threshold", 0.1);
    private double fadeThreshold;
    public FloatOption mergeThresholdOption = new FloatOption("mergeThreshold", 'm', "Merge Threshold", 1.25);
    private double mergeThreshold;
    public FloatOption activeThresholdOption = new FloatOption("activeThreshold", 'a', "Active Threshold", 5.0);
    private double activeThreshold;
    public IntOption dimensionFactorOption = new IntOption("dimensionFactor", 'd', "Dimension Factor", 4);
    private int dimensionFactor;

    private AutoExpandVector<FadingClusterWithBitVector> clusters;
    private boolean initialized;

    /*------------------------------------------------------------------------*/
    // Constructors, setters and getters
    /*------------------------------------------------------------------------*/
    /**
     * Constructor
     */
    public SEStream() {
        this.resetLearningImpl();
    }

    /*------------------------------------------------------------------------*/
    // Methods
    /*------------------------------------------------------------------------*/
    /**
     * Initialize/Reinitialize clusterer fields
     */
    @Override
    public void resetLearningImpl() {
        constDiffTime = 1 / (double) streamSpeedOption.getValue();
        maximumClusters = maximumClustersOption.getValue();
        radiusFactor = radiusFactorOption.getValue();
        decayRate = decayRateOption.getValue();
        fadeThreshold = fadeThresholdOption.getValue();
        mergeThreshold = mergeThresholdOption.getValue();
        activeThreshold = activeThresholdOption.getValue();
        dimensionFactor = dimensionFactorOption.getValue();
        clusters = new AutoExpandVector<>();
        initialized = false;
    }
    
    int instanceCpt = 0;

    /**
     * Treatment sequence
     *
     * @param instance incoming datapoint
     */
    @Override
    public void trainOnInstanceImpl(Instance instance) {
        instanceCpt++;
        if (!this.initialized) {
            init(instance);
            initialized = true;
        }
        else {
            fadingAll();
            checkSplit();
            mergeOverlapClusters();
            limitMaximumClusters();
            if (flagActiveClusters()) {
                projectDimension();
            }
            addDatapoint(instance);
        }
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        throw new UnsupportedOperationException("Not used.");
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        throw new UnsupportedOperationException("Not used.");
    }

    @Override
    public boolean isRandomizable() {
        return false;
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        throw new UnsupportedOperationException("Not used.");
    }

    /**
     * Pass the clustering result to MOA internal system for measurement and
     * visualization
     *
     * @return return the clustering result
     */
    @Override
    public Clustering getClusteringResult() {
        AutoExpandVector<Cluster> res = new AutoExpandVector<>();
        for (FadingClusterWithBitVector fc : clusters) {
            if (fc.isCluster()) {
                res.add(fc);
            }
        }
        return new Clustering(res);
    }

    /**
     * Indicates if microClustering is enabled
     *
     * @return
     */
    @Override
    public boolean implementsMicroClusterer() {
        return false;
    }

    /**
     * Pass the micro clustering result to MOA internal system for measurement
     * and visualization
     *
     * @return return the micro clustering result
     */
    @Override
    public Clustering getMicroClusteringResult() {
        AutoExpandVector<Cluster> res = new AutoExpandVector<>();
        for (FadingClusterWithBitVector fc : clusters) {
            if (!fc.isCluster() && fc.getWeight() > 1) {
                res.add(fc);
            }
        }
        return new Clustering(res);
    }

    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Initialize clusterer with first datapoint
     * 
     * @param instance first datapoint
     */
    private void init(Instance instance) {
        clusters.add(new FadingClusterWithBitVector(instance));
    }

    /**
     * Fades all clusters and remove the unrelevent ones
     */
    private void fadingAll() {
        double fadingFactor = Math.pow(2, -decayRate * constDiffTime);
        for (int i = clusters.size() - 1; i >= 0; i--) {
            clusters.get(i).fading(fadingFactor);
            if (clusters.get(i).getWeight() < fadeThreshold) {
                clusters.remove(i);
            }
        }
    }

    /**
     * Check the spliting condition of each active clusters and split them if
     * needed !Then add the new splited cluster to collection
     */
    private void checkSplit() {
        for (int i = clusters.size() - 1; i >= 0; i--) {
            if(clusters.get(i).isCluster()) {
                if (clusters.get(i).checkSplit()) {
                    FadingClusterWithBitVector lowerFc = clusters.get(i).split();
                    clusters.add(lowerFc);
                }
            }
        }
    }

    /**
     * Merge active overlaping clusters !Then remove the remaining cluster from
     * collection to keep only the merged one
     */
    private void mergeOverlapClusters() {
        for (int i = 0; i < clusters.size(); i++) {
            FadingClusterWithBitVector fc1 = clusters.get(i);
            if(fc1.isCluster()) {
                for (int j = i + 1; j < clusters.size(); j++) {
                    FadingClusterWithBitVector fc2 = clusters.get(j);
                    if(fc2.isCluster()) {
                        if (fc1.isOverlapping(fc2, mergeThreshold)) {
                            FadingClusterWithBitVector testSplit = new FadingClusterWithBitVector(fc1);
                            testSplit.merge(fc2);
                            if (!testSplit.checkSplit()) {
                                fc1.merge(fc2);
                                clusters.remove(fc2);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Force merge between closest entities to limit the number of clusters
     */
    private void limitMaximumClusters() {
        AutoExpandVector<FadingClusterWithBitVector> targets = new AutoExpandVector<>();
        while (clusters.size() > maximumClusters) {
            for (FadingClusterWithBitVector fc : clusters) {
                if (!fc.isCluster()) {
                    targets.add(fc);
                }
            }
            if(targets.size() < 2) {
                targets.clear();
                for (FadingClusterWithBitVector fc : clusters) {
                    if(fc.isCluster()) {
                        targets.add(fc);
                    }
                }
            }
            mergeClosestClusterPair(targets);
        }
    }

    /**
     * Merge the two closest entities in the vector !Then remove entities from
     * collections corresponding to the type of merge occured
     *
     */
    private void mergeClosestClusterPair(AutoExpandVector<FadingClusterWithBitVector> targets) {
        double distance = Double.NaN;
        FadingClusterWithBitVector resFc1 = null;
        FadingClusterWithBitVector resFc2 = null;
        double weight = Double.NaN;
        
        for (int i = 0; i < targets.size(); i++) {
            FadingClusterWithBitVector fc1 = targets.get(i);
            for (int j = i + 1; j < targets.size(); j++) {
                FadingClusterWithBitVector fc2 = targets.get(j);
                double cDist = FadingClusterWithBitVector.getCenterDistance(fc1, fc2);
                if (cDist < distance || Double.isNaN(distance)) {
                    distance = cDist;
                    resFc1 = fc1;
                    resFc2 = fc2;
                    weight = fc1.getWeight() + fc2.getWeight();
                } else if (cDist == distance) {
                    if (fc1.getWeight() + fc2.getWeight() > weight) {
                        resFc1 = fc1;
                        resFc2 = fc2;
                        weight = fc1.getWeight() + fc2.getWeight();
                    }
                }
            }
        }
        if (resFc1 != null && resFc2 != null) {
            resFc1.merge(resFc2);
            clusters.remove(resFc2);
            targets.remove(resFc2);
        }
    }

    /**
     * Flag clusters depending on their weight and return a boolean
     * if a cluster has changed state
     */
    private boolean flagActiveClusters() {
        boolean hasChanged = false;
        for (FadingClusterWithBitVector fc : clusters) {
            hasChanged |= fc.isCluster() != fc.getWeight() >= activeThreshold;
            fc.setClusterFlag(fc.getWeight() >= activeThreshold);
        }
        return hasChanged;
    }
    
    /**
     * Project dimension of each cluster
     */
    private void projectDimension() {
        int nbDimension = clusters.get(0).getDimension();
        int countActive = 0;
        for (FadingClusterWithBitVector fc : clusters) {
            if (fc.isCluster()) {
                countActive++;
            }
        }
        int nbProjectedDim = (int) Math.ceil(dimensionFactor * countActive);
        
        //* fcId, dimId, overlapCount, radii *//
        ArrayList<Tuple3<Integer, Integer, Double>> radii = new ArrayList<>();
        
        for (int i = 0; i < clusters.size(); i++) {
            if (clusters.get(i).isCluster()) {
                clusters.get(i).setBitVector(new BitVector(nbDimension));
                
                double[] sd1 = clusters.get(i).sd();
                
                for (int dim = 0; dim < nbDimension; dim++) {
                    radii.add(new Tuple3(i, dim, sd1[dim]));
                }
            }
        }
        
        Comparator<Tuple3<Integer, Integer, Double>> SORT_RADII = new Comparator<Tuple3<Integer, Integer, Double>>() {
            @Override
            public int compare(Tuple3<Integer, Integer, Double> o1, Tuple3<Integer, Integer, Double> o2) {
                if (o1._3() < o2._3()) {
                    return -1;
                }
                else {
                    return -1;
                }
            }
        };
        
        radii.sort(SORT_RADII);
        
        for (int i = 0; i < radii.size(); i++) {
            if (i == nbProjectedDim) {
                break;
            }
            BitVector bitVector = new BitVector(nbDimension);
            bitVector.set(radii.get(i)._2(), true);
            bitVector.or(clusters.get(radii.get(i)._1()).getBitVector());
            clusters.get(radii.get(i)._1()).setBitVector(bitVector);
        }
        
        FadingClusterWithBitVector.deleteZeroBitVectorCluster(clusters);
    }

    /**
     * Add datapoint to the clustering
     *
     * @param instance current datapoint
     */
    private void addDatapoint(Instance instance) {
        if (!addToClosestCluster(instance)) {
            clusters.add(new FadingClusterWithBitVector(instance));
        }
    }

    /**
     * Try adding datapoint to the closest cluster
     *
     * @param instance datapoint
     * @return return true if the closest cluster was close enought from the
     * datapoint, return false otherwise
     */
    private boolean addToClosestCluster(Instance instance) {
        double distance = Double.NaN;
        FadingClusterWithBitVector res = null;
        double weight = Double.NaN;
        for (FadingClusterWithBitVector fc : clusters) {
            if (fc.isCluster()) {
                double cDist = FadingClusterWithBitVector.getNormalizedDistance(fc, instance);
                if ((cDist < distance || Double.isNaN(distance))) {
                    distance = cDist;
                    res = fc;
                    weight = fc.getWeight();
                } else if (cDist == distance) {
                    if (fc.getWeight() > weight) {
                        res = fc;
                        weight = fc.getWeight();
                    }
                }
            }
        }
        if (res == null || Double.isNaN(distance)) {
            return false;
        }
        if (distance < radiusFactor) {
            res.addDatapoint(instance);
            return true;
        }
        return false;
    }

}
