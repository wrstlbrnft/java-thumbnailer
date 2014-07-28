/*
 * @(#)ThumbnailGenerationResult.java   1.0   Jul 25, 2014
 *
 * Copyright 2000-2014 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 *
 * @(#) $Id$
 */
package de.uni_siegen.wineme.come_in.thumbnailer;

import java.io.File;


public class ThumbnailGenerationResult {

  private final String mimeType;
  private final File outputFolder;
  private final boolean isSuccessful;

  public ThumbnailGenerationResult(final String mimeType, final File outputFolder, final boolean isSuccessful) {
    this.mimeType = mimeType;
    this.outputFolder = outputFolder;
    this.isSuccessful = isSuccessful;
  }

  public String mimeType() {
    return this.mimeType;
  }

  public File outputFolder() {
    return this.outputFolder;
  }

  public boolean isSuccessful() {
    return this.isSuccessful;
  }

}
