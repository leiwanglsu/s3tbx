package org.esa.s3tbx.c2rcc.landsat;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Marco Peters
 */
public class Landsat8ProductSignatureTest {
    private static final String[] EXPECTED_RHOW_BANDS = {
            "rhow_" + 1, "rhow_" + 2, "rhow_" + 3, "rhow_" + 4, "rhow_" + 5};
    private static final String[] EXPECTED_RRS_BANDS = {
            "rrs_" + 1, "rrs_" + 2, "rrs_" + 3, "rrs_" + 4, "rrs_" + 5};
    private static final String[] EXPECTED_NORM_REFLEC_BANDS = {
            "rhown_" + 1, "rhown_" + 2, "rhown_" + 3, "rhown_" + 4, "rhown_" + 5};
    private static final String EXPECTED_IOP_APIG = "iop_apig";
    private static final String EXPECTED_IOP_ADET = "iop_adet";
    private static final String EXPECTED_IOP_AGELB = "iop_agelb";
    private static final String EXPECTED_IOP_BPART = "iop_bpart";
    private static final String EXPECTED_IOP_BWIT = "iop_bwit";
    private static final String EXPECTED_IOP_ADG = "iop_adg";
    private static final String EXPECTED_IOP_ATOT = "iop_atot";
    private static final String EXPECTED_IOP_BTOT = "iop_btot";
    private static final String EXPECTED_CONC_CHL = "conc_chl";
    private static final String EXPECTED_CONC_TSM = "conc_tsm";
    private static final String[] EXPECTED_KD_BANDS = {"kd489", "kdmin", "kd_z90max"};
    private static final String EXPECTED_OOS_RTOSA = "oos_rtosa";
    private static final String EXPECTED_OOS_RHOW ="oos_rhow";
    private static final String EXPECTED_OOS_RRS = "oos_rrs";
    private static final String[] EXPECTED_IOP_UNC_BANDS = {
            "unc_apig", "unc_adet", "unc_agelb", "unc_bpart",
            "unc_bwit", "unc_adg", "unc_atot", "unc_btot"};
    private static final String[] EXPECTED_KD_UNC_BANDS = {"unc_kd489", "unc_kdmin"};
    private static final String[] EXPECTED_RTOSA_GC_BANDS = {
            "rtosa_gc_" + 1, "rtosa_gc_" + 2, "rtosa_gc_" + 3, "rtosa_gc_" + 4, "rtosa_gc_" + 5};
    private static final String[] EXPECTED_RTOSA_GCAANN_BANDS = {
            "rtosagc_aann_" + 1, "rtosagc_aann_" + 2, "rtosagc_aann_" + 3, "rtosagc_aann_" + 4, "rtosagc_aann_" + 5};
    private static final String[] EXPECTED_RTOA_BANDS = {
            "rtoa_" + 1, "rtoa_" + 2, "rtoa_" + 3, "rtoa_" + 4, "rtoa_" + 5};
    private static final String[] EXPECTED_RPATH_BANDS = {
            "rpath_" + 1, "rpath_" + 2, "rpath_" + 3, "rpath_" + 4, "rpath_" + 5};
    private static final String[] EXPECTED_TDOWN_BANDS = {
            "tdown_" + 1, "tdown_" + 2, "tdown_" + 3, "tdown_" + 4, "tdown_" + 5};
    private static final String[] EXPECTED_TUP_BANDS = {
            "tup_" + 1, "tup_" + 2, "tup_" + 3, "tup_" + 4, "tup_" + 5};

    private static final String EXPECTED_SOURCE_FLAGS = "flags";
    private static final String EXPECTED_C2RCC_FLAGS = "c2rcc_flags";

    private static final String EXPECTED_PRODUCT_TYPE = "C2RCC_LANDSAT-8";

    @Test
    public void testProductSignature_Default() throws FactoryException, TransformException {

        C2rccLandsat8Operator operator = createDefaultOperator();

        Product targetProduct = operator.getTargetProduct();

        assertDefaults(targetProduct, false);
    }

    @Test
    public void testProductSignature_Default_Rrs() throws FactoryException, TransformException {

        C2rccLandsat8Operator operator = createDefaultOperator();
        operator.setOutputAsRrs(true);
        operator.setOutputOos(true);

        Product targetProduct = operator.getTargetProduct();
        assertDefaults(targetProduct, true);
        assertBands(targetProduct, EXPECTED_OOS_RTOSA);
        assertBands(targetProduct, EXPECTED_OOS_RRS);

    }

    @Test
    public void testProductSignature_OnlyMandatory() throws FactoryException, TransformException {

        C2rccLandsat8Operator operator = createDefaultOperator();
        operator.setOutputRtoa(false);
        operator.setOutputUncertainties(false);
        operator.setOutputAcReflectance(false);
        operator.setOutputRhown(false);
        operator.setOutputKd(false);
        Product targetProduct = operator.getTargetProduct();

        assertMandatoryBands(targetProduct);
        assertEquals(12, targetProduct.getNumBands());
    }

    @Test
    public void testProductSignature_DefaultWithRtosa() throws FactoryException, TransformException {

        C2rccLandsat8Operator operator = createDefaultOperator();
        operator.setOutputRtosa(true);
        operator.setOutputRtosaGcAann(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaults(targetProduct, false);
        assertBands(targetProduct, EXPECTED_RTOSA_GC_BANDS);
        assertBands(targetProduct, EXPECTED_RTOSA_GCAANN_BANDS);
    }

    @Test
    public void testProductSignature_DefaultWithRtoa() throws FactoryException, TransformException {

        C2rccLandsat8Operator operator = createDefaultOperator();
        operator.setOutputRtoa(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaults(targetProduct, false);
        assertBands(targetProduct, EXPECTED_RTOA_BANDS);
    }

    @Test
    public void testProductSignature_DefaultWithOthers() throws FactoryException, TransformException {

        C2rccLandsat8Operator operator = createDefaultOperator();
        operator.setOutputRpath(true);
        operator.setOutputTdown(true);
        operator.setOutputTup(true);
        operator.setOutputOos(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaults(targetProduct, false);
        assertBands(targetProduct, EXPECTED_RPATH_BANDS);
        assertBands(targetProduct, EXPECTED_TDOWN_BANDS);
        assertBands(targetProduct, EXPECTED_TUP_BANDS);
        assertBands(targetProduct, EXPECTED_OOS_RTOSA);
        assertBands(targetProduct, EXPECTED_OOS_RHOW);
    }

    @Test
    public void testProductSignature_DeriveRwAlternative() throws FactoryException, TransformException {

        C2rccLandsat8Operator operator = createDefaultOperator();
        operator.setDeriveRwFromPathAndTransmittance(true);

        Product targetProduct = operator.getTargetProduct();
        assertDefaults(targetProduct, false);
    }

    @Test
    public void testProductSignature_DefaultWithUncertainties() throws FactoryException, TransformException {

        C2rccLandsat8Operator operator = createDefaultOperator();
        operator.setOutputUncertainties(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaults(targetProduct, false);
        assertBands(targetProduct, EXPECTED_IOP_UNC_BANDS);
    }

    @Test
    public void testProductSignature_DefaultWithUncertaintiesAndKd() throws FactoryException, TransformException {

        C2rccLandsat8Operator operator = createDefaultOperator();
        operator.setOutputUncertainties(true);
        operator.setOutputKd(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaults(targetProduct, false);
        assertBands(targetProduct, EXPECTED_IOP_UNC_BANDS);
        assertBands(targetProduct, EXPECTED_KD_UNC_BANDS);
        assertBands(targetProduct, EXPECTED_KD_BANDS);
    }

    private void assertDefaults(Product targetProduct, boolean asRrs) {
        assertMandatoryBands(targetProduct);
        if(asRrs) {
            assertBands(targetProduct, EXPECTED_RRS_BANDS);
        }else {
            assertBands(targetProduct, EXPECTED_RHOW_BANDS);
        }
        assertBands(targetProduct, EXPECTED_RTOA_BANDS);
        assertBands(targetProduct, EXPECTED_NORM_REFLEC_BANDS);
        assertBands(targetProduct, EXPECTED_KD_BANDS);
        assertBands(targetProduct, EXPECTED_KD_UNC_BANDS);
        assertBands(targetProduct, EXPECTED_IOP_UNC_BANDS);

        assertEquals(EXPECTED_PRODUCT_TYPE, targetProduct.getProductType());
    }

    private void assertMandatoryBands(Product targetProduct) {
        assertBands(targetProduct, EXPECTED_IOP_APIG);
        assertBands(targetProduct, EXPECTED_IOP_ADET);
        assertBands(targetProduct, EXPECTED_IOP_AGELB);
        assertBands(targetProduct, EXPECTED_IOP_BPART);
        assertBands(targetProduct, EXPECTED_IOP_BWIT);
        assertBands(targetProduct, EXPECTED_IOP_ADG);
        assertBands(targetProduct, EXPECTED_IOP_ATOT);
        assertBands(targetProduct, EXPECTED_IOP_BTOT);
        assertBands(targetProduct, EXPECTED_CONC_CHL);
        assertBands(targetProduct, EXPECTED_CONC_TSM);
        assertBands(targetProduct, EXPECTED_SOURCE_FLAGS);
        assertBands(targetProduct, EXPECTED_C2RCC_FLAGS);
    }

    private void assertBands(Product targetProduct, String... expectedBands) {
        for (String expectedBand : expectedBands) {
            assertTrue("Expected band " + expectedBand + " in product", targetProduct.containsBand(expectedBand));
        }
    }

    private C2rccLandsat8Operator createDefaultOperator() throws FactoryException, TransformException {
        C2rccLandsat8Operator operator = new C2rccLandsat8Operator();
        operator.setParameterDefaultValues();
        operator.setSourceProduct(createLandsat8TestProduct());
        return operator;
    }

    private Product createLandsat8TestProduct() throws FactoryException, TransformException {
        Product product = new Product("test-L8", "t", 1, 1);
        for (int i = 0; i < C2rccLandsat8Operator.L8_BAND_COUNT; i++) {
            String expression = String.valueOf(i);
            product.addBand(C2rccLandsat8Operator.EXPECTED_BANDNAMES[i], expression);
        }
        MetadataElement metadataRoot = product.getMetadataRoot();
        final MetadataElement metadata = LandsatMetadataTest.createMetadata(0);
        metadataRoot.addElement(metadata);

        Date time = new Date();
        product.setStartTime(ProductData.UTC.create(time, 0));
        product.setEndTime(ProductData.UTC.create(time, 500));

        Band flagBand = product.addBand("flags", ProductData.TYPE_INT8);
        FlagCoding l1FlagsCoding = new FlagCoding("flags");
        l1FlagsCoding.addFlag("terrain_occlusion", 2, "description");
        l1FlagsCoding.addFlag("cloud", 16, "description");

        product.getFlagCodingGroup().add(l1FlagsCoding);
        flagBand.setSampleCoding(l1FlagsCoding);

        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 1, 1, 10, 50, 1, 1));

        return product;
    }
}