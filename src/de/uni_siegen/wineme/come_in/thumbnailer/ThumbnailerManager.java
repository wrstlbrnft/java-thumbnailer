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

package de.uni_siegen.wineme.come_in.thumbnailer;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.Logger;

import de.uni_siegen.wineme.come_in.thumbnailer.thumbnailers.Thumbnailer;
import de.uni_siegen.wineme.come_in.thumbnailer.util.ChainedHashMap;
import de.uni_siegen.wineme.come_in.thumbnailer.util.IOUtil;
import de.uni_siegen.wineme.come_in.thumbnailer.util.StringUtil;
import de.uni_siegen.wineme.come_in.thumbnailer.util.mime.MimeTypeDetector;

/**
 * This class manages all available Thumbnailers.
 * Its purpose is to delegate a File to the appropriate Thumbnailer in order to get a Thumbnail of it.
 * This is done in a fall-through manner: If several Thumbnailer can handle a specific filetype,
 * all are tried until a Thumbnail could be created.
 *
 * Fill this class with available Thumbnailers via the registerThumbnailer()-Method.
 * Then call generateThumbnail().
 *
 * @author Benjamin
 */
public class ThumbnailerManager implements Thumbnailer, ThumbnailerConstants {

	/**
	 * @var Starting estimate of the number of mime types that the thumbnailer can manager
	 */
	private static final int DEFAULT_NB_MIME_TYPES = 40;
	/**
	 * @var Starting estimate of the number of thumbnailers per mime type
	 */
	private static final int DEFAULT_NB_THUMBNAILERS_PER_MIME = 5;

	/**
	 * @var MIME Type for "all MIME" within thumbnailers Hash
	 */
	private static final String ALL_MIME_WILDCARD = "*/*";

	/**
	 * @var Width of thumbnail picture to create (in Pixel)
	 */
	private int thumbWidth;

	/**
	 * @var Height of thumbnail picture to create (in Pixel)
	 */
	private int thumbHeight;

	/**
	 * @var Options for image resizer (currently unused)
	 */
	private int thumbOptions = 0;

	/** Folder under which new thumbnails should be filed */
	private File thumbnailFolder;

	/** The logger for this class */
	private static Logger mLog = Logger.getLogger(ThumbnailerManager.class);


	/**
	 * Thumbnailers per MIME-Type they accept (ALL_MIME_WILDCARD for all)
	 */
	private ChainedHashMap<String, Thumbnailer> thumbnailers;

	/**
	 * All Thumbnailers.
	 */
	private Queue<Thumbnailer> allThumbnailers;

	/**
	 * Magic Mime Detection ... a wrapper class to Aperature's Mime thingies.
	 */
	private final MimeTypeDetector mimeTypeDetector;

	/**
	 * Initialise Thumbnail Manager
	 */
	public ThumbnailerManager()
	{
		// Execute close() when JVM shuts down (if it wasn't executed before).
		final ThumbnailerManager self = this;
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    @Override
         public void run() { IOUtil.quietlyClose(self); }
		});

		this.thumbnailers = new ChainedHashMap<String, Thumbnailer>(ThumbnailerManager.DEFAULT_NB_MIME_TYPES, ThumbnailerManager.DEFAULT_NB_THUMBNAILERS_PER_MIME);
		this.allThumbnailers = new LinkedList<Thumbnailer>();

		this.mimeTypeDetector = new MimeTypeDetector();

		this.thumbHeight = ThumbnailerConstants.THUMBNAIL_DEFAULT_HEIGHT;
		this.thumbWidth = ThumbnailerConstants.THUMBNAIL_DEFAULT_WIDTH;
	}
/* currently not used
	private static String generate_hash(String str)
	{
		return StringUtil.transpose_string(StringUtil.md5(str));
	}
*/
	/**
	 * Calculate a thumbnail filename (via hashing).
	 *
	 * @param input			Input file
	 * @param checkExist	If true: guarantee that such a filename doesn't exist yet
	 * @return	The chosen filename
	 */
	public File chooseThumbnailFilename(final File input, final boolean checkExist)
	{
		if (this.thumbnailFolder == null) {
         throw new RuntimeException("chooseThumbnailFilename cannot be run before a first call to setThumbnailFolder()");
      }
		if (input == null) {
         throw new NullPointerException("Input file may not be null");
      }

		final String hash = ""; //"_" + generate_hash(input.getAbsolutePath());
		final String prefix = input.getName().replace('.', '_');

		int tries = 0;
		String suffix = "";
		File output;
		do
		{
			if (tries > 0)
			{
				final int suffix_length = tries / 4 + 1; // Simple (i.e. guessed) heuristic to add randomness if many files have the same name
				suffix = "-" + StringUtil.randomString(suffix_length);
			}

			final String name = prefix + hash + suffix + ".png";
			output = new File(this.thumbnailFolder, name);

			tries++;
		}
		while (checkExist && output.exists());

		return output;
	}


	private File chooseThumbnailFolder(final File input) {
	   if (this.thumbnailFolder == null) {
         throw new RuntimeException("chooseThumbnailFilename cannot be run before a first call to setThumbnailFolder()");
      }
      if (input == null) {
         throw new NullPointerException("Input file may not be null");
      }

      final String inputFileName = input.getName();
      final int lastDotPosition = inputFileName.lastIndexOf('.');
      final String nameWithoutExtension = inputFileName.substring(0, lastDotPosition);
      final File folder = new File(this.thumbnailFolder, nameWithoutExtension + "-" + System.currentTimeMillis());
      return folder;
	}



	/**
	 * Set the folder where the thumbnails should be generated by default
	 * (if no output file is given).
	 *
	 * @param thumbnailPath			Path where the future thumbnails will be written to
	 * @throws FileDoesNotExistException	If the given path is not writeable
	 */
	public void setThumbnailFolder(final String thumbnailPath) throws FileDoesNotExistException {
		this.setThumbnailFolder(new File(thumbnailPath));
	}

	/**
	 * Set the folder where the thumbnails should be generated by default
	 * (if no output file is given).
	 *
	 * @param thumbnailPath					Path where the future thumbnails will be written to
	 * @throws FileDoesNotExistException	If the given path is not writeable
	 */
	public void setThumbnailFolder(final File thumbnailPath) throws FileDoesNotExistException {
		FileDoesNotExistException.checkWrite(thumbnailPath, "The thumbnail folder", true, true);

		this.thumbnailFolder = thumbnailPath;
	}

	/**
	 * Generate a Thumbnail.
	 * The output file name is generated using a hashing scheme.
	 * It is garantueed that an existing Thumbnail is not overwritten by this.
	 *
	 * @param 	input	Input file that should be processed.
	 * @return	Name of Thumbnail-File generated.
	 * @throws FileDoesNotExistException
	 * @throws IOException
	 * @throws ThumbnailerException
	 */
	public File createThumbnail(final File input) throws FileDoesNotExistException, IOException, ThumbnailerException
	{
		final File output = this.chooseThumbnailFilename(input, true);
		this.generateThumbnail(input, output);

		return output;
	}


	/**
	 * Generates a thumbnail for each page of the input file.
	 *
	 * @param input file that should be processed
	 * @return the folder containing the generated thumbnails
	 * @throws ThumbnailerException
	 * @throws IOException
	 */
	public File createThumbnails(final File input) throws IOException, ThumbnailerException {
	   final File outputFolder = this.chooseThumbnailFolder(input);
	   this.generateThumbnails(input, outputFolder);
	   return outputFolder;
	}


   /**
	 * Add a Thumbnailer-Class to the list of available Thumbnailers
	 * Note that the order you add Thumbnailers may make a difference:
	 * First added Thumbnailers are tried first, if one fails, the next
	 * (that claims to be able to treat such a document) is tried.
	 * (Thumbnailers that claim to treat all MIME Types are tried last, though.)
	 *
	 * @param thumbnailer	Thumbnailer to add.
	 */
	public void registerThumbnailer(final Thumbnailer thumbnailer)
	{
		final String[] acceptMIME = thumbnailer.getAcceptedMIMETypes();
		if (acceptMIME == null) {
         this.thumbnailers.put(ThumbnailerManager.ALL_MIME_WILDCARD, thumbnailer);
      } else
		{
			for (final String mime: acceptMIME) {
            this.thumbnailers.put(mime, thumbnailer);
         }
		}
		this.allThumbnailers.add(thumbnailer);

		thumbnailer.setImageSize(this.thumbWidth, this.thumbHeight, this.thumbOptions);
	}

	/**
	 * Instead of a deconstructor:
	 * De-initialize ThumbnailManager and its thumbnailers.
	 *
	 * This functions should be called before termination of the program,
	 * and Thumbnails can't be generated after calling this function.
	 */
	public void close() {
		if (this.allThumbnailers == null)
       {
         return; // Already closed
      }

		for (final Thumbnailer thumbnailer: this.allThumbnailers)
		{
			try {
				thumbnailer.close();
			} catch (final IOException e) {
				ThumbnailerManager.mLog.error("Error during close of Thumbnailer:", e);
			}
		}

		this.thumbnailers = null;
		this.allThumbnailers = null;
	}

	/**
	 * Generate a Thumbnail of the input file.
	 * Try all available Thumbnailers and use the first that returns an image.
	 *
	 * MIME-Detection narrows the selection of Thumbnailers to try:
	 * <li>First all Thumbnailers that declare to accept such a MIME Type are used
	 * <li>Then all Thumbnailers that declare to accept all possible MIME Types.
	 *
	 * @param 	input		Input file that should be processed
	 * @param 	output		File in which should be written
	 * @param	mimeType	MIME-Type of input file (null if unknown)
	 * @throws 	IOException			If file cannot be read/written.
	 * @throws ThumbnailerException If the thumbnailing process failed
	 * 								(i.e., no thumbnailer could generate an Thumbnail.
	 * 								 The last ThumbnailerException is re-thrown.)
	 */
	public void generateThumbnail(final File input, final File output, String mimeType) throws IOException, ThumbnailerException {
		FileDoesNotExistException.check(input, "The input file");
		FileDoesNotExistException.checkWrite(output, "The output file", true, false);

		boolean generated = false;

		// MIME might be known already (in case of recursive thumbnail managers)
		if (mimeType == null)
		{
			mimeType = this.mimeTypeDetector.getMimeType(input);
			ThumbnailerManager.mLog.debug("Detected Mime-Typ: " + mimeType);
		}

		if (mimeType != null) {
         generated = this.executeThumbnailers(mimeType, input, output, mimeType, true);
      }

		// Try again using wildcard thumbnailers
		if (!generated) {
         generated = this.executeThumbnailers(ThumbnailerManager.ALL_MIME_WILDCARD, input, output, mimeType, true);
      }

		if (!generated) {
         throw new ThumbnailerException("No suitable Thumbnailer has been found. (File: " + input.getName() + " ; Detected MIME: " + mimeType + ")");
      }
	}

	/**
	 * Generate a Thumbnail of the input file.
	 * Try all available Thumbnailers and use the first that returns an image.
	 *
	 * MIME-Detection narrows the selection of Thumbnailers to try:
	 * <li>First all Thumbnailers that declare to accept such a MIME Type are used
	 * <li>Then all Thumbnailers that declare to accept all possible MIME Types.
	 *
	 * @param 	input		Input file that should be processed
	 * @param 	output		File in which should be written
	 * @throws 	IOException			If file cannot be read/written.
	 * @throws ThumbnailerException If the thumbnailing process failed
	 * 								(i.e., no thumbnailer could generate an Thumbnail.
	 * 								 The last ThumbnailerException is re-thrown.)
	 */
	public void generateThumbnail(final File input, final File output) throws IOException, ThumbnailerException {
		this.generateThumbnail(input, output, null);
	}


	public void generateThumbnails(final File input, final File outputFolder) throws IOException, ThumbnailerException {
	   this.generateThumbnails(input, outputFolder, null);
	}

	public void generateThumbnails(final File input, final File outputFolder, String mimeType) throws IOException, ThumbnailerException {
      FileDoesNotExistException.check(input);

      boolean generated = false;

      // MIME might be known already (in case of recursive thumbnail managers)
      if (mimeType == null) {
         mimeType = this.mimeTypeDetector.getMimeType(input);
         ThumbnailerManager.mLog.debug("Detected MIME type: " + mimeType);
      }

      if (mimeType != null) {
         generated = this.executeThumbnailers(mimeType, input, outputFolder, mimeType, false);
      }

      // Try again using wildcard thumbnailers
      if (!generated) {
         generated = this.executeThumbnailers(ThumbnailerManager.ALL_MIME_WILDCARD, input, outputFolder, mimeType, false);
      }

      if (!generated) {
         throw new ThumbnailerException("No suitable Thumbnailer has been found. (File: " + input.getName() + " ; Detected MIME: " + mimeType + ")");
      }
   }



	/**
	 * Helper function for Thumbnail generation:
	 * execute all thumbnailers of a given MimeType.
	 *
	 *
	 * @param useMimeType		Which MIME Type the thumbnailers should be taken from
	 * @param input				Input File that should be processed
	 * @param output			Output file where the image shall be written.
	 * @param detectedMimeType	MIME Type that was returned by automatic MIME Detection
	 * @param firstPageOnly if true, creates a thumbnail for the first page of the input. Otherwise, creates a thumbnail for each page of the input.
	 * @return	True on success (1 thumbnailer could generate the output file).
	 * @throws IOException	Input file cannot be read, or output file cannot be written, or necessary temporary files could not be created.
	 */
	private boolean executeThumbnailers(final String useMimeType, final File input, final File output, final String detectedMimeType, final boolean firstPageOnly) throws IOException {
		for (final Thumbnailer thumbnailer: this.thumbnailers.getIterable(useMimeType)) {
			try {
			   if (firstPageOnly) {
			      thumbnailer.generateThumbnail(input, output, detectedMimeType);
			   } else {
			      thumbnailer.generateThumbnails(input, output);
			   }
				return true;
			} catch (final ThumbnailerException e) {
				// This Thumbnailer apparently wasn't suitable, so try next
				ThumbnailerManager.mLog.warn("Warning: " + thumbnailer.getClass().getName() + " could not handle the file " + input.getName() + " (trying next)", e);
			}
		}
		return false;
	}

	/**
	 * Set the image size of all following thumbnails.
	 *
	 * ThumbnailManager delegates this to all his containing Thumbailers.
	 */
	public void setImageSize(final int width, final int height, final int imageResizeOptions) {
		this.thumbHeight = height;
		this.thumbWidth = width;
		this.thumbOptions = imageResizeOptions;

		if (this.thumbWidth < 0) {
         this.thumbWidth = 0;
      }
		if (this.thumbHeight < 0) {
         this.thumbHeight = 0;
      }

		for (final Thumbnailer thumbnailer: this.allThumbnailers) {
         thumbnailer.setImageSize(this.thumbWidth, this.thumbHeight, this.thumbOptions);
      }
	}

	/**
	 * Get the currently set Image Width of this Thumbnailer.
	 * @return	image width of created thumbnails.
	 */
	public int getCurrentImageWidth()
	{
		return this.thumbWidth;
	}

	/**
	 * Get the currently set Image Height of this Thumbnailer.
	 * @return	image height of created thumbnails.
	 */
	public int getCurrentImageHeight()
	{
		return this.thumbHeight;
	}


	/**
	 * Summarize all contained MIME Type Thumbnailers.
	 * @return All accepted MIME Types, null if any.
	 */
	public String[] getAcceptedMIMETypes() {
		if (this.thumbnailers.containsKey(ThumbnailerManager.ALL_MIME_WILDCARD)) {
         return null; // All MIME Types
      } else {
         return this.thumbnailers.keySet().toArray(new String[]{});
      }
	}

}
