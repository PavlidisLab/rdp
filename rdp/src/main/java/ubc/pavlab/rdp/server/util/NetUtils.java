/*
 * The rdp project
 * 
 * Copyright (c) 2014 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubc.pavlab.rdp.server.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

/**
 * @author pavlidis
 * @version $Id: NetUtils.java,v 1.15 2013/08/20 19:49:03 paul Exp $
 */
public class NetUtils {

    private static Log log = LogFactory.getLog( NetUtils.class );

    /**
     * determine if a file exists on the remote server.
     * 
     * @param f
     * @param seekFile
     * @return the size of the file
     * @throws FileNotFoundException if the file does not exist.
     * @throws IOException on other IO errors.
     */
    public static long checkForFile( FTPClient f, String seekFile ) throws IOException {
        f.enterLocalPassiveMode();
        FTPFile[] allfilesInGroup = f.listFiles( seekFile );
        if ( allfilesInGroup == null || allfilesInGroup.length == 0 ) {
            throw new FileNotFoundException( "File " + seekFile + " does not seem to exist on the remote host" );
        }
        return allfilesInGroup[0].getSize();
    }

    /**
     * Convenient method to get a FTP connection.
     * 
     * @param host
     * @param login
     * @param password
     * @param mode
     * @return
     * @throws SocketException
     * @throws IOException
     */
    public static FTPClient connect( int mode, String host, String loginName, String password ) throws SocketException,
            IOException {
        FTPClient f = new FTPClient();
        f.enterLocalPassiveMode();
        f.setBufferSize( 32 * 2 ^ 20 );
        boolean success = false;
        f.connect( host );
        int reply = f.getReplyCode();
        if ( FTPReply.isPositiveCompletion( reply ) ) success = f.login( loginName, password );
        if ( !success ) {
            f.disconnect();
            throw new IOException( "Couldn't connect to " + host );
        }
        f.setFileType( mode );
        log.debug( "Connected to " + host );
        return f;
    }

    /**
     * @param f
     * @param seekFile
     * @param outputFile
     * @param force
     * @return boolean indicating success or failure.
     * @throws IOException
     */
    public static boolean ftpDownloadFile( FTPClient f, String seekFile, File outputFile, boolean force )
            throws IOException {
        boolean success = false;

        assert f != null && f.isConnected() : "No FTP connection is available";
        f.enterLocalPassiveMode();

        long expectedSize = checkForFile( f, seekFile );

        if ( outputFile.exists() && outputFile.length() == expectedSize && !force ) {
            log.info( "Output file " + outputFile + " already exists with correct size. Will not re-download" );
            return true;
        }

        OutputStream os = new FileOutputStream( outputFile );

        log.debug( "Seeking file " + seekFile + " with size " + expectedSize + " bytes" );
        success = f.retrieveFile( seekFile, os );
        os.close();
        if ( !success ) {
            throw new IOException( "Failed to complete download of " + seekFile );
        }
        return success;
    }

    /**
     * @param f
     * @param seekFile
     * @param outputFileName
     * @param force
     * @return boolean indicating success or failure.
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static boolean ftpDownloadFile( FTPClient f, String seekFile, String outputFileName, boolean force )
            throws IOException, FileNotFoundException {
        f.enterLocalPassiveMode();
        return ftpDownloadFile( f, seekFile, new File( outputFileName ), force );
    }

    /**
     * Get the size of a remote file.
     * 
     * @param f FTPClient
     * @param seekFile
     * @return
     * @throws IOException
     */
    public static long ftpFileSize( FTPClient f, String seekFile ) throws IOException {

        if ( f == null || !f.isConnected() ) {
            throw new IOException( "No FTP connection" );
        }

        f.enterLocalPassiveMode();

        int maxTries = 3;

        for ( int i = 0; i < maxTries; i++ ) {

            FTPFile[] files = f.listFiles( seekFile );

            if ( files.length == 1 ) {
                return files[0].getSize();
            } else if ( files.length > 1 ) {
                throw new IOException( files.length + " files found when expecting one" );
            } // otherwise keep trying.
        }

        throw new FileNotFoundException( "Didn't get expected file information for " + seekFile + " (" + maxTries
                + " attempts)" );

    }

}
