/*
 *
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 */

package org.esa.s3tbx.dataio.avhrr.noaa.pod;

import org.esa.snap.core.dataio.persistence.Item;
import org.esa.snap.core.dataio.persistence.PersistenceConverter;
import org.esa.snap.core.dataio.persistence.PersistenceSpi;

/**
 * @author Marco Zuehlke
 * @author Sabine Embacher
 */
public class PodGeoCodingPersistenceSpi implements PersistenceSpi {

    public static final PodGeoCodingPersistenceConverter CONVERTER = new PodGeoCodingPersistenceConverter();

    @Override
    public boolean canDecode(Item item) {
        return CONVERTER.canDecode(item);
    }

    @Override
    public boolean canEncode(Object object) {
        return object instanceof PodGeoCoding;
    }

    @Override
    public PersistenceConverter createConverter() {
        return CONVERTER;
    }
}
