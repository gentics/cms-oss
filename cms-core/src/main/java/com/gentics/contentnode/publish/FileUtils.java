 /*
 * @author herbert
 * @date 19.03.2007
 * @version $Id: FileUtils.java,v 1.3 2007-11-13 10:03:41 norbert Exp $
 */
package com.gentics.contentnode.publish;

import java.io.File;
import java.io.IOException;

/**
 * Simple interface which is responsible for various 
 * platform dependent file operations
 * 
 * Currently copy and symlink only.
 * 
 * Implementations should have a public default constructor.
 * 
 * @author herbert
 */
public interface FileUtils {
    
	/**
	 * This method should return if an implementation supports
	 * symlinks AND hardlinks.
	 * This method should return true if it is certain that
	 * {@link #createSymlink(File, File)} does not throw a
	 * {@link UnsupportedOperationException}
	 * @return true if symlinks are supported
	 */
	public boolean supportsSymlinks();
    
	/**
	 * Creates a symlink between src and dest
	 * @param src source file
	 * @param dest destination path (including file name)
	 * @return true if everything succeeded.
	 * @throws IOException 
	 * @throws UnsupportedOperationException if the implementation does not support creating symlinks. - copy should be used instead.
	 */
	public boolean createSymlink(File src, File dest) throws IOException;
    
	/**
	 * Creates a hardlink between src and dest
	 * @param src source file
	 * @param dest destination path (including file name)
	 * @return true if everything succeeded.
	 * @throws IOException 
	 * @throws UnsupportedOperationException if the implementation does not support creating hardlinks. - copy should be used instead.
	 */
	public boolean createLink(File src, File dest) throws IOException;
    
	/**
	 * Copies file src to dest.
	 * @param src source file
	 * @param dest destination path (including file name)
	 * @return true if everything succeeded.
	 * @throws IOException 
	 */
	public boolean createCopy(File src, File dest) throws IOException;

	/**
	 * Deletes a directory recursively.
	 * @param directory
	 * @return true on success / false on failure.
	 * @throws IOException 
	 */
	public boolean deleteDirectory(File directory) throws IOException;
}
