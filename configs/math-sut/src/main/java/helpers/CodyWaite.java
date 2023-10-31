package helpers;

/**
 * Enclose the Cody/Waite reduction (used in "sin", "cos" and "tan").
 */
public class CodyWaite {

    /**
     * k
     */
    private final int finalK;

    /**
     * remA
     */
    private final double finalRemA;

    /**
     * remB
     */
    private final double finalRemB;

    /**
     * @param xa Argument.
     */
    public CodyWaite(double xa) {
        // Estimate k.
        // k = (int)(xa / 1.5707963267948966);
        int k = (int) (xa * 0.6366197723675814);
        // Compute remainder.
        double remA;
        double remB;
        while (true) {
            double a = -k * 1.570796251296997;
            remA = xa + a;
            remB = -(remA - xa - a);
            a = -k * 7.549789948768648E-8;
            double b = remA;
            remA = a + b;
            remB += -(remA - b - a);
            a = -k * 6.123233995736766E-17;
            b = remA;
            remA = a + b;
            remB += -(remA - b - a);
            if (remA > 0) {
                break;
            }
            // Remainder is negative, so decrement k and try again.
            // This should only happen if the input is very close
            // to an even multiple of pi/2.
            --k;
        }
        this.finalK = k;
        this.finalRemA = remA;
        this.finalRemB = remB;
    }

    /**
     * @return k
     */
    public int getK() {
        return finalK;
    }

    /**
     * @return remA
     */
    public double getRemA() {
        return finalRemA;
    }

    /**
     * @return remB
     */
    public double getRemB() {
        return finalRemB;
    }
}
