/*
 *    SphereCluster.java
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

package moa.cluster;

import java.util.ArrayList;
import java.util.Random;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import java.util.Arrays;

/**
 * A simple implementation of the <code>Cluster</code> interface representing
 * ellipsoid clusters. The inclusion probability is one inside the sphere and zero
 * everywhere else.
 *
 */
public class EllipsoidCluster extends Cluster {

    private static final long serialVersionUID = 1L;

    protected double[] center;
    protected double[] radius;
    protected double weight;


    public EllipsoidCluster(double[] center, double[] radius) {
        this( center, radius, 1.0 );
    }

    public EllipsoidCluster() {}

    public EllipsoidCluster( double[] center, double[] radius, double weight) {
        this();
        this.center = center;
        this.radius = radius;
        this.weight = weight;
    }
    
    @Override
    public double[] getCenter() {
        double[] copy = new double[center.length];
        System.arraycopy(center, 0, copy, 0, center.length);
        return copy;
    }

    public void setCenter(double[] center) {
        this.center = center;
    }

    public double[] getRadius() {
        double[] copy = new double[radius.length];
        System.arraycopy(radius, 0, copy, 0, radius.length);
        return copy;
    }

    public void setRadius( double[] radius ) {
        this.radius = radius;
    }

    @Override
    public double getWeight() {
        return weight;
    }

    public void setWeight( double weight ) {
        this.weight = weight;
    }

    @Override
    public double getInclusionProbability(Instance instance) {
        center = getCenter();
        radius = getRadius();
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
    
    /**
    * Samples this cluster by returning a point from inside it.
    * @param random a random number source
    * @return a point that lies inside this cluster
    */
    @Override
   public Instance sample(Random random) {
        // Create sample in hypersphere coordinates
        //get the center through getCenter so subclass have a chance
        double[] center = getCenter();

        final int dimensions = center.length;

        final double sin[] = new double[dimensions - 1];
        final double cos[] = new double[dimensions - 1];
        double length[] = getRadius();
        for (int i = 0; i < length.length; i++) {
            length[i] *= random.nextDouble();
        }

        double lastValue = 1.0;
        for (int i = 0; i < dimensions-1; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            sin[i] = lastValue * Math.sin( angle ); // Store cumulative values
            cos[i] = Math.cos( angle );
            lastValue = sin[i];
        }

        // Calculate cartesian coordinates
        double res[] = new double[dimensions];

        // First value uses only cosines
        res[0] = center[0] + length[0]*cos[0];

        // Loop through 'middle' coordinates which use cosines and sines
        for (int i = 1; i < dimensions-1; i++) {
            res[i] = center[i] + length[i]*sin[i-1]*cos[i];
        }

        // Last value uses only sines
        res[dimensions-1] = center[dimensions-1] + length[dimensions-1]*sin[dimensions-2];

        return new DenseInstance(1.0, res);
   }

    @Override
    protected void getClusterSpecificInfo(ArrayList<String> infoTitle, ArrayList<String> infoValue) {
        super.getClusterSpecificInfo(infoTitle, infoValue);
        infoTitle.add("Radius");
        infoValue.add(Arrays.toString(getRadius()));
    }

}
