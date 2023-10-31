import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicReference;
import helpers.CodyWaite;
import static helpers.Constants.*;

public class MathClass {

    /**
     * Miller-Rabin probabilistic primality test for int type, used in such a way that a result is always guaranteed.
     * <p>
     * It uses the prime numbers as successive base therefore it is guaranteed to be always correct.
     * (see Handbook of applied cryptography by Menezes, table 4.1)
     *
     * @param n number to test: an odd integer &ge; 3
     * @return true if n is prime. false if n is definitely composite.
     */
    public static boolean millerRabinPrimeTest(final int n) {
        final int nMinus1 = n - 1;
        final int s = Integer.numberOfTrailingZeros(nMinus1);
        final int r = nMinus1 >> s;
        // r must be odd, it is not checked here
        int t = 1;
        if (n >= 2047) {
            t = 2;
        }
        if (n >= 1373653) {
            t = 3;
        }
        if (n >= 25326001) {
            t = 4;
        }
        // works up to 3.2 billion, int range stops at 2.7 so we are safe :-)
        BigInteger br = BigInteger.valueOf(r);
        BigInteger bn = BigInteger.valueOf(n);
        for (int i = 0; i < t; i++) {
            BigInteger a = BigInteger.valueOf(PRIMES[i]);
            BigInteger bPow = a.modPow(br, bn);
            int y = bPow.intValue();
            if ((1 != y) && (y != nMinus1)) {
                int j = 1;
                while ((j <= s - 1) && (nMinus1 != y)) {
                    long square = ((long) y) * y;
                    y = (int) (square % n);
                    if (1 == y) {
                        return false;
                    }
                    // definitely composite
                    j++;
                }
                if (nMinus1 != y) {
                    return false;
                }
            // definitely composite
            }
        }
        // definitely prime
        return true;
    }

    /**
     * Primality test: tells if the argument is a (provable) prime or not.
     * <p>
     * It uses the Miller-Rabin probabilistic test in such a way that a result is guaranteed:
     * it uses the firsts prime numbers as successive base (see Handbook of applied cryptography
     * by Menezes, table 4.1).
     *
     * @param n number to test.
     * @return true if n is prime. (All numbers &lt; 2 return false).
     */
    public static boolean isPrime(int n) {
        if (n < 2) {
            return false;
        }
        for (int p : PRIMES) {
            if (0 == (n % p)) {
                return n == p;
            }
        }
        return millerRabinPrimeTest(n);
    }

    /**
     * Return the smallest prime greater than or equal to n.
     *
     * @param n a positive number.
     * @return the smallest prime greater than or equal to n.
     * @throws MathIllegalArgumentException if n &lt; 0.
     */
    public static int nextPrime(int n) {
        if (n < 0) {
            throw new java.lang.IllegalArgumentException();
        }
        if (n == 2) {
            return 2;
        }
        // make sure n is odd
        n |= 1;
        if (n == 1) {
            return 2;
        }
        if (isPrime(n)) {
            return n;
        }
        // prepare entry in the +2, +4 loop:
        // n should not be a multiple of 3
        final int rem = n % 3;
        if (0 == rem) {
            // if n % 3 == 0
            // n % 3 == 2
            n += 2;
        } else if (1 == rem) {
            // if n % 3 == 1
            // if (isPrime(n)) return n;
            // n % 3 == 2
            n += 4;
        }
        while (true) {
            // this loop skips all multiple of 3
            if (isPrime(n)) {
                return n;
            }
            // n % 3 == 1
            n += 2;
            if (isPrime(n)) {
                return n;
            }
            // n % 3 == 2
            n += 4;
        }
    }

    /**
     * Absolute value.
     * @param x number from which absolute value is requested
     * @return abs(x)
     */
    public static int abs(final int x) {
        final int i = x >>> 31;
        return (x ^ (~i + 1)) + i;
    }

    /**
     * Compute the minimum of two values
     * @param a first value
     * @param b second value
     * @return a if a is lesser or equal to b, b otherwise
     */
    public static int min(final int a, final int b) {
        return (a <= b) ? a : b;
    }

    /**
     * Computes the greatest common divisor of two <em>positive</em> numbers
     * (this precondition is <em>not</em> checked and the result is undefined
     * if not fulfilled) using the "binary gcd" method which avoids division
     * and modulo operations.
     * See Knuth 4.5.2 algorithm B.
     * The algorithm is due to Josef Stein (1961).
     * <br/>
     * Special cases:
     * <ul>
     *  <li>The result of {@code gcd(x, x)}, {@code gcd(0, x)} and
     *   {@code gcd(x, 0)} is the value of {@code x}.</li>
     *  <li>The invocation {@code gcd(0, 0)} is the only one which returns
     *   {@code 0}.</li>
     * </ul>
     *
     * @param a Positive number.
     * @param b Positive number.
     * @return the greatest common divisor.
     */
    private static int gcdPositive(int a, int b) {
        if (a == 0) {
            return b;
        } else if (b == 0) {
            return a;
        }
        // Make "a" and "b" odd, keeping track of common power of 2.
        final int aTwos = Integer.numberOfTrailingZeros(a);
        a >>= aTwos;
        final int bTwos = Integer.numberOfTrailingZeros(b);
        b >>= bTwos;
        final int shift = min(aTwos, bTwos);
        // "b" becomes the minimum of the current values.
        while (a != b) {
            final int delta = a - b;
            b = Math.min(a, b);
            a = Math.abs(delta);
            // Remove any power of 2 in "a" ("b" is guaranteed to be odd).
            a >>= Integer.numberOfTrailingZeros(a);
        }
        // Recover the common power of 2.
        return a << shift;
    }

    /**
     * Computes the greatest common divisor of the absolute value of two
     * numbers, using a modified version of the "binary gcd" method.
     * See Knuth 4.5.2 algorithm B.
     * The algorithm is due to Josef Stein (1961).
     * <br/>
     * Special cases:
     * <ul>
     *  <li>The invocations
     *   {@code gcd(Integer.MIN_VALUE, Integer.MIN_VALUE)},
     *   {@code gcd(Integer.MIN_VALUE, 0)} and
     *   {@code gcd(0, Integer.MIN_VALUE)} throw an
     *   {@code ArithmeticException}, because the result would be 2^31, which
     *   is too large for an int value.</li>
     *  <li>The result of {@code gcd(x, x)}, {@code gcd(0, x)} and
     *   {@code gcd(x, 0)} is the absolute value of {@code x}, except
     *   for the special cases above.</li>
     *  <li>The invocation {@code gcd(0, 0)} is the only one which returns
     *   {@code 0}.</li>
     * </ul>
     *
     * @param p Number.
     * @param q Number.
     * @return the greatest common divisor (never negative).
     * @throws MathArithmeticException if the result cannot be represented as
     * a non-negative {@code int} value.
     * @since 1.1
     */
    public static int gcd(int p, int q) throws java.lang.ArithmeticException {
        int a = p;
        int b = q;
        if (a == 0 || b == 0) {
            if (a == Integer.MIN_VALUE || b == Integer.MIN_VALUE) {
                throw new java.lang.ArithmeticException();
            }
            return abs(a + b);
        }
        long al = a;
        long bl = b;
        boolean useLong = false;
        if (a < 0) {
            if (Integer.MIN_VALUE == a) {
                useLong = true;
            } else {
                a = -a;
            }
            al = -al;
        }
        if (b < 0) {
            if (Integer.MIN_VALUE == b) {
                useLong = true;
            } else {
                b = -b;
            }
            bl = -bl;
        }
        if (useLong) {
            if (al == bl) {
                throw new java.lang.ArithmeticException();
            }
            long blbu = bl;
            bl = al;
            al = blbu % al;
            if (al == 0) {
                if (bl > Integer.MAX_VALUE) {
                    throw new java.lang.ArithmeticException();
                }
                return (int) bl;
            }
            blbu = bl;
            // Now "al" and "bl" fit in an "int".
            b = (int) al;
            a = (int) (blbu % al);
        }
        return gcdPositive(a, b);
    }

    /**
     * Multiply two integers, checking for overflow.
     *
     * @param x Factor.
     * @param y Factor.
     * @return the product {@code x * y}.
     * @throws MathArithmeticException if the result can not be
     * represented as an {@code int}.
     * @since 1.1
     */
    public static int mulAndCheck(int x, int y) throws java.lang.IllegalArgumentException {
        long m = ((long) x) * ((long) y);
        if (m < Integer.MIN_VALUE || m > Integer.MAX_VALUE) {
            throw new java.lang.IllegalArgumentException();
        }
        return (int) m;
    }

    /**
     * Multiply two long integers, checking for overflow.
     *
     * @param a Factor.
     * @param b Factor.
     * @return the product {@code a * b}.
     * @throws MathArithmeticException if the result can not be represented
     * as a {@code long}.
     * @since 1.2
     */
    public static long mulAndCheck(long a, long b) throws java.lang.ArithmeticException {
        long ret;
        if (a > b) {
            // use symmetry to reduce boundary cases
            ret = mulAndCheck(b, a);
        } else {
            if (a < 0) {
                if (b < 0) {
                    // check for positive overflow with negative a, negative b
                    if (a >= Long.MAX_VALUE / b) {
                        ret = a * b;
                    } else {
                        throw new java.lang.ArithmeticException();
                    }
                } else if (b > 0) {
                    // check for negative overflow with negative a, positive b
                    if (Long.MIN_VALUE / b <= a) {
                        ret = a * b;
                    } else {
                        throw new java.lang.ArithmeticException();
                    }
                } else {
                    // assert b == 0
                    ret = 0;
                }
            } else if (a > 0) {
                // check for positive overflow with positive a, positive b
                if (a <= Long.MAX_VALUE / b) {
                    ret = a * b;
                } else {
                    throw new java.lang.ArithmeticException();
                }
            } else {
                // assert a == 0
                ret = 0;
            }
        }
        return ret;
    }

    /**
     * Raise an int to an int power.
     *
     * @param k Number to raise.
     * @param e Exponent (must be positive or zero).
     * @return \( k^e \)
     * @throws NotPositiveException if {@code e < 0}.
     * @throws MathArithmeticException if the result would overflow.
     */
    public static int pow(final int k, final int e) throws java.lang.IllegalArgumentException {
        if (e < 0) {
            throw new java.lang.IllegalArgumentException();
        }
        int exp = e;
        int result = 1;
        int k2p = k;
        while (true) {
            if ((exp & 0x1) != 0) {
                result = mulAndCheck(result, k2p);
            }
            exp >>= 1;
            if (exp == 0) {
                break;
            }
            k2p = mulAndCheck(k2p, k2p);
        }
        return result;
    }

    /**
     * Compute the base 10 logarithm.
     * @param x a number
     * @return log10(x)
     */
    public static double log10(final double x) {
        final double[] hiPrec = new double[2];
        final double lores = log(x, hiPrec);
        if (Double.isInfinite(lores)) {
            // don't allow this to be converted to NaN
            return lores;
        }
        final double tmp = hiPrec[0] * HEX_40000000;
        final double lna = hiPrec[0] + tmp - tmp;
        final double lnb = hiPrec[0] - lna + hiPrec[1];
        final double rln10a = 0.4342944622039795;
        final double rln10b = 1.9699272335463627E-8;
        return rln10b * lnb + rln10b * lna + rln10a * lnb + rln10a * lna;
    }

    /**
     * Internal helper method for natural logarithm function.
     * @param x original argument of the natural logarithm function
     * @param hiPrec extra bits of precision on output (To Be Confirmed)
     * @return log(x)
     */
    private static double log(final double x, final double[] hiPrec) {
        if (x == 0) {
            // Handle special case of +0/-0
            return Double.NEGATIVE_INFINITY;
        }
        long bits = Double.doubleToRawLongBits(x);
        /* Handle special cases of negative input, and NaN */
        if (((bits & 0x8000000000000000L) != 0 || x != x) && x != 0.0) {
            if (hiPrec != null) {
                hiPrec[0] = Double.NaN;
            }
            return Double.NaN;
        }
        /* Handle special cases of Positive infinity. */
        if (x == Double.POSITIVE_INFINITY) {
            if (hiPrec != null) {
                hiPrec[0] = Double.POSITIVE_INFINITY;
            }
            return Double.POSITIVE_INFINITY;
        }
        /* Extract the exponent */
        int exp = (int) (bits >> 52) - 1023;
        if ((bits & 0x7ff0000000000000L) == 0) {
            // Subnormal!
            if (x == 0) {
                // Zero
                if (hiPrec != null) {
                    hiPrec[0] = Double.NEGATIVE_INFINITY;
                }
                return Double.NEGATIVE_INFINITY;
            }
            /* Normalize the subnormal number. */
            bits <<= 1;
            while ((bits & 0x0010000000000000L) == 0) {
                --exp;
                bits <<= 1;
            }
        }
        if ((exp == -1 || exp == 0) && x < 1.01 && x > 0.99 && hiPrec == null) {
            /* The normal method doesn't work well in the range [0.99, 1.01], so call do a straight
           polynomial expansion in higer precision. */
            /* Compute x - 1.0 and split it */
            double xa = x - 1.0;
            double xb = xa - x + 1.0;
            double tmp = xa * HEX_40000000;
            double aa = xa + tmp - tmp;
            double ab = xa - aa;
            xa = aa;
            xb = ab;
            final double[] lnCoef_last = LN_QUICK_COEF[LN_QUICK_COEF.length - 1];
            double ya = lnCoef_last[0];
            double yb = lnCoef_last[1];
            for (int i = LN_QUICK_COEF.length - 2; i >= 0; i--) {
                /* Multiply a = y * x */
                aa = ya * xa;
                ab = ya * xb + yb * xa + yb * xb;
                /* split, so now y = a */
                tmp = aa * HEX_40000000;
                ya = aa + tmp - tmp;
                yb = aa - ya + ab;
                /* Add  a = y + lnQuickCoef */
                final double[] lnCoef_i = LN_QUICK_COEF[i];
                aa = ya + lnCoef_i[0];
                ab = yb + lnCoef_i[1];
                /* Split y = a */
                tmp = aa * HEX_40000000;
                ya = aa + tmp - tmp;
                yb = aa - ya + ab;
            }
            /* Multiply a = y * x */
            aa = ya * xa;
            ab = ya * xb + yb * xa + yb * xb;
            /* split, so now y = a */
            tmp = aa * HEX_40000000;
            ya = aa + tmp - tmp;
            yb = aa - ya + ab;
            return ya + yb;
        }
        // lnm is a log of a number in the range of 1.0 - 2.0, so 0 <= lnm < ln(2)
        final double[] lnm = LN_MANT[(int) ((bits & 0x000ffc0000000000L) >> 42)];
        /*
    double epsilon = x / Double.longBitsToDouble(bits & 0xfffffc0000000000L);

    epsilon -= 1.0;
         */
        // y is the most significant 10 bits of the mantissa
        // double y = Double.longBitsToDouble(bits & 0xfffffc0000000000L);
        // double epsilon = (x - y) / y;
        final double epsilon = (bits & 0x3ffffffffffL) / (TWO_POWER_52 + (bits & 0x000ffc0000000000L));
        double lnza = 0.0;
        double lnzb = 0.0;
        if (hiPrec != null) {
            /* split epsilon -> x */
            double tmp = epsilon * HEX_40000000;
            double aa = epsilon + tmp - tmp;
            double ab = epsilon - aa;
            double xa = aa;
            double xb = ab;
            /* Need a more accurate epsilon, so adjust the division. */
            final double numer = bits & 0x3ffffffffffL;
            final double denom = TWO_POWER_52 + (bits & 0x000ffc0000000000L);
            aa = numer - xa * denom - xb * denom;
            xb += aa / denom;
            /* Remez polynomial evaluation */
            final double[] lnCoef_last = LN_HI_PREC_COEF[LN_HI_PREC_COEF.length - 1];
            double ya = lnCoef_last[0];
            double yb = lnCoef_last[1];
            for (int i = LN_HI_PREC_COEF.length - 2; i >= 0; i--) {
                /* Multiply a = y * x */
                aa = ya * xa;
                ab = ya * xb + yb * xa + yb * xb;
                /* split, so now y = a */
                tmp = aa * HEX_40000000;
                ya = aa + tmp - tmp;
                yb = aa - ya + ab;
                /* Add  a = y + lnHiPrecCoef */
                final double[] lnCoef_i = LN_HI_PREC_COEF[i];
                aa = ya + lnCoef_i[0];
                ab = yb + lnCoef_i[1];
                /* Split y = a */
                tmp = aa * HEX_40000000;
                ya = aa + tmp - tmp;
                yb = aa - ya + ab;
            }
            /* Multiply a = y * x */
            aa = ya * xa;
            ab = ya * xb + yb * xa + yb * xb;
            /* split, so now lnz = a */
            /*
      tmp = aa * 1073741824.0;
      lnza = aa + tmp - tmp;
      lnzb = aa - lnza + ab;
             */
            lnza = aa + ab;
            lnzb = -(lnza - aa - ab);
        } else {
            /* High precision not required.  Eval Remez polynomial
         using standard double precision */
            lnza = -0.16624882440418567;
            lnza = lnza * epsilon + 0.19999954120254515;
            lnza = lnza * epsilon + -0.2499999997677497;
            lnza = lnza * epsilon + 0.3333333333332802;
            lnza = lnza * epsilon + -0.5;
            lnza = lnza * epsilon + 1.0;
            lnza *= epsilon;
        }
        /* Relative sizes:
         * lnzb     [0, 2.33E-10]
         * lnm[1]   [0, 1.17E-7]
         * ln2B*exp [0, 1.12E-4]
         * lnza      [0, 9.7E-4]
         * lnm[0]   [0, 0.692]
         * ln2A*exp [0, 709]
         */
        /* Compute the following sum:
         * lnzb + lnm[1] + ln2B*exp + lnza + lnm[0] + ln2A*exp;
         */
        // return lnzb + lnm[1] + ln2B*exp + lnza + lnm[0] + ln2A*exp;
        double a = LN_2_A * exp;
        double b = 0.0;
        double c = a + lnm[0];
        double d = -(c - a - lnm[0]);
        a = c;
        b += d;
        c = a + lnza;
        d = -(c - a - lnza);
        a = c;
        b += d;
        c = a + LN_2_B * exp;
        d = -(c - a - LN_2_B * exp);
        a = c;
        b += d;
        c = a + lnm[1];
        d = -(c - a - lnm[1]);
        a = c;
        b += d;
        c = a + lnzb;
        d = -(c - a - lnzb);
        a = c;
        b += d;
        if (hiPrec != null) {
            hiPrec[0] = a;
            hiPrec[1] = b;
        }
        return a + b;
    }

    /**
     *  Compute sine over the first quadrant (0 < x < pi/2).
     *  Use combination of table lookup and rational polynomial expansion.
     *  @param xa number from which sine is requested
     *  @param xb extra bits for x (may be 0.0)
     *  @return sin(xa + xb)
     */
    private static double sinQ(double xa, double xb) {
        int idx = (int) ((xa * 8.0) + 0.5);
        // idx*0.125;
        final double epsilon = xa - EIGHTHS[idx];
        // Table lookups
        final double sintA = SINE_TABLE_A[idx];
        final double sintB = SINE_TABLE_B[idx];
        final double costA = COSINE_TABLE_A[idx];
        final double costB = COSINE_TABLE_B[idx];
        // Polynomial eval of sin(epsilon), cos(epsilon)
        double sinEpsA = epsilon;
        double sinEpsB = polySine(epsilon);
        final double cosEpsA = 1.0;
        final double cosEpsB = polyCosine(epsilon);
        // Split epsilon   xa + xb = x
        final double temp = sinEpsA * HEX_40000000;
        double temp2 = (sinEpsA + temp) - temp;
        sinEpsB += sinEpsA - temp2;
        sinEpsA = temp2;
        /* Compute sin(x) by angle addition formula */
        double result;
        /* Compute the following sum:
         *
         * result = sintA + costA*sinEpsA + sintA*cosEpsB + costA*sinEpsB +
         *          sintB + costB*sinEpsA + sintB*cosEpsB + costB*sinEpsB;
         *
         * Ranges of elements
         *
         * xxxtA   0            PI/2
         * xxxtB   -1.5e-9      1.5e-9
         * sinEpsA -0.0625      0.0625
         * sinEpsB -6e-11       6e-11
         * cosEpsA  1.0
         * cosEpsB  0           -0.0625
         *
         */
        // result = sintA + costA*sinEpsA + sintA*cosEpsB + costA*sinEpsB +
        // sintB + costB*sinEpsA + sintB*cosEpsB + costB*sinEpsB;
        // result = sintA + sintA*cosEpsB + sintB + sintB * cosEpsB;
        // result += costA*sinEpsA + costA*sinEpsB + costB*sinEpsA + costB * sinEpsB;
        double a = 0;
        double b = 0;
        double t = sintA;
        double c = a + t;
        double d = -(c - a - t);
        a = c;
        b += d;
        t = costA * sinEpsA;
        c = a + t;
        d = -(c - a - t);
        a = c;
        b += d;
        b = b + sintA * cosEpsB + costA * sinEpsB;
        /*
    t = sintA*cosEpsB;
    c = a + t;
    d = -(c - a - t);
    a = c;
    b = b + d;

    t = costA*sinEpsB;
    c = a + t;
    d = -(c - a - t);
    a = c;
    b = b + d;
         */
        b = b + sintB + costB * sinEpsA + sintB * cosEpsB + costB * sinEpsB;
        if (xb != 0.0) {
            // approximate cosine*xb
            t = ((costA + costB) * (cosEpsA + cosEpsB) - (sintA + sintB) * (sinEpsA + sinEpsB)) * xb;
            c = a + t;
            d = -(c - a - t);
            a = c;
            b += d;
        }
        result = a + b;
        return result;
    }

    /**
     * Compute cosine in the first quadrant by subtracting input from PI/2 and
     * then calling sinQ.  This is more accurate as the input approaches PI/2.
     *  @param xa number from which cosine is requested
     *  @param xb extra bits for x (may be 0.0)
     *  @return cos(xa + xb)
     */
    private static double cosQ(double xa, double xb) {
        final double pi2a = 1.5707963267948966;
        final double pi2b = 6.123233995736766E-17;
        final double a = pi2a - xa;
        double b = -(a - pi2a + xa);
        b += pi2b - xb;
        return sinQ(a, b);
    }

    /**
     * Sine function.
     *
     * @param x Argument.
     * @return sin(x)
     */
    public static double sin(double x) {
        boolean negative = false;
        int quadrant = 0;
        double xa;
        double xb = 0.0;
        /* Take absolute value of the input */
        xa = x;
        if (x < 0) {
            negative = true;
            xa = -xa;
        }
        /* Check for zero and negative zero */
        if (xa == 0.0) {
            long bits = Double.doubleToRawLongBits(x);
            if (bits < 0) {
                return -0.0;
            }
            return 0.0;
        }
        if (xa != xa || xa == Double.POSITIVE_INFINITY) {
            return Double.NaN;
        }
        /* Perform any argument reduction */
        if (xa > 3294198.0) {
            // PI * (2**20)
            // Argument too big for CodyWaite reduction.  Must use
            // PayneHanek.
            double[] reduceResults = new double[3];
            reducePayneHanek(xa, reduceResults);
            quadrant = ((int) reduceResults[0]) & 3;
            xa = reduceResults[1];
            xb = reduceResults[2];
        } else if (xa > 1.5707963267948966) {
            final CodyWaite cw = new CodyWaite(xa);
            quadrant = cw.getK() & 3;
            xa = cw.getRemA();
            xb = cw.getRemB();
        }
        if (negative) {
            // Flip bit 1
            quadrant ^= 2;
        }
        switch(quadrant) {
            case 0:
                return sinQ(xa, xb);
            case 1:
                return cosQ(xa, xb);
            case 2:
                return -sinQ(xa, xb);
            case 3:
                return -cosQ(xa, xb);
            default:
                return Double.NaN;
        }
    }

    /**
     * Tangent function.
     *
     * @param x Argument.
     * @return tan(x)
     */
    public static double tan(double x) {
        boolean negative = false;
        int quadrant = 0;
        /* Take absolute value of the input */
        double xa = x;
        if (x < 0) {
            negative = true;
            xa = -xa;
        }
        /* Check for zero and negative zero */
        if (xa == 0.0) {
            long bits = Double.doubleToRawLongBits(x);
            if (bits < 0) {
                return -0.0;
            }
            return 0.0;
        }
        if (xa != xa || xa == Double.POSITIVE_INFINITY) {
            return Double.NaN;
        }
        /* Perform any argument reduction */
        double xb = 0;
        if (xa > 3294198.0) {
            // PI * (2**20)
            // Argument too big for CodyWaite reduction.  Must use
            // PayneHanek.
            double[] reduceResults = new double[3];
            reducePayneHanek(xa, reduceResults);
            quadrant = ((int) reduceResults[0]) & 3;
            xa = reduceResults[1];
            xb = reduceResults[2];
        } else if (xa > 1.5707963267948966) {
            final CodyWaite cw = new CodyWaite(xa);
            quadrant = cw.getK() & 3;
            xa = cw.getRemA();
            xb = cw.getRemB();
        }
        if (xa > 1.5) {
            // Accuracy suffers between 1.5 and PI/2
            final double pi2a = 1.5707963267948966;
            final double pi2b = 6.123233995736766E-17;
            final double a = pi2a - xa;
            double b = -(a - pi2a + xa);
            b += pi2b - xb;
            xa = a + b;
            xb = -(xa - a - b);
            quadrant ^= 1;
            negative ^= true;
        }
        double result;
        if ((quadrant & 1) == 0) {
            result = tanQ(xa, xb, false);
        } else {
            result = -tanQ(xa, xb, true);
        }
        if (negative) {
            result = -result;
        }
        return result;
    }

    /**
     * Two arguments arctangent function
     * @param y ordinate
     * @param x abscissa
     * @return phase angle of point (x,y) between {@code -PI} and {@code PI}
     */
    public static double atan2(double y, double x) {
        if (x != x || y != y) {
            return Double.NaN;
        }
        if (y == 0) {
            final double result = x * y;
            final double invx = 1d / x;
            final double invy = 1d / y;
            if (invx == 0) {
                // X is infinite
                if (x > 0) {
                    // return +/- 0.0
                    return y;
                } else {
                    return copySign(Math.PI, y);
                }
            }
            if (x < 0 || invx < 0) {
                if (y < 0 || invy < 0) {
                    return -Math.PI;
                } else {
                    return Math.PI;
                }
            } else {
                return result;
            }
        }
        if (y == Double.POSITIVE_INFINITY) {
            if (x == Double.POSITIVE_INFINITY) {
                return Math.PI * F_1_4;
            }
            if (x == Double.NEGATIVE_INFINITY) {
                return Math.PI * F_3_4;
            }
            return Math.PI * F_1_2;
        }
        if (y == Double.NEGATIVE_INFINITY) {
            if (x == Double.POSITIVE_INFINITY) {
                return -Math.PI * F_1_4;
            }
            if (x == Double.NEGATIVE_INFINITY) {
                return -Math.PI * F_3_4;
            }
            return -Math.PI * F_1_2;
        }
        if (x == Double.POSITIVE_INFINITY) {
            if (y > 0 || 1 / y > 0) {
                return 0d;
            }
            if (y < 0 || 1 / y < 0) {
                return -0d;
            }
        }
        if (x == Double.NEGATIVE_INFINITY) {
            if (y > 0.0 || 1 / y > 0.0) {
                return Math.PI;
            }
            if (y < 0 || 1 / y < 0) {
                return -Math.PI;
            }
        }
        if (x == 0) {
            if (y > 0 || 1 / y > 0) {
                return Math.PI * F_1_2;
            }
            if (y < 0 || 1 / y < 0) {
                return -Math.PI * F_1_2;
            }
        }
        // Compute ratio r = y/x
        final double r = y / x;
        if (Double.isInfinite(r)) {
            // bypass calculations that can create NaN
            return atan(r, 0, x < 0);
        }
        double ra = doubleHighPart(r);
        double rb = r - ra;
        // Split x
        final double xa = doubleHighPart(x);
        final double xb = x - xa;
        rb += (y - ra * xa - ra * xb - rb * xa - rb * xb) / x;
        final double temp = ra + rb;
        rb = -(temp - ra - rb);
        ra = temp;
        if (ra == 0) {
            // Fix up the sign so atan works correctly
            ra = copySign(0d, y);
        }
        // Call atan
        final double result = atan(ra, rb, x < 0);
        return result;
    }

    /**
     * Reduce the input argument using the Payne and Hanek method.
     *  This is good for all inputs 0.0 < x < inf
     *  Output is remainder after dividing by PI/2
     *  The result array should contain 3 numbers.
     *  result[0] is the integer portion, so mod 4 this gives the quadrant.
     *  result[1] is the upper bits of the remainder
     *  result[2] is the lower bits of the remainder
     *
     * @param x number to reduce
     * @param result placeholder where to put the result
     */
    private static void reducePayneHanek(double x, double[] result) {
        /* Convert input double to bits */
        long inbits = Double.doubleToRawLongBits(x);
        int exponent = (int) ((inbits >> 52) & 0x7ff) - 1023;
        /* Convert to fixed point representation */
        inbits &= 0x000fffffffffffffL;
        inbits |= 0x0010000000000000L;
        /* Normalize input to be between 0.5 and 1.0 */
        exponent++;
        inbits <<= 11;
        /* Based on the exponent, get a shifted copy of recip2pi */
        long shpi0;
        long shpiA;
        long shpiB;
        int idx = exponent >> 6;
        int shift = exponent - (idx << 6);
        if (shift != 0) {
            shpi0 = (idx == 0) ? 0 : (RECIP_2PI[idx - 1] << shift);
            shpi0 |= RECIP_2PI[idx] >>> (64 - shift);
            shpiA = (RECIP_2PI[idx] << shift) | (RECIP_2PI[idx + 1] >>> (64 - shift));
            shpiB = (RECIP_2PI[idx + 1] << shift) | (RECIP_2PI[idx + 2] >>> (64 - shift));
        } else {
            shpi0 = (idx == 0) ? 0 : RECIP_2PI[idx - 1];
            shpiA = RECIP_2PI[idx];
            shpiB = RECIP_2PI[idx + 1];
        }
        /* Multiply input by shpiA */
        long a = inbits >>> 32;
        long b = inbits & 0xffffffffL;
        long c = shpiA >>> 32;
        long d = shpiA & 0xffffffffL;
        long ac = a * c;
        long bd = b * d;
        long bc = b * c;
        long ad = a * d;
        long prodB = bd + (ad << 32);
        long prodA = ac + (ad >>> 32);
        boolean bita = (bd & 0x8000000000000000L) != 0;
        boolean bitb = (ad & 0x80000000L) != 0;
        boolean bitsum = (prodB & 0x8000000000000000L) != 0;
        /* Carry */
        if ((bita && bitb) || ((bita || bitb) && !bitsum)) {
            prodA++;
        }
        bita = (prodB & 0x8000000000000000L) != 0;
        bitb = (bc & 0x80000000L) != 0;
        prodB += bc << 32;
        prodA += bc >>> 32;
        bitsum = (prodB & 0x8000000000000000L) != 0;
        /* Carry */
        if ((bita && bitb) || ((bita || bitb) && !bitsum)) {
            prodA++;
        }
        /* Multiply input by shpiB */
        c = shpiB >>> 32;
        d = shpiB & 0xffffffffL;
        ac = a * c;
        bc = b * c;
        ad = a * d;
        /* Collect terms */
        ac += (bc + ad) >>> 32;
        bita = (prodB & 0x8000000000000000L) != 0;
        bitb = (ac & 0x8000000000000000L) != 0;
        prodB += ac;
        bitsum = (prodB & 0x8000000000000000L) != 0;
        /* Carry */
        if ((bita && bitb) || ((bita || bitb) && !bitsum)) {
            prodA++;
        }
        /* Multiply by shpi0 */
        c = shpi0 >>> 32;
        d = shpi0 & 0xffffffffL;
        bd = b * d;
        bc = b * c;
        ad = a * d;
        prodA += bd + ((bc + ad) << 32);
        /*
         * prodA, prodB now contain the remainder as a fraction of PI.  We want this as a fraction of
         * PI/2, so use the following steps:
         * 1.) multiply by 4.
         * 2.) do a fixed point muliply by PI/4.
         * 3.) Convert to floating point.
         * 4.) Multiply by 2
         */
        /* This identifies the quadrant */
        int intPart = (int) (prodA >>> 62);
        /* Multiply by 4 */
        prodA <<= 2;
        prodA |= prodB >>> 62;
        prodB <<= 2;
        /* Multiply by PI/4 */
        a = prodA >>> 32;
        b = prodA & 0xffffffffL;
        c = PI_O_4_BITS[0] >>> 32;
        d = PI_O_4_BITS[0] & 0xffffffffL;
        ac = a * c;
        bd = b * d;
        bc = b * c;
        ad = a * d;
        long prod2B = bd + (ad << 32);
        long prod2A = ac + (ad >>> 32);
        bita = (bd & 0x8000000000000000L) != 0;
        bitb = (ad & 0x80000000L) != 0;
        bitsum = (prod2B & 0x8000000000000000L) != 0;
        /* Carry */
        if ((bita && bitb) || ((bita || bitb) && !bitsum)) {
            prod2A++;
        }
        bita = (prod2B & 0x8000000000000000L) != 0;
        bitb = (bc & 0x80000000L) != 0;
        prod2B += bc << 32;
        prod2A += bc >>> 32;
        bitsum = (prod2B & 0x8000000000000000L) != 0;
        /* Carry */
        if ((bita && bitb) || ((bita || bitb) && !bitsum)) {
            prod2A++;
        }
        /* Multiply input by pio4bits[1] */
        c = PI_O_4_BITS[1] >>> 32;
        d = PI_O_4_BITS[1] & 0xffffffffL;
        ac = a * c;
        bc = b * c;
        ad = a * d;
        /* Collect terms */
        ac += (bc + ad) >>> 32;
        bita = (prod2B & 0x8000000000000000L) != 0;
        bitb = (ac & 0x8000000000000000L) != 0;
        prod2B += ac;
        bitsum = (prod2B & 0x8000000000000000L) != 0;
        /* Carry */
        if ((bita && bitb) || ((bita || bitb) && !bitsum)) {
            prod2A++;
        }
        /* Multiply inputB by pio4bits[0] */
        a = prodB >>> 32;
        b = prodB & 0xffffffffL;
        c = PI_O_4_BITS[0] >>> 32;
        d = PI_O_4_BITS[0] & 0xffffffffL;
        ac = a * c;
        bc = b * c;
        ad = a * d;
        /* Collect terms */
        ac += (bc + ad) >>> 32;
        bita = (prod2B & 0x8000000000000000L) != 0;
        bitb = (ac & 0x8000000000000000L) != 0;
        prod2B += ac;
        bitsum = (prod2B & 0x8000000000000000L) != 0;
        /* Carry */
        if ((bita && bitb) || ((bita || bitb) && !bitsum)) {
            prod2A++;
        }
        /* Convert to double */
        // High order 52 bits
        double tmpA = (prod2A >>> 12) / TWO_POWER_52;
        // Low bits
        double tmpB = (((prod2A & 0xfffL) << 40) + (prod2B >>> 24)) / TWO_POWER_52 / TWO_POWER_52;
        double sumA = tmpA + tmpB;
        double sumB = -(sumA - tmpA - tmpB);
        /* Multiply by PI/2 and return */
        result[0] = intPart;
        result[1] = sumA * 2.0;
        result[2] = sumB * 2.0;
    }

    /**
     * Returns the first argument with the sign of the second argument.
     * A NaN {@code sign} argument is treated as positive.
     *
     * @param magnitude the value to return
     * @param sign the sign for the returned value
     * @return the magnitude with the same sign as the {@code sign} argument
     */
    public static double copySign(double magnitude, double sign) {
        // The highest order bit is going to be zero if the
        // highest order bit of m and s is the same and one otherwise.
        // So (m^s) will be positive if both m and s have the same sign
        // and negative otherwise.
        // don't care about NaN
        final long m = Double.doubleToRawLongBits(magnitude);
        final long s = Double.doubleToRawLongBits(sign);
        if ((m ^ s) >= 0) {
            return magnitude;
        }
        // flip sign
        return -magnitude;
    }

    /**
     *  Compute tangent (or cotangent) over the first quadrant.   0 < x < pi/2
     *  Use combination of table lookup and rational polynomial expansion.
     *  @param xa number from which sine is requested
     *  @param xb extra bits for x (may be 0.0)
     *  @param cotanFlag if true, compute the cotangent instead of the tangent
     *  @return tan(xa+xb) (or cotangent, depending on cotanFlag)
     */
    private static double tanQ(double xa, double xb, boolean cotanFlag) {
        int idx = (int) ((xa * 8.0) + 0.5);
        // idx*0.125;
        final double epsilon = xa - EIGHTHS[idx];
        // Table lookups
        final double sintA = SINE_TABLE_A[idx];
        final double sintB = SINE_TABLE_B[idx];
        final double costA = COSINE_TABLE_A[idx];
        final double costB = COSINE_TABLE_B[idx];
        // Polynomial eval of sin(epsilon), cos(epsilon)
        double sinEpsA = epsilon;
        double sinEpsB = polySine(epsilon);
        final double cosEpsA = 1.0;
        final double cosEpsB = polyCosine(epsilon);
        // Split epsilon   xa + xb = x
        double temp = sinEpsA * HEX_40000000;
        double temp2 = (sinEpsA + temp) - temp;
        sinEpsB += sinEpsA - temp2;
        sinEpsA = temp2;
        /* Compute sin(x) by angle addition formula */
        /* Compute the following sum:
         *
         * result = sintA + costA*sinEpsA + sintA*cosEpsB + costA*sinEpsB +
         *          sintB + costB*sinEpsA + sintB*cosEpsB + costB*sinEpsB;
         *
         * Ranges of elements
         *
         * xxxtA   0            PI/2
         * xxxtB   -1.5e-9      1.5e-9
         * sinEpsA -0.0625      0.0625
         * sinEpsB -6e-11       6e-11
         * cosEpsA  1.0
         * cosEpsB  0           -0.0625
         *
         */
        // result = sintA + costA*sinEpsA + sintA*cosEpsB + costA*sinEpsB +
        // sintB + costB*sinEpsA + sintB*cosEpsB + costB*sinEpsB;
        // result = sintA + sintA*cosEpsB + sintB + sintB * cosEpsB;
        // result += costA*sinEpsA + costA*sinEpsB + costB*sinEpsA + costB * sinEpsB;
        double a = 0;
        double b = 0;
        // Compute sine
        double t = sintA;
        double c = a + t;
        double d = -(c - a - t);
        a = c;
        b += d;
        t = costA * sinEpsA;
        c = a + t;
        d = -(c - a - t);
        a = c;
        b += d;
        b += sintA * cosEpsB + costA * sinEpsB;
        b += sintB + costB * sinEpsA + sintB * cosEpsB + costB * sinEpsB;
        double sina = a + b;
        double sinb = -(sina - a - b);
        // Compute cosine
        a = b = c = d = 0.0;
        t = costA * cosEpsA;
        c = a + t;
        d = -(c - a - t);
        a = c;
        b += d;
        t = -sintA * sinEpsA;
        c = a + t;
        d = -(c - a - t);
        a = c;
        b += d;
        b += costB * cosEpsA + costA * cosEpsB + costB * cosEpsB;
        b -= sintB * sinEpsA + sintA * sinEpsB + sintB * sinEpsB;
        double cosa = a + b;
        double cosb = -(cosa - a - b);
        if (cotanFlag) {
            double tmp;
            tmp = cosa;
            cosa = sina;
            sina = tmp;
            tmp = cosb;
            cosb = sinb;
            sinb = tmp;
        }
        /* estimate and correct, compute 1.0/(cosa+cosb) */
        /*
    double est = (sina+sinb)/(cosa+cosb);
    double err = (sina - cosa*est) + (sinb - cosb*est);
    est += err/(cosa+cosb);
    err = (sina - cosa*est) + (sinb - cosb*est);
         */
        // f(x) = 1/x,   f'(x) = -1/x^2
        double est = sina / cosa;
        /* Split the estimate to get more accurate read on division rounding */
        temp = est * HEX_40000000;
        double esta = (est + temp) - temp;
        double estb = est - esta;
        temp = cosa * HEX_40000000;
        double cosaa = (cosa + temp) - temp;
        double cosab = cosa - cosaa;
        // double err = (sina - est*cosa)/cosa;  // Correction for division rounding
        // Correction for division rounding
        double err = (sina - esta * cosaa - esta * cosab - estb * cosaa - estb * cosab) / cosa;
        // Change in est due to sinb
        err += sinb / cosa;
        // Change in est due to cosb
        err += -sina * cosb / cosa / cosa;
        if (xb != 0.0) {
            // tan' = 1 + tan^2      cot' = -(1 + cot^2)
            // Approximate impact of xb
            double xbadj = xb + est * est * xb;
            if (cotanFlag) {
                xbadj = -xbadj;
            }
            err += xbadj;
        }
        return est + err;
    }

    /**
     *  Computes sin(x) - x, where |x| < 1/16.
     *  Use a Remez polynomial approximation.
     *  @param x a number smaller than 1/16
     *  @return sin(x) - x
     */
    private static double polySine(final double x) {
        double x2 = x * x;
        double p = 2.7553817452272217E-6;
        p = p * x2 + -1.9841269659586505E-4;
        p = p * x2 + 0.008333333333329196;
        p = p * x2 + -0.16666666666666666;
        // p *= x2;
        // p *= x;
        p = p * x2 * x;
        return p;
    }

    /**
     *  Computes cos(x) - 1, where |x| < 1/16.
     *  Use a Remez polynomial approximation.
     *  @param x a number smaller than 1/16
     *  @return cos(x) - 1
     */
    private static double polyCosine(double x) {
        double x2 = x * x;
        double p = 2.479773539153719E-5;
        p = p * x2 + -0.0013888888689039883;
        p = p * x2 + 0.041666666666621166;
        p = p * x2 + -0.49999999999999994;
        p *= x2;
        return p;
    }

    /**
     * Internal helper function to compute arctangent.
     * @param xa number from which arctangent is requested
     * @param xb extra bits for x (may be 0.0)
     * @param leftPlane if true, result angle must be put in the left half plane
     * @return atan(xa + xb) (or angle shifted by {@code PI} if leftPlane is true)
     */
    private static double atan(double xa, double xb, boolean leftPlane) {
        if (xa == 0.0) {
            // Matches +/- 0.0; return correct sign
            return leftPlane ? copySign(Math.PI, xa) : xa;
        }
        final boolean negate;
        if (xa < 0) {
            // negative
            xa = -xa;
            xb = -xb;
            negate = true;
        } else {
            negate = false;
        }
        if (xa > 1.633123935319537E16) {
            // Very large input
            return (negate ^ leftPlane) ? (-Math.PI * F_1_2) : (Math.PI * F_1_2);
        }
        /* Estimate the closest tabulated arctan value, compute eps = xa-tangentTable */
        final int idx;
        if (xa < 1) {
            idx = (int) (((-1.7168146928204136 * xa * xa + 8.0) * xa) + 0.5);
        } else {
            final double oneOverXa = 1 / xa;
            idx = (int) (-((-1.7168146928204136 * oneOverXa * oneOverXa + 8.0) * oneOverXa) + 13.07);
        }
        final double ttA = TANGENT_TABLE_A[idx];
        final double ttB = TANGENT_TABLE_B[idx];
        double epsA = xa - ttA;
        double epsB = -(epsA - xa + ttA);
        epsB += xb - ttB;
        double temp = epsA + epsB;
        epsB = -(temp - epsA - epsB);
        epsA = temp;
        /* Compute eps = eps / (1.0 + xa*tangent) */
        temp = xa * HEX_40000000;
        double ya = xa + temp - temp;
        double yb = xb + xa - ya;
        xa = ya;
        xb += yb;
        // if (idx > 8 || idx == 0)
        if (idx == 0) {
            /* If the slope of the arctan is gentle enough (< 0.45), this approximation will suffice */
            // double denom = 1.0 / (1.0 + xa*tangentTableA[idx] + xb*tangentTableA[idx] + xa*tangentTableB[idx] + xb*tangentTableB[idx]);
            final double denom = 1d / (1d + (xa + xb) * (ttA + ttB));
            // double denom = 1.0 / (1.0 + xa*tangentTableA[idx]);
            ya = epsA * denom;
            yb = epsB * denom;
        } else {
            double temp2 = xa * ttA;
            double za = 1d + temp2;
            double zb = -(za - 1d - temp2);
            temp2 = xb * ttA + xa * ttB;
            temp = za + temp2;
            zb += -(temp - za - temp2);
            za = temp;
            zb += xb * ttB;
            ya = epsA / za;
            temp = ya * HEX_40000000;
            final double yaa = (ya + temp) - temp;
            final double yab = ya - yaa;
            temp = za * HEX_40000000;
            final double zaa = (za + temp) - temp;
            final double zab = za - zaa;
            /* Correct for rounding in division */
            yb = (epsA - yaa * zaa - yaa * zab - yab * zaa - yab * zab) / za;
            yb += -epsA * zb / za / za;
            yb += epsB / za;
        }
        epsA = ya;
        epsB = yb;
        /* Evaluate polynomial */
        final double epsA2 = epsA * epsA;
        /*
    yb = -0.09001346640161823;
    yb = yb * epsA2 + 0.11110718400605211;
    yb = yb * epsA2 + -0.1428571349122913;
    yb = yb * epsA2 + 0.19999999999273194;
    yb = yb * epsA2 + -0.33333333333333093;
    yb = yb * epsA2 * epsA;
         */
        yb = 0.07490822288864472;
        yb = yb * epsA2 - 0.09088450866185192;
        yb = yb * epsA2 + 0.11111095942313305;
        yb = yb * epsA2 - 0.1428571423679182;
        yb = yb * epsA2 + 0.19999999999923582;
        yb = yb * epsA2 - 0.33333333333333287;
        yb = yb * epsA2 * epsA;
        ya = epsA;
        temp = ya + yb;
        yb = -(temp - ya - yb);
        ya = temp;
        /* Add in effect of epsB.   atan'(x) = 1/(1+x^2) */
        yb += epsB / (1d + epsA * epsA);
        final double eighths = EIGHTHS[idx];
        // result = yb + eighths[idx] + ya;
        double za = eighths + ya;
        double zb = -(za - eighths - ya);
        temp = za + yb;
        zb += -(temp - za - yb);
        za = temp;
        double result = za + zb;
        if (leftPlane) {
            // Result is in the left plane
            final double resultb = -(result - za - zb);
            final double pia = 1.5707963267948966 * 2;
            final double pib = 6.123233995736766E-17 * 2;
            za = pia - result;
            zb = -(za - pia + result);
            zb += pib - resultb;
            result = za + zb;
        }
        if (negate ^ leftPlane) {
            result = -result;
        }
        return result;
    }

    // Generic helper methods
    /**
     * Get the high order bits from the mantissa.
     * Equivalent to adding and subtracting HEX_40000 but also works for very large numbers
     *
     * @param d the value to split
     * @return the high order part of the mantissa
     */
    private static double doubleHighPart(double d) {
        if (d > -SAFE_MIN && d < SAFE_MIN) {
            // These are un-normalised - don't try to convert
            return d;
        }
        // can take raw bits because just gonna convert it back
        long xl = Double.doubleToRawLongBits(d);
        // Drop low order bits
        xl &= MASK_30BITS;
        return Double.longBitsToDouble(xl);
    }

    /**
     * Compute the square root of a number.
     * <p><b>Note:</b> this implementation currently delegates to {@link Math#sqrt}
     * @param a number on which evaluation is done
     * @return square root of a
     */
    public static double sqrt(final double a) {
        return Math.sqrt(a);
    }

    /**
     * Compute the arc cosine of a number.
     * @param x number on which evaluation is done
     * @return arc cosine of x
     */
    public static double acos(double x) {
        if (x != x) {
            return Double.NaN;
        }
        if (x > 1.0 || x < -1.0) {
            return Double.NaN;
        }
        if (x == -1.0) {
            return Math.PI;
        }
        if (x == 1.0) {
            return 0.0;
        }
        if (x == 0) {
            return Math.PI / 2.0;
        }
        /* Compute acos(x) = atan(sqrt(1-x*x)/x) */
        /* Split x */
        double temp = x * HEX_40000000;
        final double xa = x + temp - temp;
        final double xb = x - xa;
        /* Square it */
        double ya = xa * xa;
        double yb = xa * xb * 2.0 + xb * xb;
        /* Subtract from 1 */
        ya = -ya;
        yb = -yb;
        double za = 1.0 + ya;
        double zb = -(za - 1.0 - ya);
        temp = za + yb;
        zb += -(temp - za - yb);
        za = temp;
        /* Square root */
        double y = sqrt(za);
        temp = y * HEX_40000000;
        ya = y + temp - temp;
        yb = y - ya;
        /* Extend precision of sqrt */
        yb += (za - ya * ya - 2 * ya * yb - yb * yb) / (2.0 * y);
        /* Contribution of zb to sqrt */
        yb += zb / (2.0 * y);
        y = ya + yb;
        yb = -(y - ya - yb);
        // Compute ratio r = y/x
        double r = y / x;
        // Did r overflow?
        if (Double.isInfinite(r)) {
            // so return the appropriate value
            return Math.PI / 2;
        }
        double ra = doubleHighPart(r);
        double rb = r - ra;
        // Correct for rounding in division
        rb += (y - ra * xa - ra * xb - rb * xa - rb * xb) / x;
        // Add in effect additional bits of sqrt.
        rb += yb / x;
        temp = ra + rb;
        rb = -(temp - ra - rb);
        ra = temp;
        return atan(ra, rb, x < 0);
    }

    private static final double[] EXP_FRAC_TABLE_A = EXP_FRAC_A.clone();
    private static final double[] EXP_FRAC_TABLE_B = EXP_FRAC_B.clone();
    private static final double[] EXP_INT_TABLE_A = EXP_INT_A.clone();
    private static final double[] EXP_INT_TABLE_B = EXP_INT_B.clone();

    /**
     * Exponential function.
     *
     * Computes exp(x), function result is nearly rounded.   It will be correctly
     * rounded to the theoretical value for 99.9% of input values, otherwise it will
     * have a 1 ULP error.
     *
     * Method:
     *    Lookup intVal = exp(int(x))
     *    Lookup fracVal = exp(int(x-int(x) / 1024.0) * 1024.0 );
     *    Compute z as the exponential of the remaining bits by a polynomial minus one
     *    exp(x) = intVal * fracVal * (1 + z)
     *
     * Accuracy:
     *    Calculation is done with 63 bits of precision, so result should be correctly
     *    rounded for 99.9% of input values, with less than 1 ULP error otherwise.
     *
     * @param x   a double
     * @return double e<sup>x</sup>
     */
    public static double exp(double x) {
        return exp(x, 0.0, null);
    }

    /**
     * Internal helper method for exponential function.
     * @param x original argument of the exponential function
     * @param extra extra bits of precision on input (To Be Confirmed)
     * @param hiPrec extra bits of precision on output (To Be Confirmed)
     * @return exp(x)
     */
    private static double exp(double x, double extra, double[] hiPrec) {
        double intPartA;
        double intPartB;
        int intVal = (int) x;
        /* Lookup exp(floor(x)).
         * intPartA will have the upper 22 bits, intPartB will have the lower
         * 52 bits.
         */
        if (x < 0.0) {
            // may be affected by a JIT bug. Subsequent comparisons can safely use intVal
            if (x < -746d) {
                if (hiPrec != null) {
                    hiPrec[0] = 0.0;
                    hiPrec[1] = 0.0;
                }
                return 0.0;
            }
            if (intVal < -709) {
                /* This will produce a subnormal output */
                final double result = exp(x + 40.19140625, extra, hiPrec) / 285040095144011776.0;
                if (hiPrec != null) {
                    hiPrec[0] /= 285040095144011776.0;
                    hiPrec[1] /= 285040095144011776.0;
                }
                return result;
            }
            if (intVal == -709) {
                /* exp(1.494140625) is nearly a machine number... */
                final double result = exp(x + 1.494140625, extra, hiPrec) / 4.455505956692756620;
                if (hiPrec != null) {
                    hiPrec[0] /= 4.455505956692756620;
                    hiPrec[1] /= 4.455505956692756620;
                }
                return result;
            }
            intVal--;
        } else {
            if (intVal > 709) {
                if (hiPrec != null) {
                    hiPrec[0] = Double.POSITIVE_INFINITY;
                    hiPrec[1] = 0.0;
                }
                return Double.POSITIVE_INFINITY;
            }
        }
        intPartA = EXP_INT_TABLE_A[EXP_INT_TABLE_MAX_INDEX + intVal];
        intPartB = EXP_INT_TABLE_B[EXP_INT_TABLE_MAX_INDEX + intVal];
        /* Get the fractional part of x, find the greatest multiple of 2^-10 less than
         * x and look up the exp function of it.
         * fracPartA will have the upper 22 bits, fracPartB the lower 52 bits.
         */
        final int intFrac = (int) ((x - intVal) * 1024.0);
        final double fracPartA = EXP_FRAC_TABLE_A[intFrac];
        final double fracPartB = EXP_FRAC_TABLE_B[intFrac];
        /* epsilon is the difference in x from the nearest multiple of 2^-10.  It
         * has a value in the range 0 <= epsilon < 2^-10.
         * Do the subtraction from x as the last step to avoid possible loss of precision.
         */
        final double epsilon = x - (intVal + intFrac / 1024.0);
        /* Compute z = exp(epsilon) - 1.0 via a minimax polynomial.  z has
       full double precision (52 bits).  Since z < 2^-10, we will have
       62 bits of precision when combined with the constant 1.  This will be
       used in the last addition below to get proper rounding. */
        /* Remez generated polynomial.  Converges on the interval [0, 2^-10], error
       is less than 0.5 ULP */
        double z = 0.04168701738764507;
        z = z * epsilon + 0.1666666505023083;
        z = z * epsilon + 0.5000000000042687;
        z = z * epsilon + 1.0;
        z = z * epsilon + -3.940510424527919E-20;
        /* Compute (intPartA+intPartB) * (fracPartA+fracPartB) by binomial
       expansion.
       tempA is exact since intPartA and intPartB only have 22 bits each.
       tempB will have 52 bits of precision.
         */
        double tempA = intPartA * fracPartA;
        double tempB = intPartA * fracPartB + intPartB * fracPartA + intPartB * fracPartB;
        /* Compute the result.  (1+z)(tempA+tempB).  Order of operations is
       important.  For accuracy add by increasing size.  tempA is exact and
       much larger than the others.  If there are extra bits specified from the
       pow() function, use them. */
        final double tempC = tempB + tempA;
        // because z could be negative at the same time.
        if (tempC == Double.POSITIVE_INFINITY) {
            return Double.POSITIVE_INFINITY;
        }
        final double result;
        if (extra != 0.0) {
            result = tempC * extra * z + tempC * extra + tempC * z + tempB + tempA;
        } else {
            result = tempC * z + tempB + tempA;
        }
        if (hiPrec != null) {
            // If requesting high precision
            hiPrec[0] = tempA;
            hiPrec[1] = tempC * extra * z + tempC * extra + tempC * z + tempB;
        }
        return result;
    }

    /**
     * Compute exp(x) - 1
     * @param x number to compute shifted exponential
     * @return exp(x) - 1
     */
    public static double expm1(double x) {
        return expm1(x, null);
    }

    /**
     * Internal helper method for expm1
     * @param x number to compute shifted exponential
     * @param hiPrecOut receive high precision result for -1.0 < x < 1.0
     * @return exp(x) - 1
     */
    private static double expm1(double x, double[] hiPrecOut) {
        if (x != x || x == 0.0) {
            // NaN or zero
            return x;
        }
        if (x <= -1.0 || x >= 1.0) {
            // If not between +/- 1.0
            // return exp(x) - 1.0;
            double[] hiPrec = new double[2];
            exp(x, 0.0, hiPrec);
            if (x > 0.0) {
                return -1.0 + hiPrec[0] + hiPrec[1];
            } else {
                final double ra = -1.0 + hiPrec[0];
                double rb = -(ra + 1.0 - hiPrec[0]);
                rb += hiPrec[1];
                return ra + rb;
            }
        }
        double baseA;
        double baseB;
        double epsilon;
        boolean negative = false;
        if (x < 0.0) {
            x = -x;
            negative = true;
        }
        {
            int intFrac = (int) (x * 1024.0);
            double tempA = EXP_FRAC_TABLE_A[intFrac] - 1.0;
            double tempB = EXP_FRAC_TABLE_B[intFrac];
            double temp = tempA + tempB;
            tempB = -(temp - tempA - tempB);
            tempA = temp;
            temp = tempA * HEX_40000000;
            baseA = tempA + temp - temp;
            baseB = tempB + (tempA - baseA);
            epsilon = x - intFrac / 1024.0;
        }
        /* Compute expm1(epsilon) */
        double zb = 0.008336750013465571;
        zb = zb * epsilon + 0.041666663879186654;
        zb = zb * epsilon + 0.16666666666745392;
        zb = zb * epsilon + 0.49999999999999994;
        zb *= epsilon;
        zb *= epsilon;
        double za = epsilon;
        double temp = za + zb;
        zb = -(temp - za - zb);
        za = temp;
        temp = za * HEX_40000000;
        temp = za + temp - temp;
        zb += za - temp;
        za = temp;
        /* Combine the parts.   expm1(a+b) = expm1(a) + expm1(b) + expm1(a)*expm1(b) */
        double ya = za * baseA;
        // double yb = za*baseB + zb*baseA + zb*baseB;
        temp = ya + za * baseB;
        double yb = -(temp - ya - za * baseB);
        ya = temp;
        temp = ya + zb * baseA;
        yb += -(temp - ya - zb * baseA);
        ya = temp;
        temp = ya + zb * baseB;
        yb += -(temp - ya - zb * baseB);
        ya = temp;
        // ya = ya + za + baseA;
        // yb = yb + zb + baseB;
        temp = ya + baseA;
        yb += -(temp - baseA - ya);
        ya = temp;
        temp = ya + za;
        // yb += (ya > za) ? -(temp - ya - za) : -(temp - za - ya);
        yb += -(temp - ya - za);
        ya = temp;
        temp = ya + baseB;
        // yb += (ya > baseB) ? -(temp - ya - baseB) : -(temp - baseB - ya);
        yb += -(temp - ya - baseB);
        ya = temp;
        temp = ya + zb;
        // yb += (ya > zb) ? -(temp - ya - zb) : -(temp - zb - ya);
        yb += -(temp - ya - zb);
        ya = temp;
        if (negative) {
            /* Compute expm1(-x) = -expm1(x) / (expm1(x) + 1) */
            double denom = 1.0 + ya;
            double denomr = 1.0 / denom;
            double denomb = -(denom - 1.0 - ya) + yb;
            double ratio = ya * denomr;
            temp = ratio * HEX_40000000;
            final double ra = ratio + temp - temp;
            double rb = ratio - ra;
            temp = denom * HEX_40000000;
            za = denom + temp - temp;
            zb = denom - za;
            rb += (ya - za * ra - za * rb - zb * ra - zb * rb) * denomr;
            // f(x) = x/1+x
            // Compute f'(x)
            // Product rule:  d(uv) = du*v + u*dv
            // Chain rule:  d(f(g(x)) = f'(g(x))*f(g'(x))
            // d(1/x) = -1/(x*x)
            // d(1/1+x) = -1/( (1+x)^2) *  1 =  -1/((1+x)*(1+x))
            // d(x/1+x) = -x/((1+x)(1+x)) + 1/1+x = 1 / ((1+x)(1+x))
            // Adjust for yb
            // numerator
            rb += yb * denomr;
            // denominator
            rb += -ya * denomb * denomr * denomr;
            // negate
            ya = -ra;
            yb = -rb;
        }
        if (hiPrecOut != null) {
            hiPrecOut[0] = ya;
            hiPrecOut[1] = yb;
        }
        return ya + yb;
    }

    /**
     * Compute the hyperbolic sine of a number.
     * @param x number on which evaluation is done
     * @return hyperbolic sine of x
     */
    public static double sinh(double x) {
        boolean negate = false;
        if (x != x) {
            return x;
        }
        if (x > 20) {
            if (x >= LOG_MAX_VALUE) {
                // Avoid overflow (MATH-905).
                final double t = exp(0.5 * x);
                return (0.5 * t) * t;
            } else {
                return 0.5 * exp(x);
            }
        } else if (x < -20) {
            if (x <= -LOG_MAX_VALUE) {
                // Avoid overflow (MATH-905).
                final double t = exp(-0.5 * x);
                return (-0.5 * t) * t;
            } else {
                return -0.5 * exp(-x);
            }
        }
        if (x == 0) {
            return x;
        }
        if (x < 0.0) {
            x = -x;
            negate = true;
        }
        double result;
        if (x > 0.25) {
            double[] hiPrec = new double[2];
            exp(x, 0.0, hiPrec);
            double ya = hiPrec[0] + hiPrec[1];
            double yb = -(ya - hiPrec[0] - hiPrec[1]);
            double temp = ya * HEX_40000000;
            double yaa = ya + temp - temp;
            double yab = ya - yaa;
            // recip = 1/y
            double recip = 1.0 / ya;
            temp = recip * HEX_40000000;
            double recipa = recip + temp - temp;
            double recipb = recip - recipa;
            // Correct for rounding in division
            recipb += (1.0 - yaa * recipa - yaa * recipb - yab * recipa - yab * recipb) * recip;
            // Account for yb
            recipb += -yb * recip * recip;
            recipa = -recipa;
            recipb = -recipb;
            // y = y + 1/y
            temp = ya + recipa;
            yb += -(temp - ya - recipa);
            ya = temp;
            temp = ya + recipb;
            yb += -(temp - ya - recipb);
            ya = temp;
            result = ya + yb;
            result *= 0.5;
        } else {
            double[] hiPrec = new double[2];
            expm1(x, hiPrec);
            double ya = hiPrec[0] + hiPrec[1];
            double yb = -(ya - hiPrec[0] - hiPrec[1]);
            /* Compute expm1(-x) = -expm1(x) / (expm1(x) + 1) */
            double denom = 1.0 + ya;
            double denomr = 1.0 / denom;
            double denomb = -(denom - 1.0 - ya) + yb;
            double ratio = ya * denomr;
            double temp = ratio * HEX_40000000;
            double ra = ratio + temp - temp;
            double rb = ratio - ra;
            temp = denom * HEX_40000000;
            double za = denom + temp - temp;
            double zb = denom - za;
            rb += (ya - za * ra - za * rb - zb * ra - zb * rb) * denomr;
            // Adjust for yb
            // numerator
            rb += yb * denomr;
            // denominator
            rb += -ya * denomb * denomr * denomr;
            // y = y - 1/y
            temp = ya + ra;
            yb += -(temp - ya - ra);
            ya = temp;
            temp = ya + rb;
            yb += -(temp - ya - rb);
            ya = temp;
            result = ya + yb;
            result *= 0.5;
        }
        if (negate) {
            result = -result;
        }
        return result;
    }

    /**
     * All long-representable factorials
     */
    static final long[] FACTORIALS = new long[] { 1l, 1l, 2l, 6l, 24l, 120l, 720l, 5040l, 40320l, 362880l, 3628800l, 39916800l, 479001600l, 6227020800l, 87178291200l, 1307674368000l, 20922789888000l, 355687428096000l, 6402373705728000l, 121645100408832000l, 2432902008176640000l };

    /**
     * Stirling numbers of the second kind.
     */
    static final AtomicReference<long[][]> STIRLING_S2 = new AtomicReference<long[][]>(null);

    /**
     * Check binomial preconditions.
     *
     * @param n Size of the set.
     * @param k Size of the subsets to be counted.
     * @throws NotPositiveException if {@code n < 0}.
     * @throws NumberIsTooLargeException if {@code k > n}.
     */
    public static void checkBinomial(final int n, final int k) throws java.lang.IllegalArgumentException, java.lang.ArithmeticException {
        if (n < k) {
            throw new java.lang.IllegalArgumentException();
        }
        if (n < 0) {
            throw new java.lang.ArithmeticException();
        }
    }

    /**
     * Returns an exact representation of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "{@code n choose k}", the number of
     * {@code k}-element subsets that can be selected from an
     * {@code n}-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> {@code 0 <= k <= n } (otherwise
     * {@code MathIllegalArgumentException} is thrown)</li>
     * <li> The result is small enough to fit into a {@code long}. The
     * largest value of {@code n} for which all coefficients are
     * {@code  < Long.MAX_VALUE} is 66. If the computed value exceeds
     * {@code Long.MAX_VALUE} a {@code MathArithMeticException} is
     * thrown.</li>
     * </ul></p>
     *
     * @param n the size of the set
     * @param k the size of the subsets to be counted
     * @return {@code n choose k}
     * @throws NotPositiveException if {@code n < 0}.
     * @throws NumberIsTooLargeException if {@code k > n}.
     * @throws MathArithmeticException if the result is too large to be
     * represented by a long integer.
     */
    public static long binomialCoefficient(final int n, final int k) throws java.lang.IllegalArgumentException, java.lang.ArithmeticException {
        checkBinomial(n, k);
        if ((n == k) || (k == 0)) {
            return 1;
        }
        if ((k == 1) || (k == n - 1)) {
            return n;
        }
        // Use symmetry for large k
        if (k > n / 2) {
            return binomialCoefficient(n, n - k);
        }
        // We use the formula
        // (n choose k) = n! / (n-k)! / k!
        // (n choose k) == ((n-k+1)*...*n) / (1*...*k)
        // which could be written
        // (n choose k) == (n-1 choose k-1) * n / k
        long result = 1;
        if (n <= 61) {
            // For n <= 61, the naive implementation cannot overflow.
            int i = n - k + 1;
            for (int j = 1; j <= k; j++) {
                result = result * i / j;
                i++;
            }
        } else if (n <= 66) {
            // For n > 61 but n <= 66, the result cannot overflow,
            // but we must take care not to overflow intermediate values.
            int i = n - k + 1;
            for (int j = 1; j <= k; j++) {
                // We know that (result * i) is divisible by j,
                // but (result * i) may overflow, so we split j:
                // Filter out the gcd, d, so j/d and i/d are integer.
                // result is divisible by (j/d) because (j/d)
                // is relative prime to (i/d) and is a divisor of
                // result * (i/d).
                final long d = gcd(i, j);
                result = (result / (j / d)) * (i / d);
                i++;
            }
        } else {
            // For n > 66, a result overflow might occur, so we check
            // the multiplication, taking care to not overflow
            // unnecessary.
            int i = n - k + 1;
            for (int j = 1; j <= k; j++) {
                final long d = gcd(i, j);
                result = mulAndCheck(result / (j / d), i / d);
                i++;
            }
        }
        return result;
    }

    /**
     * Returns n!. Shorthand for {@code n} <a
     * href="http://mathworld.wolfram.com/Factorial.html"> Factorial</a>, the
     * product of the numbers {@code 1,...,n}.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> {@code n >= 0} (otherwise
     * {@code MathIllegalArgumentException} is thrown)</li>
     * <li> The result is small enough to fit into a {@code long}. The
     * largest value of {@code n} for which {@code n!} does not exceed
     * Long.MAX_VALUE} is 20. If the computed value exceeds {@code Long.MAX_VALUE}
     * an {@code MathArithMeticException } is thrown.</li>
     * </ul>
     * </p>
     *
     * @param n argument
     * @return {@code n!}
     * @throws MathArithmeticException if the result is too large to be represented
     * by a {@code long}.
     * @throws NotPositiveException if {@code n < 0}.
     * @throws MathArithmeticException if {@code n > 20}: The factorial value is too
     * large to fit in a {@code long}.
     */
    public static long factorial(final int n) throws java.lang.IllegalArgumentException, java.lang.ArithmeticException {
        if (n < 0) {
            throw new java.lang.IllegalArgumentException();
        }
        if (n > 20) {
            throw new java.lang.ArithmeticException();
        }
        return FACTORIALS[n];
    }

    /**
     * Returns the <a
     * href="http://mathworld.wolfram.com/StirlingNumberoftheSecondKind.html">
     * Stirling number of the second kind</a>, "{@code S(n,k)}", the number of
     * ways of partitioning an {@code n}-element set into {@code k} non-empty
     * subsets.
     * <p>
     * The preconditions are {@code 0 <= k <= n } (otherwise
     * {@code NotPositiveException} is thrown)
     * </p>
     * @param n the size of the set
     * @param k the number of non-empty subsets
     * @return {@code S(n,k)}
     * @throws NotPositiveException if {@code k < 0}.
     * @throws NumberIsTooLargeException if {@code k > n}.
     * @throws MathArithmeticException if some overflow happens, typically for n exceeding 25 and
     * k between 20 and n-2 (S(n,n-1) is handled specifically and does not overflow)
     * @since 3.1
     */
    public static long stirlingS2(final int n, final int k) throws java.lang.IllegalArgumentException, java.lang.ArithmeticException {
        if (k < 0) {
            throw new java.lang.IllegalArgumentException();
        }
        if (k > n) {
            throw new java.lang.IllegalArgumentException();
        }
        long[][] stirlingS2 = STIRLING_S2.get();
        if (stirlingS2 == null) {
            // the cache has never been initialized, compute the first numbers
            // by direct recurrence relation
            // as S(26,9) = 11201516780955125625 is larger than Long.MAX_VALUE
            // we must stop computation at row 26
            final int maxIndex = 26;
            stirlingS2 = new long[maxIndex][];
            stirlingS2[0] = new long[] { 1l };
            for (int i = 1; i < stirlingS2.length; ++i) {
                stirlingS2[i] = new long[i + 1];
                stirlingS2[i][0] = 0;
                stirlingS2[i][1] = 1;
                stirlingS2[i][i] = 1;
                for (int j = 2; j < i; ++j) {
                    stirlingS2[i][j] = j * stirlingS2[i - 1][j] + stirlingS2[i - 1][j - 1];
                }
            }
            // atomically save the cache
            STIRLING_S2.compareAndSet(null, stirlingS2);
        }
        if (n < stirlingS2.length) {
            // the number is in the small cache
            return stirlingS2[n][k];
        } else {
            // use explicit formula to compute the number without caching it
            if (k == 0) {
                return 0;
            } else if (k == 1 || k == n) {
                return 1;
            } else if (k == 2) {
                return (1l << (n - 1)) - 1l;
            } else if (k == n - 1) {
                return binomialCoefficient(n, 2);
            } else {
                // definition formula: note that this may trigger some overflow
                long sum = 0;
                long sign = ((k & 0x1) == 0) ? 1 : -1;
                for (int j = 1; j <= k; ++j) {
                    sign = -sign;
                    sum += sign * binomialCoefficient(k, j) * pow(j, n);
                    if (sum < 0) {
                        // there was an overflow somewhere
                        throw new java.lang.ArithmeticException();
                    }
                }
                return sum / factorial(k);
            }
        }
    }
}

