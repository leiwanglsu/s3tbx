/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.s3tbx.meris.radiometry;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.meris.radiometry.calibration.CalibrationAlgorithm;
import org.esa.s3tbx.meris.radiometry.calibration.Resolution;
import org.esa.s3tbx.meris.radiometry.equalization.EqualizationAlgorithm;
import org.esa.s3tbx.meris.radiometry.equalization.ReprocessingVersion;
import org.esa.s3tbx.meris.radiometry.smilecorr.SmileCorrectionAlgorithm;
import org.esa.s3tbx.meris.radiometry.smilecorr.SmileCorrectionAuxdata;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.pointop.ProductConfigurer;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SampleOperator;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.math.RsMathUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

import static org.esa.snap.dataio.envisat.EnvisatConstants.MERIS_DETECTOR_INDEX_DS_NAME;
import static org.esa.snap.dataio.envisat.EnvisatConstants.MERIS_L1B_FLAGS_DS_NAME;
import static org.esa.snap.dataio.envisat.EnvisatConstants.MERIS_L1B_RADIANCE_10_BAND_NAME;
import static org.esa.snap.dataio.envisat.EnvisatConstants.MERIS_L1B_RADIANCE_11_BAND_NAME;
import static org.esa.snap.dataio.envisat.EnvisatConstants.MERIS_L1B_RADIANCE_12_BAND_NAME;
import static org.esa.snap.dataio.envisat.EnvisatConstants.MERIS_L1B_RADIANCE_13_BAND_NAME;
import static org.esa.snap.dataio.envisat.EnvisatConstants.MERIS_L1B_RADIANCE_14_BAND_NAME;
import static org.esa.snap.dataio.envisat.EnvisatConstants.MERIS_L1B_RADIANCE_15_BAND_NAME;
import static org.esa.snap.dataio.envisat.EnvisatConstants.MERIS_L1B_RADIANCE_1_BAND_NAME;
import static org.esa.snap.dataio.envisat.EnvisatConstants.MERIS_L1B_RADIANCE_2_BAND_NAME;
import static org.esa.snap.dataio.envisat.EnvisatConstants.MERIS_L1B_RADIANCE_3_BAND_NAME;
import static org.esa.snap.dataio.envisat.EnvisatConstants.MERIS_L1B_RADIANCE_4_BAND_NAME;
import static org.esa.snap.dataio.envisat.EnvisatConstants.MERIS_L1B_RADIANCE_5_BAND_NAME;
import static org.esa.snap.dataio.envisat.EnvisatConstants.MERIS_L1B_RADIANCE_6_BAND_NAME;
import static org.esa.snap.dataio.envisat.EnvisatConstants.MERIS_L1B_RADIANCE_7_BAND_NAME;
import static org.esa.snap.dataio.envisat.EnvisatConstants.MERIS_L1B_RADIANCE_8_BAND_NAME;
import static org.esa.snap.dataio.envisat.EnvisatConstants.MERIS_L1B_RADIANCE_9_BAND_NAME;
import static org.esa.snap.dataio.envisat.EnvisatConstants.MERIS_L1_TYPE_PATTERN;
import static org.esa.snap.dataio.envisat.EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME;


/**
 * This operator is used to perform radiometric corrections on MERIS L1b data products.
 * The corrections include the following optional steps:
 * <ul>
 * <li><b>Radiometric re-calibration</b><br/>
 * Multiplies the inverse gains of the 2nd reprocessing and multiplies the gains of 3rd reprocessing
 * to the radiance values.</li>
 * <li><b>Smile-effect correction</b><br/>
 * Corrects the radiance values for the small variations of the spectral wavelength
 * of each pixel along the image (smile-effect).
 * </li>
 * <li><b>Meris equalisation</b><br/>
 * Removes systematic detector-to-detector radiometric differences in MERIS L1b data products. </li>
 * <li><b>Radiance-to-reflectance conversion</b><br/>
 * Converts the TOA radiance values into TOA reflectance values. </li>
 * </ul>
 *
 * @author Marco Peters
 * @since BEAM 4.9
 */
@OperatorMetadata(alias = "Meris.CorrectRadiometry",
        description = "Performs radiometric corrections on MERIS L1b data products.",
        authors = "Marc Bouvet (ESTEC); Marco Peters, Ralf Quast, Thomas Storm, Marco Zuehlke (Brockmann Consult)",
        copyright = "(c) 2015 by Brockmann Consult",
        category = "Optical/Preprocessing",
        version = "1.2")
public class MerisRadiometryCorrectionOp extends SampleOperator {

    private static final String UNIT_DL = "dl";
    private static final double RAW_SATURATION_THRESHOLD = 65435.0;
    private static final String DEFAULT_SOURCE_RAC_RESOURCE = "MER_RAC_AXVIEC20050708_135553_20021224_121445_20041213_220000";
    private static final String DEFAULT_TARGET_RAC_RESOURCE = "MER_RAC_AXVACR20091016_154511_20021224_121445_20041213_220000";
    private static final int INVALID_BIT_INDEX = 7;
    private static final int LAND_BIT_INDEX = 4;

    @Parameter(defaultValue = "true",
            label = "Perform calibration",
            description = "Whether to perform the calibration.")
    private boolean doCalibration;

    @Parameter(label = "Source radiometric correction file (optional)",
            description = "The radiometric correction auxiliary file for the source product. " +
                          "The default '" + DEFAULT_SOURCE_RAC_RESOURCE + "'")
    private File sourceRacFile;

    @Parameter(label = "Target radiometric correction file (optional)",
            description = "The radiometric correction auxiliary file for the target product. " +
                          "The default '" + DEFAULT_TARGET_RAC_RESOURCE + "'")
    private File targetRacFile;

    @Parameter(defaultValue = "true",
            label = "Perform Smile-effect correction",
            description = "Whether to perform Smile-effect correction.")
    private boolean doSmile;

    @Parameter(defaultValue = "true",
            label = "Perform equalisation",
            description = "Perform removal of detector-to-detector systematic radiometric differences in MERIS L1b data products.")
    private boolean doEqualization;

    @Parameter(label = "Reprocessing version", valueSet = {"AUTO_DETECT", "REPROCESSING_2", "REPROCESSING_3"},
            defaultValue = "AUTO_DETECT",
            description = "The version of the reprocessing the product comes from. Is only used if " +
                    "equalisation is enabled.")
    private ReprocessingVersion reproVersion;
    private transient ReprocessingVersion effectiveReproVersion = ReprocessingVersion.AUTO_DETECT;

    @Parameter(defaultValue = "false",
            label = "Perform radiance-to-reflectance conversion",
            description = "Whether to perform radiance-to-reflectance conversion. " +
                          "When selecting ENVISAT as target format, the radiance to reflectance conversion can not be performed.")
    private boolean doRadToRefl;

    @SourceProduct(alias = "source", label = "Name", description = "The source product.",
            bands = {
                    MERIS_L1B_FLAGS_DS_NAME, MERIS_DETECTOR_INDEX_DS_NAME,
                    MERIS_L1B_RADIANCE_1_BAND_NAME,
                    MERIS_L1B_RADIANCE_2_BAND_NAME,
                    MERIS_L1B_RADIANCE_3_BAND_NAME,
                    MERIS_L1B_RADIANCE_4_BAND_NAME,
                    MERIS_L1B_RADIANCE_5_BAND_NAME,
                    MERIS_L1B_RADIANCE_6_BAND_NAME,
                    MERIS_L1B_RADIANCE_7_BAND_NAME,
                    MERIS_L1B_RADIANCE_8_BAND_NAME,
                    MERIS_L1B_RADIANCE_9_BAND_NAME,
                    MERIS_L1B_RADIANCE_10_BAND_NAME,
                    MERIS_L1B_RADIANCE_11_BAND_NAME,
                    MERIS_L1B_RADIANCE_12_BAND_NAME,
                    MERIS_L1B_RADIANCE_13_BAND_NAME,
                    MERIS_L1B_RADIANCE_14_BAND_NAME,
                    MERIS_L1B_RADIANCE_15_BAND_NAME
            })
    private Product sourceProduct;

    private transient CalibrationAlgorithm calibrationAlgorithm;
    private transient EqualizationAlgorithm equalizationAlgorithm;
    private transient SmileCorrectionAlgorithm smileCorrAlgorithm;

    private transient int detectorIndexSampleIndex;
    private transient int sunZenithAngleSampleIndex;
    private transient int flagBandIndex;
    private transient int currentPixel = 0;
    private Map<Integer, Double> bandIndexToMaxValueMap;

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();
        validateSourceProduct();
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) {
        int i = -1;
        // define samples corresponding to spectral bands, using the spectral band index as sample index
        for (final Band band : getSourceProduct().getBands()) {
            final int spectralBandIndex = band.getSpectralBandIndex();
            if (spectralBandIndex != -1) {
                sampleConfigurer.defineSample(spectralBandIndex, band.getName());
                if (spectralBandIndex > i) {
                    i = spectralBandIndex;
                }
            }
        }
        detectorIndexSampleIndex = i + 1;
        if (doCalibration || doSmile || doEqualization) {
            sampleConfigurer.defineSample(detectorIndexSampleIndex, MERIS_DETECTOR_INDEX_DS_NAME);
        }
        sunZenithAngleSampleIndex = i + 2;
        if (doRadToRefl) {
            sampleConfigurer.defineSample(sunZenithAngleSampleIndex, MERIS_SUN_ZENITH_DS_NAME);
        }
        flagBandIndex = i + 3;
        if (doSmile) {
            sampleConfigurer.defineSample(flagBandIndex, MERIS_L1B_FLAGS_DS_NAME);
        }
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) {
        // define samples corresponding to spectral bands, using the spectral band index as sample index
        for (final Band band : getTargetProduct().getBands()) { // pitfall: using targetProduct field here throws NPE
            final int spectralBandIndex = band.getSpectralBandIndex();
            if (spectralBandIndex != -1) {
                sampleConfigurer.defineSample(spectralBandIndex, band.getName());
            }
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        productConfigurer.copyMetadata();
        productConfigurer.copyTimeCoding();

        Product targetProduct = productConfigurer.getTargetProduct();
        targetProduct.setName(getSourceProduct().getName());
        if (doRadToRefl) {
            targetProduct.setProductType(String.format("%s_REFL", getSourceProduct().getProductType()));
            targetProduct.setAutoGrouping("reflec");
        } else {
            targetProduct.setProductType(getSourceProduct().getProductType());
            targetProduct.setAutoGrouping("radiance");
        }
        targetProduct.setDescription("MERIS L1b Radiometric Correction");

        bandIndexToMaxValueMap = new TreeMap<>();
        Band[] bands = getSourceProduct().getBands();
        int bandIndex = 0;
        for (Band sourceBand : bands) {
            if (sourceBand.getSpectralBandIndex() != -1) {
                final String targetBandName;
                final String targetBandDescription;
                final int dataType;
                final String unit;
                final double scalingFactor;
                final double scalingOffset;
                if (doRadToRefl) {
                    targetBandName = sourceBand.getName().replace("radiance", "reflec");
                    targetBandDescription = "Radiometry-corrected TOA reflectance";
                    dataType = ProductData.TYPE_FLOAT32;
                    unit = UNIT_DL;
                    scalingFactor = 1.0;
                    scalingOffset = 0;
                } else {
                    targetBandName = sourceBand.getName();
                    targetBandDescription = "Radiometry-corrected TOA radiance";
                    dataType = sourceBand.getDataType();
                    unit = sourceBand.getUnit();
                    scalingFactor = sourceBand.getScalingFactor();
                    scalingOffset = sourceBand.getScalingOffset();
                }
                final Band targetBand = targetProduct.addBand(targetBandName, dataType);
                targetBand.setScalingFactor(scalingFactor);
                targetBand.setScalingOffset(scalingOffset);
                bandIndexToMaxValueMap.put(bandIndex++, targetBand.scale(0xFFFF));
                targetBand.setDescription(targetBandDescription);
                targetBand.setUnit(unit);
                targetBand.setValidPixelExpression(sourceBand.getValidPixelExpression());
                ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
            }
        }

        productConfigurer.copyTiePointGrids(); // fixme: always need to copy tie-points before copying geo-coding (nf)
        productConfigurer.copyGeoCoding();

        // copy all source bands yet ignored
        for (final Band sourceBand : getSourceProduct().getBands()) {
            if (sourceBand.getSpectralBandIndex() == -1 && !targetProduct.containsBand(sourceBand.getName())) {
                productConfigurer.copyBands(sourceBand.getName());
            }
        }
        productConfigurer.copyMasks();
    }

    @Override
    protected void computeSample(int x, int y, Sample[] sourceSamples, WritableSample targetSample) {
        checkCancellation();

        final int bandIndex = targetSample.getIndex();
        final Sample sourceRadiance = sourceSamples[bandIndex];
        int detectorIndex = -1;
        if (doCalibration || doSmile || doEqualization) {
            detectorIndex = sourceSamples[detectorIndexSampleIndex].getInt();
        }
        double value = sourceRadiance.getDouble();
        boolean isValidDetectorIndex = detectorIndex >= 0;
        if (doCalibration && isValidDetectorIndex && value < sourceRadiance.getNode().scale(RAW_SATURATION_THRESHOLD)) {
            value = calibrationAlgorithm.calibrate(bandIndex, detectorIndex, value);
        }
        if (doSmile) {
            final Sample flagSample = sourceSamples[flagBandIndex];
            final boolean invalid = flagSample.getBit(INVALID_BIT_INDEX);
            if (!invalid && detectorIndex != -1) {
                final boolean land = flagSample.getBit(LAND_BIT_INDEX);
                double[] sourceValues = new double[15];
                for (int i = 0; i < sourceValues.length; i++) {
                    sourceValues[i] = sourceSamples[i].getDouble();
                }
                value = smileCorrAlgorithm.correct(bandIndex, detectorIndex, sourceValues, land);
            }
        }
        if (doRadToRefl) {
            final float solarFlux = ((Band) sourceRadiance.getNode()).getSolarFlux();
            final float sunZenithSample = sourceSamples[sunZenithAngleSampleIndex].getFloat();
            value = RsMathUtils.radianceToReflectance((float) value, sunZenithSample, solarFlux);
        }
        if (doEqualization && isValidDetectorIndex) {
            value = equalizationAlgorithm.performEqualization(value, bandIndex, detectorIndex);
        }

        final double croppedValue = Math.min(bandIndexToMaxValueMap.get(bandIndex), value);
        targetSample.set(croppedValue);

    }

    @Override
    public void doExecute(ProgressMonitor pm) throws OperatorException {
        final String productType = getSourceProduct().getProductType();
        int workload = 0;
        if (doCalibration) {
            workload++;
        }
        if (doSmile) {
            workload++;
        }
        if (doEqualization) {
            workload++;
        }
        pm.beginTask("Initializing algorithms", workload);
        try {
            if (doCalibration) {
                pm.setSubTaskName("Initializing calibration algorithm");
                try (
                        InputStream sourceRacStream = openStream(sourceRacFile, DEFAULT_SOURCE_RAC_RESOURCE);
                        InputStream targetRacStream = openStream(targetRacFile, DEFAULT_TARGET_RAC_RESOURCE)
                ) {
                    final double cntJD = 0.5 * (getSourceProduct().getStartTime().getMJD() +
                                                getSourceProduct().getEndTime().getMJD());
                    final Resolution resolution = productType.contains("RR") ? Resolution.RR : Resolution.FR;
                    calibrationAlgorithm = new CalibrationAlgorithm(resolution, cntJD, sourceRacStream, targetRacStream);
                } catch (IOException e) {
                    throw new OperatorException(e);
                }
                // If calibration is performed the equalization  has to use the LUTs of Reprocessing 3
                effectiveReproVersion = ReprocessingVersion.REPROCESSING_3;
                pm.worked(1);
            }
            if (doSmile) {
                pm.setSubTaskName("Initializing smile correction algorithm");
                smileCorrAlgorithm = new SmileCorrectionAlgorithm(SmileCorrectionAuxdata.loadAuxdata(productType));
                pm.worked(1);
            }
            if (doEqualization) {
                pm.setSubTaskName("Initializing equalization algorithm");
                equalizationAlgorithm = new EqualizationAlgorithm(getSourceProduct(), effectiveReproVersion);
                pm.worked(1);
            }
        } catch (Exception e) {
            throw new OperatorException(e);
        } finally {
            pm.done();
        }
    }

    private static InputStream openStream(File racFile, String defaultRacResource) throws FileNotFoundException {
        if (racFile == null) {
            return CalibrationAlgorithm.class.getResourceAsStream(defaultRacResource);
        } else {
            return new FileInputStream(racFile);
        }
    }

    private void validateSourceProduct() throws OperatorException {
        if (!MERIS_L1_TYPE_PATTERN.matcher(getSourceProduct().getProductType()).matches()) {
            String msg = String.format("Source product must be of type MERIS Level 1b. Product type is: '%s'",
                                       getSourceProduct().getProductType());
            getLogger().warning(msg);
        }
        if (reproVersion.getVersion() == -1) { // auto-detection is enabled
            effectiveReproVersion = ReprocessingVersion.autoDetect(getSourceProduct());
        }
        if (effectiveReproVersion.equals(ReprocessingVersion.REPROCESSING_1)) {
            throw new OperatorException("Source product is before reprocessing version 2. Check if you can get the data from a more recent dataset.");
        }
        if (effectiveReproVersion.equals(ReprocessingVersion.AUTO_DETECT)) {
            throw new OperatorException("Reprocessing could not be detected. Check if the source product is valid.");
        }
        boolean isReprocessing2 = effectiveReproVersion == ReprocessingVersion.REPROCESSING_2;
        if (!isReprocessing2 && doCalibration) {
            getLogger().warning("Skipping calibration. Source product is already of 3rd reprocessing.");
            doCalibration = false;
        }
        if (doCalibration || doEqualization) {
            if (getSourceProduct().getStartTime() == null) {
                throw new OperatorException("Source product must have a start time");
            }
        }
        if (doCalibration) {
            if (getSourceProduct().getEndTime() == null) {
                throw new OperatorException("Source product must have an end time");
            }
        }

        final String msgPatternMissingBand = "Source product must contain '%s'.";
        if (doSmile) {
            if (!getSourceProduct().containsBand(MERIS_DETECTOR_INDEX_DS_NAME)) {
                throw new OperatorException(String.format(msgPatternMissingBand, MERIS_DETECTOR_INDEX_DS_NAME));
            }
            if (!getSourceProduct().containsBand(MERIS_L1B_FLAGS_DS_NAME)) {
                throw new OperatorException(String.format(msgPatternMissingBand, MERIS_L1B_FLAGS_DS_NAME));
            }
            if (!getSourceProduct().getBand(MERIS_L1B_FLAGS_DS_NAME).isFlagBand()) {
                throw new OperatorException(
                        String.format("Flag-coding is missing for band '%s' ", MERIS_L1B_FLAGS_DS_NAME));
            }
        }
        if (doEqualization) {
            if (!getSourceProduct().containsBand(MERIS_DETECTOR_INDEX_DS_NAME)) {
                throw new OperatorException(String.format(msgPatternMissingBand, MERIS_DETECTOR_INDEX_DS_NAME));
            }
        }
        if (doRadToRefl) {
            if (!getSourceProduct().containsRasterDataNode(MERIS_SUN_ZENITH_DS_NAME)) {
                throw new OperatorException(String.format(msgPatternMissingBand, MERIS_SUN_ZENITH_DS_NAME));
            }
        }
    }

    private void checkCancellation() {
        if (currentPixel % 1000 == 0) {
            checkForCancellation();
            currentPixel = 0;
        }
        currentPixel++;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MerisRadiometryCorrectionOp.class);
        }
    }
}
