/*
 * @(#)ThumbnailNamer.java   1.0   Apr 29, 2015
 *
 * Copyright 2000-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 *
 * @(#) $Id$
 */
package de.uni_siegen.wineme.come_in.thumbnailer;

import java.io.File;


public class ThumbnailNamer {

  public static String getName(final File outputFolder, final int pageNumber) {
    return outputFolder.getName() + "-" + pageNumber + ".png";
  }

  public static File getFile(final File outputFolder, final int pageNumber) {
    return new File(outputFolder, getName(outputFolder, pageNumber));
  }

}
