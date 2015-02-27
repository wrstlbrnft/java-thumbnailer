/*
 * regain/Thumbnailer - A file search engine providing plenty of formats (Plugin)
 * Copyright (C) 2011  Come_IN Computerclubs (University of Siegen)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Contact: Come_IN-Team <come_in-team@listserv.uni-siegen.de>
 */

package de.uni_siegen.wineme.come_in.thumbnailer.thumbnailers;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.document.DocumentFormat;
import org.artofsolving.jodconverter.office.DefaultOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.OfficeException;
import org.artofsolving.jodconverter.office.OfficeManager;

import de.uni_siegen.wineme.come_in.thumbnailer.ThumbnailerException;
import de.uni_siegen.wineme.come_in.thumbnailer.util.IOUtil;
import de.uni_siegen.wineme.come_in.thumbnailer.util.Platform;
import de.uni_siegen.wineme.come_in.thumbnailer.util.TemporaryFilesManager;
import de.uni_siegen.wineme.come_in.thumbnailer.util.mime.MimeTypeDetector;

/**
 * This generates a thumbnail of Office-Files by converting them into the OpenOffice-Format first.
 * Performance note: this may take several seconds per file. The first time this Thumbnailer is
 * called, OpenOffice is started. Depends on: <li>OpenOffice 3 / LibreOffice <li>JODConverter 3
 * beta5 or higher <li>Aperture Core (MIME Type detection) <li>OpenOfficeThumbnailer
 *
 * @TODO Be stricter about which kind of files to process. Currently it converts just like
 *       everything. (See tests/ThumbnailersFailingTest)
 * @author Benjamin
 */
public abstract class JODConverterThumbnailer extends AbstractThumbnailer {

   static final String TEMP_FILE = "jodtemp";

  /** The logger for this class */
   protected static Logger mLog = Logger.getLogger(JODConverterThumbnailer.class);

   /**
    * JOD Office Manager
    */
   protected static OfficeManager officeManager = null;

   /**
    * JOD Converter
    */
   protected static OfficeDocumentConverter officeConverter = null;

   /**
    * Thumbnail Extractor for OpenOffice Files
    */
   protected OpenOfficeThumbnailer ooo_thumbnailer = null;

   /**
    * MimeIdentification
    */
   protected MimeTypeDetector mimeTypeDetector = null;

   private TemporaryFilesManager temporaryFilesManager = null;

   /**
    * OpenOffice Home Folder (Configurable)
    */
   private static String openOfficeHomeFolder = null;

   /**
    * The Port on which to connect (must be unoccupied)
    */
   private final static int OOO_DEFAULT_PORT = 8100;

   /**
    * OpenOffice Home Folder (Configurable)
    */
   private static int openOfficePort = JODConverterThumbnailer.OOO_DEFAULT_PORT;

   /**
    * How long may a conversion take? (in ms)
    */
   private static final long JOD_DOCUMENT_TIMEOUT = 120000;



   protected static File openOfficeTemplateProfileDir = null;


   public JODConverterThumbnailer() {
      this.ooo_thumbnailer = new OpenOfficeThumbnailer();
      this.mimeTypeDetector = new MimeTypeDetector();
      this.temporaryFilesManager = new TemporaryFilesManager();
   }




   public static void setOpenOfficePort(final int openOfficePort) {
      if (openOfficePort > 0) {
         JODConverterThumbnailer.openOfficePort = openOfficePort;
      }
   }

   public static void setOpenOfficeHomeFolder(final String openOfficeHomeFolder) {
      JODConverterThumbnailer.openOfficeHomeFolder = openOfficeHomeFolder;
   }

   public static void setOpenOfficeProfileFolder(final String paramOpenOfficeProfile) {
      if (paramOpenOfficeProfile != null) {
         JODConverterThumbnailer.openOfficeTemplateProfileDir = new File(paramOpenOfficeProfile);
      }
   }

   /**
    * Start OpenOffice-Service and connect to it. (Does not reconnect if already connected.)
    */
   public static void connect() {
      connect(false);
   }

   /**
    * Start OpenOffice-Service and connect to it.
    *
    * @param forceReconnect
    *           Connect even if he is already connected.
    */
   public static void connect(final boolean forceReconnect) {
      if (!forceReconnect && isConnected()) {
         return;
      }

      final DefaultOfficeManagerConfiguration config =
            new DefaultOfficeManagerConfiguration().setPortNumber(
                  JODConverterThumbnailer.openOfficePort).setTaskExecutionTimeout(
                  JODConverterThumbnailer.JOD_DOCUMENT_TIMEOUT);

      if (JODConverterThumbnailer.openOfficeHomeFolder != null) {
         config.setOfficeHome(JODConverterThumbnailer.openOfficeHomeFolder);
      }

      if (JODConverterThumbnailer.openOfficeTemplateProfileDir != null) {
         if (JODConverterThumbnailer.openOfficeTemplateProfileDir.exists()) {
            config.setTemplateProfileDir(JODConverterThumbnailer.openOfficeTemplateProfileDir);
         } else {
            JODConverterThumbnailer.mLog.info("No Template Profile Folder found at " + JODConverterThumbnailer.openOfficeTemplateProfileDir.getAbsolutePath()
                  + " - Creating temporary one.");
         }
      } else {
         JODConverterThumbnailer.mLog.info("Creating temporary profile folder...");
      }

      JODConverterThumbnailer.officeManager = config.buildOfficeManager();
      JODConverterThumbnailer.officeManager.start();

      JODConverterThumbnailer.officeConverter =
            new OfficeDocumentConverter(JODConverterThumbnailer.officeManager);
   }

   /**
    * Check if a connection to OpenOffice is established.
    *
    * @return True if connected.
    */
   public static boolean isConnected() {
      return JODConverterThumbnailer.officeManager != null && JODConverterThumbnailer.officeManager.isRunning();
   }

   /**
    * Stop the OpenOffice Process and disconnect.
    */
   public static void disconnect() {
      // close the connection
      if (JODConverterThumbnailer.officeManager != null) {
         JODConverterThumbnailer.officeManager.stop();
      }
      JODConverterThumbnailer.officeManager = null;
   }

   @Override
   public void close() throws IOException {
      try {
         try {
            this.temporaryFilesManager.deleteAllTempfiles();
            this.ooo_thumbnailer.close();
         } finally {
            disconnect();
         }
      } finally {
         super.close();
      }
   }

   /**
    * Generates a thumbnail of Office files.
    *
    * @param input
    *           Input file that should be processed
    * @param output
    *           File in which should be written
    * @throws IOException
    *            If file cannot be read/written
    * @throws ThumbnailerException
    *            If the thumbnailing process failed.
    */
   @Override
   public void generateThumbnail(final File input, final File output) throws IOException, ThumbnailerException {
      this.checkConnecton();
      File outputTmp = null;
      try {
         outputTmp = this.convertToOpenOfficeFile(input);
         this.ooo_thumbnailer.generateThumbnail(outputTmp, output);
      } finally {
         IOUtil.deleteQuietlyForce(outputTmp);
      }
   }


  private File convertToOpenOfficeFile(final File input) throws IOException, ThumbnailerException {
    final File outputTmp = File.createTempFile(JODConverterThumbnailer.TEMP_FILE, "." + this.getStandardOpenOfficeExtension());
    final File checkedInput = this.checkInputPath(input);

     try {
         JODConverterThumbnailer.officeConverter.convert(checkedInput, outputTmp);
     } catch (final OfficeException e) {
        throw new ThumbnailerException("Could not convert into OpenOffice-File", e);
     }
     if (outputTmp.length() == 0) {
        throw new ThumbnailerException("Could not convert into OpenOffice-File (file was empty)...");
     }
    return outputTmp;
  }




  private File checkInputPath(File input) {
    // Naughty hack to circumvent invalid URLs under windows (C:\\ ...)
     if (Platform.isWindows()) {
        input = new File(input.getAbsolutePath().replace("\\\\", "\\"));
     }
    return input;
  }


  private File convertToPdf(final File input) throws ThumbnailerException, IOException {
    final File tempFile = File.createTempFile(JODConverterThumbnailer.TEMP_FILE, ".pdf");
    final File checkedInput = this.checkInputPath(input);
    try {
      final DocumentFormat format = JODConverterThumbnailer.officeConverter.getFormatRegistry().getFormatByExtension("pdf");
      JODConverterThumbnailer.officeConverter.convert(checkedInput, tempFile, format);
    } catch (final OfficeException e) {
      throw new ThumbnailerException("Could not convert into PDF file", e);
    }
    if (tempFile.length() == 0) {
      throw new ThumbnailerException("Could not convert into PDF file (file was empty)");
    }
    return tempFile;
  }


  private void checkConnecton() {
    // Connect on first use
    if (!isConnected()) {
       connect();
    }
  }


   /**
    * Generate a Thumbnail of the input file. (Fix file ending according to MIME-Type).
    *
    * @param input
    *           Input file that should be processed
    * @param output
    *           File in which should be written
    * @param mimeType
    *           MIME-Type of input file (null if unknown)
    * @throws IOException
    *            If file cannot be read/written
    * @throws ThumbnailerException
    *            If the thumbnailing process failed.
    */
   @Override
   public void generateThumbnail(final File input, final File output, final String mimeType) throws IOException, ThumbnailerException {
      final File checkedInput = this.checkExtensionForMimeType(input, mimeType);
      this.generateThumbnail(checkedInput, output);
   }


   @Override
   public void generateThumbnails(final File input, final File outputFolder, final String mimeType) throws IOException, ThumbnailerException {
     final File checkedInput = this.checkExtensionForMimeType(input, mimeType);
     this.generateThumbnails(checkedInput, outputFolder);
   }


   @Override
   public void generateThumbnails(final File input, final File outputFolder) throws IOException, ThumbnailerException {
     this.checkConnecton();
     File tempPdfFile = null;
     PDFBoxThumbnailer pdfNailer = null;
     try {
        tempPdfFile = this.convertToPdf(input);
        // invoke the converter for PDF files
        pdfNailer = new PDFBoxThumbnailer();
        pdfNailer.setImageSize(this.thumbWidth, this.thumbHeight, this.imageResizeOptions);
        pdfNailer.generateThumbnails(tempPdfFile, outputFolder);
     } finally {
       if (pdfNailer != null) {
         pdfNailer.close();
       }
        IOUtil.deleteQuietlyForce(tempPdfFile);
     }
   }


  private File checkExtensionForMimeType(File input, final String mimeType) throws IOException {
    final String ext = FilenameUtils.getExtension(input.getName());
    if (!this.mimeTypeDetector.doesExtensionMatchMimeType(ext, mimeType)) {
       String newExt;
       if ("application/zip".equals(mimeType)) {
          newExt = this.getStandardZipExtension();
       } else if ("application/vnd.ms-office".equals(mimeType)) {
          newExt = this.getStandardOfficeExtension();
       } else {
          newExt = this.mimeTypeDetector.getStandardExtensionForMimeType(mimeType);
       }

       input = this.temporaryFilesManager.createTempfileCopy(input, newExt);
    }
    return input;
  }

   protected abstract String getStandardZipExtension();

   protected abstract String getStandardOfficeExtension();

   protected abstract String getStandardOpenOfficeExtension();

   @Override
   public void setImageSize(final int thumbWidth, final int thumbHeight,
         final int imageResizeOptions) {
      super.setImageSize(thumbWidth, thumbHeight, imageResizeOptions);
      this.ooo_thumbnailer.setImageSize(thumbWidth, thumbHeight, imageResizeOptions);
   }
}
