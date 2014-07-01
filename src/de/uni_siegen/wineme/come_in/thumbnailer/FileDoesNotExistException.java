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

public class FileDoesNotExistException extends IOException {

	private static final long serialVersionUID = -8622959058435621680L;

	public FileDoesNotExistException() {
	}

	public FileDoesNotExistException(final String arg0) {
		super(arg0);
	}

	public FileDoesNotExistException(final Throwable arg0) {
		super(arg0);
	}

	public FileDoesNotExistException(final String arg0, final Throwable arg1) {
		super(arg0, arg1);
	}

	/**
	 * Test if the file/directory exists and can be read.
	 * @param f		File to be tested
	 * @param label	How the file is called.
	 * @throws FileDoesNotExistException	if it does not exists or cannot be read.
	 */
	public static void check(final File f, String label) throws FileDoesNotExistException
	{
		if (label == null) {
			label = "The file";
		}

		if (f == null) {
			throw new FileDoesNotExistException(label + " does not exist (is null).");
		}

		if (!f.isFile() && !f.isDirectory()) {
			throw new FileDoesNotExistException(label + " is not a file or directory: "+ f.getAbsolutePath());
		}

		if (!f.exists()) {
			throw new FileDoesNotExistException(label + " does not exist: "+ f.getAbsolutePath());
		}

		if (!f.canRead()) {
			throw new FileDoesNotExistException(label + " cannot be read: "+ f.getAbsolutePath());
		}

		if (f.length() == 0) {
         throw new FileDoesNotExistException(label + " is empty");
      }
	}

	public static void check(final File f) throws FileDoesNotExistException
	{
		check(f, null);
	}

	public static void checkWrite(final File f, String label, final boolean createParentDirsIfNotExist, final boolean isDir) throws FileDoesNotExistException {
		if (label == null) {
         label = "The file";
      }
		if (f == null) {
         throw new FileDoesNotExistException(label + " does not exist (is null).");
      }

		if (createParentDirsIfNotExist)
		{
			boolean ret;
			if (isDir)
			{
				ret = f.mkdirs();
				if (!ret && !f.exists()) {
               throw new FileDoesNotExistException("Not all directories could be created: " + f.getAbsolutePath());
            }
			}
			else
			{
				final File parent = f.getParentFile();
				if (parent != null) // No idea why it returns null sometimes
				{
					ret = parent.mkdirs();
					if (!ret && !parent.exists()) {
                  throw new FileDoesNotExistException("Not all parent directories could be created: " + f.getAbsolutePath());
               }
				}
			}
		}

		if (isDir)
		{
			if (!f.canWrite()) {
            throw new FileDoesNotExistException(label + " cannot be written/modified: " + f.getAbsolutePath());
         }
		}
		else
		{
			try {
				f.createNewFile();
				f.delete();
			} catch (final IOException e) {
				throw new FileDoesNotExistException(label + " ist nicht beschreibbar: " + f.getAbsolutePath());
			}
		}
	}
	public static void checkWrite(final File f, final String label) throws FileDoesNotExistException {
		checkWrite(f, label, false, false);
	}
	public static void checkWrite(final File f) throws FileDoesNotExistException {
		checkWrite(f, null);
	}


}
