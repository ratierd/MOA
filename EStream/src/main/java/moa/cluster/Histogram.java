/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.cluster;

/**
 *
 * @author David
 */
public class Histogram {
    
    /*------------------------------------------------------------------------*/
    // Attributes
    /*------------------------------------------------------------------------*/
    public static final int SIZE = 10;
    private double min;
    private double max;
    private double width;
    private boolean minFlag;
    private boolean maxFlag;
    public double[] height;
    public double[] upperBound;

    /*------------------------------------------------------------------------*/
    // Constructors, setters and getters
    /*------------------------------------------------------------------------*/
    /**
     * Constructor by copy
     *
     * @param hist copied histogram
     */
    public Histogram(Histogram hist) {
        this.min = hist.min;
        this.max = hist.max;
        this.width = hist.width;
        this.minFlag = hist.minFlag;
        this.maxFlag = hist.maxFlag;
        this.upperBound = hist.upperBound.clone();
        this.height = hist.height.clone();
    }

    /**
     * Default constructor
     */
    public Histogram() {
        this.minFlag = false;
        this.maxFlag = false;
        this.min = 0;
        this.max = 0;
        this.width = 0;
        this.height = new double[SIZE];
        this.upperBound = new double[SIZE];
    }

    /**
     * Get histogram width
     *
     * @return
     */
    public double getWidth() {
        return this.width;
    }

    /*------------------------------------------------------------------------*/
    // Methods
    /*------------------------------------------------------------------------*/
    /**
     * Add value with a default height of 1
     *
     * @param value value
     */
    public void add(double value) {
        this.add(value, 1);
    }

    /**
     * Add value with height
     *
     * @param value value
     * @param aHeight height
     */
    public void add(double value, double aHeight) {
        if (!minFlag) {
            minFlag = true;
            min = value;
            height[0] = aHeight;
        } else if (!maxFlag) {
            maxFlag = true;
            if (value < min) {
                max = min;
                min = value;
                height[SIZE - 1] = height[0];
                height[0] = aHeight;
                width = (max - min) / SIZE;
                updateInterval();
            } else if (value > min) {
                max = value;
                height[SIZE - 1] = aHeight;
                width = (max - min) / SIZE;
                updateInterval();
            } else {
                maxFlag = false;
                height[0] += aHeight;
            }
        } else {
            if (value < min) {
                min = value;
                update();
            } else if (value > max) {
                max = value;
                update();
            }

            int i = 0;
            while (value > upperBound[i] && i < SIZE) {
                i++;
            }
            height[i] += aHeight;
        }
    }

    /**
     * Merge with another histogram
     *
     * @param hist other histogram
     */
    public void merge(Histogram hist) {
        if (minFlag && maxFlag) {
            min = Double.min(min, hist.min);
            max = Double.max(max, hist.max);
            update();
        }
        double curPosition = hist.min + hist.width * 0.5;
        for (int i = 0; i < SIZE; i++) {
            add(curPosition, hist.height[i]);
            curPosition += hist.width;
        }
    }

    /**
     * Split histogram
     *
     * @param index split index
     * @return other part of the splited Histogram
     */
    public Histogram split(int index) {
        Histogram rightHist = new Histogram();
        if (index != -1) {
            double rightW = 0;
            double leftW = 0;
            for (int i = 0; i < SIZE; i++) {
                if (i <= index) {
                    leftW += height[i];
                } else {
                    rightW += height[i];
                }
            }
            if (leftW >= rightW) {
                System.arraycopy(height, index + 1, rightHist.height, index + 1, SIZE - (index + 1));
                rightHist.upperBound = upperBound.clone();
                rightHist.minFlag = true;
                rightHist.maxFlag = true;
                rightHist.min = upperBound[index];
                rightHist.max = upperBound[SIZE - 1];
                rightHist.width = (max - min) / SIZE;
                max = upperBound[index];
            } else {
                System.arraycopy(height, 0, rightHist.height, 0, index + 1);
                rightHist.upperBound = upperBound.clone();
                rightHist.minFlag = true;
                rightHist.maxFlag = true;
                rightHist.min = min;
                rightHist.max = upperBound[index];
                rightHist.width = (max - min) / SIZE;
                min = upperBound[index];
            }
            rightHist.update();
            update();
        }
        return rightHist;
    }

    /**
     * Find split index if there is one
     *
     * @return split index or -1 if none
     */
    public int findSplitIndex() {
        double significant = 3.843;
        int sensitivity = 5;
        int state = 0;
        int index = -1;
        int splitIndex = -1;
        double peek1;
        double peek2 = -1;
        double valley = -1;
        double minValley = -1;

        peek1 = height[0];
        for (int i = 0; i < SIZE; i++) {
            switch (state) {
                case 0:
                    if (height[i] > peek1 && height[i] >= sensitivity) {
                        peek1 = height[i];
                    } else if (height[i] < peek1) {
                        state = 1;
                        valley = height[i];
                        index = i;
                    }   break;
                case 1:
                    if (height[i] < valley) {
                        valley = height[i];
                        index = i;
                    } else if (height[i] > valley) {
                        state = 2;
                        peek2 = height[i];
                        double observed = valley;
                        double expected = (valley + Double.min(peek1, peek2)) / 2;
                        if (2 * Math.pow(observed - expected, 2) / expected >= significant) {
                            if (valley < minValley || splitIndex == -1) {
                                minValley = valley;
                                splitIndex = index;
                            }
                        }
                        if (peek2 > peek1) {
                            peek1 = peek2;
                            state = 1;
                        }
                        
                    }   break;
                case 2:
                    if (height[i] > peek2 && height[i] >= sensitivity) {
                        peek2 = height[i];
                    } else if (height[i] < peek2) {
                        double observed = valley;
                        double expected = (valley + Double.min(peek1, peek2)) / 2;
                        if (2 * Math.pow(observed - expected, 2) / expected >= significant) {
                            if (valley < minValley || splitIndex == -1) {
                                minValley = valley;
                                splitIndex = index;
                            }
                        }
                        if (peek2 > peek1) {
                            peek1 = peek2;
                            state = 1;
                        }
                    }   break;
                default:
                    break;
            }
        }
        return splitIndex;
    }

    /**
     * Update hidtogram
     */
    private void update() {
        double oldWidth = width;
        double[] oldUpperBound1 = upperBound.clone();
        double[] oldHeight = height.clone();

        updateInterval();

        double newLowerBound = min;
        double oldLowerBound = oldUpperBound1[0] - oldWidth;
        double newUpperBound = min + width;
        double oldUpperBound = oldUpperBound1[0];
        double intersection;
        int oldI = 0;
        int newI = 0;
        for (int i = 0; i < SIZE; i++) {
            height[i] = 0;
        }
        while (newI < SIZE && oldI < SIZE) {
            if (newLowerBound < oldLowerBound) {
                if (newUpperBound > oldLowerBound) {
                    if (newUpperBound < oldUpperBound) {
                        intersection = newUpperBound - oldLowerBound;
                        height[newI] += oldHeight[oldI] * intersection / oldWidth;
                        newI++;
                        newLowerBound += width;
                        newUpperBound += width;
                    } else {
                        intersection = oldWidth;
                        height[newI] += oldHeight[oldI] * intersection / oldWidth;
                        oldI++;
                        oldLowerBound += oldWidth;
                        oldUpperBound += oldWidth;
                    }
                } else {
                    newI++;
                    newLowerBound += width;
                    newUpperBound += width;
                }
            } else {
                if (newLowerBound < oldUpperBound) {
                    if (newUpperBound > oldUpperBound) {
                        intersection = oldUpperBound - newLowerBound;
                        height[newI] += oldHeight[oldI] * intersection / oldWidth;
                        oldI++;
                        oldLowerBound += oldWidth;
                        oldUpperBound += oldWidth;
                    } else {
                        intersection = width;
                        height[newI] += oldHeight[oldI] * intersection / oldWidth;
                        newI++;
                        newLowerBound += width;
                        newUpperBound += width;
                    }
                } else {
                    oldI++;
                    oldLowerBound += oldWidth;
                    oldUpperBound += oldWidth;
                }
            }
        }
    }

    /**
     * Update histogram interval
     */
    private void updateInterval() {
        width = (max - min) / SIZE;
        double curPosition = min;
        for (int i = 0; i < SIZE; i++) {
            curPosition += width;
            upperBound[i] = curPosition;
        }
        upperBound[SIZE - 1] = max;
    }

}
