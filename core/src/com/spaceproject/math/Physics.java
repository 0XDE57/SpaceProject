package com.spaceproject.math;


import com.badlogic.gdx.Gdx;

import java.util.Arrays;

/**
 * Disclaimer: I am not a physicist. There may be errata but I will do my best.
 * Sources:
 *      https://en.wikipedia.org/wiki/Wien%27s_displacement_law
 *      https://en.wikipedia.org/wiki/Black-body_radiation
 *      https://en.wikipedia.org/wiki/Planck%27s_law
 *      https://en.wikipedia.org/wiki/Stellar_classification
 *
 *      https://www.fourmilab.ch/documents/specrend/
 *      https://en.wikipedia.org/wiki/CIE_1931_color_space#Color_matching_functions
 *
 *      Wolfram Alpha for testing and confirming formulas and values.
 *      https://www.wolframalpha.com/widgets/view.jsp?id=5072e9b72faacd73c9a4e4cb36ad08d
 */
public class Physics {
    
    // ------ Universal Constants ------
    // c: Speed of light: 299,792,458 (meters per second)
    public static final double speedOfLight = 299792458; //m/s
    
    // h: Planck's constant: 6.626 × 10^-34 (Joule seconds)
    public static final double planckConstant = 6.626 * (10 ^ -34); //Js
    
    // k: Boltzmann constant: 1.380649 × 10^-23 J⋅K (Joules per Kelvin)
    public static final double boltzmannConstant = 1.380649 * (10 ^ -23); //JK
    
    // b: Wien's displacement constant: 2.897771955 × 10−3 m⋅K,[1] or b ≈ 2898 μm⋅K
    public static final double wiensDisplacementConstant = 2.8977719; //mK
    
    // G: Gravitational constant: 6.674×10−11 Nm^2 / kg^2 (newton square meters per kilogram squared)
    public static final double gravitationalConstant = 6.674 * (10 ^ -11);
    
    /** Wien's displacement law: λₘT = b
     * Hotter things - peak at shorter wavelengths - bluer
     * Cooler things - peak at longer wavelengths - redder
     * λₘ = The maximum wavelength in nanometers corresponding to peak intensity
     * T  = The absolute temperature in kelvin
     * b  = Wein’s Constant: 2.88 x 10-3 m-K or 0.288 cm-K
     */
    public static double temperatureToWavelength(double kelvin) {
        return wiensDisplacementConstant / kelvin;
    }
    
    /** T = b / λₘ */
    public static double wavelengthToTemperature(double wavelength) {
        return wiensDisplacementConstant / wavelength;
    }
    
    /** ν = c / λ
     * ν = frequency (hertz)
     * λ = wavelength (nanometers)
     * c = speed of light
     */
    public static double wavelengthToFrequency(double wavelength) {
        return speedOfLight / wavelength;
    }
    
    /** E = (h * c) / λ
     * E = energy
     * λ = wavelength (nanometers)
     * h = planck constant
     * c = speed of light
     */
    public static double wavelengthToPhotonEnergy(double wavelength) {
        return (planckConstant * speedOfLight) / wavelength;
    }
    
    /** E = hv
     * E = energy
     * v = frequency (hertz)
     * h = planck constant
     */
    public static double frequencyToPhotonEnergy(double frequency) {
        return planckConstant * frequency;
    }
    
    /** approximate RGB [0-255] values for wavelengths between 380 nm and 780 nm
     * Ported from: RGB VALUES FOR VISIBLE WAVELENGTHS by Dan Bruton (astro@tamu.edu)
     *      http://www.physics.sfasu.edu/astro/color/spectra.html
     */
    public static int[] wavelengthToRGB(double wavelength, double gamma) {
        double factor;
        double red, green, blue;
        
        if ((wavelength >= 380) && (wavelength < 440)) {
            red = -(wavelength - 440) / (440 - 380);
            green = 0.0;
            blue = 1.0;
        } else if ((wavelength >= 440) && (wavelength < 490)) {
            red = 0.0;
            green = (wavelength - 440) / (490 - 440);
            blue = 1.0;
        } else if ((wavelength >= 490) && (wavelength < 510)) {
            red = 0.0;
            green = 1.0;
            blue = -(wavelength - 510) / (510 - 490);
        } else if ((wavelength >= 510) && (wavelength < 580)) {
            red = (wavelength - 510) / (580 - 510);
            green = 1.0;
            blue = 0.0;
        } else if ((wavelength >= 580) && (wavelength < 645)) {
            red = 1.0;
            green = -(wavelength - 645) / (645 - 580);
            blue = 0.0;
        } else if ((wavelength >= 645) && (wavelength < 781)) {
            red = 1.0;
            green = 0.0;
            blue = 0.0;
        } else {
            red = 0.0;
            green = 0.0;
            blue = 0.0;
        }
        
        // Let the intensity fall off near the vision limits
        if ((wavelength >= 380) && (wavelength < 420)) {
            factor = 0.3 + 0.7 * (wavelength - 380) / (420 - 380);
        } else if ((wavelength >= 420) && (wavelength < 701)) {
            factor = 1.0;
        } else if ((wavelength >= 701) && (wavelength < 781)) {
            factor = 0.3 + 0.7 * (780 - wavelength) / (780 - 700);
        } else {
            factor = 0.0;
        }
        
        // Don't want 0^x = 1 for x <> 0
        final double intensityMax = 255;
        int[] rgb = new int[3];
        rgb[0] = red   == 0.0 ? 0 : (int)Math.round(intensityMax * Math.pow(red * factor, gamma));
        rgb[1] = green == 0.0 ? 0 : (int)Math.round(intensityMax * Math.pow(green * factor, gamma));
        rgb[2] = blue  == 0.0 ? 0 : (int)Math.round(intensityMax * Math.pow(blue * factor, gamma));
        return rgb;
    }
    
    /** approximate RGB [0-255] values for wavelengths between 380 nm and 780 nm with a default gamma of 0.8 */
    public static int[] wavelengthToRGB(double wavelength) {
        return wavelengthToRGB(wavelength, 0.8);
    }
    
    public static void test() {
        /* Black Body Radiation!
         * Common color temperatures:
         *      1900	Candle flame
         *      2000	Sunlight at sunset
         *      2800	Tungsten bulb—60 watt
         *      2900	Tungsten bulb—200 watt
         *      3300	Tungsten/halogen lamp
         *      3780	Carbon arc lamp
         *      5500	Sunlight plus skylight
         *      5772    Sun "effective temperature"
         *      6000	Xenon strobe light
         *      6500	Overcast sky
         *      7500	North sky light
         */
        double kelvin = 5772;
        double wavelength = 502; // 597.2 terahertz | 2.47 eV
        Gdx.app.debug("PhysicsDebug",kelvin + " K = " + MyMath.round(temperatureToWavelength(kelvin) * 1000000, 1) + " nm");
        Gdx.app.debug("PhysicsDebug",wavelength+ " nm = " + MyMath.round(wavelengthToTemperature(wavelength) * 1000000, 1) + " K");
        Gdx.app.debug("PhysicsDebug","temp(wave(" + kelvin + ")) = " + wavelengthToTemperature(temperatureToWavelength(kelvin)));
        Gdx.app.debug("PhysicsDebug","wave(temp(" + wavelength +")) = " + temperatureToWavelength(wavelengthToTemperature(wavelength)));
        
        Gdx.app.debug("PhysicsDebug",wavelength + " nm = " + MyMath.round(wavelengthToFrequency(wavelength) / 1000, 1) + " THz");
        //todo: photon energy calculations are returning -1.7410894895E8 eV, expecting 2.47 eV
        //bug: planck is coming out as -291.54400000000004. expected: 6.626 * (10 ^ -34)
        //looks like we have hit the Double.MIN_EXPONENT...
        Gdx.app.debug("PhysicsDebug",
                "double: [" + Double.MIN_VALUE + " to " + Double.MAX_VALUE +
                        "] exp: [" + Double.MIN_EXPONENT + " to " + Double.MAX_EXPONENT + "]");
        Gdx.app.debug("PhysicsDebug", "planck: " + planckConstant);
        Gdx.app.debug("PhysicsDebug",wavelength + " nm = " + MyMath.round(frequencyToPhotonEnergy(wavelengthToFrequency(wavelength)),2) + " eV");
        Gdx.app.debug("PhysicsDebug",wavelength + " nm = " + MyMath.round(wavelengthToPhotonEnergy((wavelength)),2) + " eV");
        
        /* Tristimulus values: The human eye with normal vision has three kinds of cone cells that sense light, having peaks of spectral sensitivity in
         *      short   420 nm – 440 nm
         *      middle  530 nm – 540 nm
         *      long    560 nm – 580 nm
         *
         * Typical color ranges:
         *      Color   Wavelength(nm) Frequency(THz)
         *      Red     620-750        484-400
         *      Orange  590-620        508-484
         *      Yellow  570-590        526-508
         *      Green   495-570        606-526
         *      Blue    450-495        668-606
         *      Violet  380-450        789-668
         */
        double gamma = 0.8;
        int red = 650;
        int green = 540;
        int blue = 470;
        Gdx.app.debug("PhysicsDebug",  wavelength + " -> " + Arrays.toString(wavelengthToRGB(wavelength, gamma)));
        Gdx.app.debug("PhysicsDebug",  red + " -> " + Arrays.toString(wavelengthToRGB(red, gamma)));//red-ish
        Gdx.app.debug("PhysicsDebug",  green + " -> " + Arrays.toString(wavelengthToRGB(green, gamma)));//green-ish
        Gdx.app.debug("PhysicsDebug",  blue + "  -> " + Arrays.toString(wavelengthToRGB(blue, gamma)));//blue-ish
    }
    
    public static class Sun {
        public static final String spectralClass = "GV2 (main sequence)";
        
        //mass: nominal solar mass parameter: GM⊙ = 1.3271244 × 10^20 m3 s−2 or 1.9885 × 10^30 kg.
        public static final double mass = 1.9885 * (10 ^ 30);//kg
        
        //radius: nominal solar radius 	R⊙ = 6.957 × 10^8 m
        public static final double radius = 6.957 * (10 ^ 8);//m
        
        //effective temperature
        public static final double kelvin = 5772; //K
        
        //5772K = 502nm = 597 THz = green light
        //so wait...is the sun peak wavelength actually green? why it look yellow
        //
        //https://www.e-education.psu.edu/meteo300/node/683
        public static final double peakWavelength = temperatureToWavelength(kelvin) * 1000000;
        
        //luminosity: 1 sol -> L⊙ = nominal solar luminosity: 	L⊙ = 3.828 × 10^26 W
        public static final double luminosity = 3.828 * (10 ^ 26); //Watts
        
        //public static final age = 4.78 billion years
        
        //AU Astronomical unit: roughly the distance from Earth to the Sun ~1.495978707 × 10^11 m
        public static final long astronomicalUnit = 149597870700L;
    }
    
}
