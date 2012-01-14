package org.apache.maven.plugin.pmd;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Base class for mojos that check if there were any PMD violations.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public abstract class AbstractPmdViolationCheckMojo
    extends AbstractMojo
{
    private static final Boolean FAILURES_KEY = Boolean.TRUE;

    private static final Boolean WARNINGS_KEY = Boolean.FALSE;

    /**
     * The location of the XML report to check, as generated by the PMD report.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File targetDirectory;

    /**
     * Whether to fail the build if the validation check fails.
     *
     * @parameter expression="${pmd.failOnViolation}" default-value="true"
     * @required
     */
    private boolean failOnViolation;

    /**
     * The project language, for determining whether to run the report.
     *
     * @parameter expression="${project.artifact.artifactHandler.language}"
     * @required
     * @readonly
     */
    private String language;

    /**
     * Whether to build an aggregated report at the root, or build individual reports.
     *
     * @parameter expression="${aggregate}" default-value="false"
     * @since 2.2
     */
    protected boolean aggregate;

    /**
     * Print details of check failures to build output.
     *
     * @parameter expression="${pmd.verbose}" default-value="false"
     */
    private boolean verbose;

    /**
     * The project to analyze.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    protected void executeCheck( String filename, String tagName, String key, int failurePriority )
        throws MojoFailureException, MojoExecutionException
    {
        if ( aggregate && !project.isExecutionRoot() )
        {
            return;
        }

        if ( "java".equals( language ) || aggregate )
        {
            File outputFile = new File( targetDirectory, filename );

            if ( outputFile.exists() )
            {
                Reader reader = null;
                try
                {
                    XmlPullParser xpp = new MXParser();
                    reader = ReaderFactory.newXmlReader( outputFile );
                    xpp.setInput( reader );

                    Map<Boolean, List<Map<String, String>>> violations = getViolations( xpp, tagName, failurePriority );

                    List<Map<String, String>> failures = violations.get( FAILURES_KEY );
                    List<Map<String, String>> warnings = violations.get( WARNINGS_KEY );

                    if ( verbose )
                    {
                        printErrors( failures, warnings );
                    }

                    int failureCount = failures.size();
                    int warningCount = warnings.size();

                    String message = getMessage( failureCount, warningCount, key, outputFile );

                    if ( failureCount > 0 && failOnViolation )
                    {
                        throw new MojoFailureException( message );
                    }

                    this.getLog().info( message );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException(
                                                      "Unable to read PMD results xml: " + outputFile.getAbsolutePath(),
                                                      e );
                }
                catch ( XmlPullParserException e )
                {
                    throw new MojoExecutionException(
                                                      "Unable to read PMD results xml: " + outputFile.getAbsolutePath(),
                                                      e );
                }
                finally
                {
                    IOUtil.close( reader );
                }
            }
            else
            {
                throw new MojoFailureException( "Unable to perform check, " + "unable to find " + outputFile );
            }
        }
    }

    /**
     * Method for collecting the violations found by the PMD tool
     *
     * @param xpp
     *            the xml parser object
     * @param tagName
     *            the element that will be checked
     * @return an int that specifies the number of violations found
     * @throws XmlPullParserException
     * @throws IOException
     */
    private Map<Boolean, List<Map<String, String>>> getViolations( XmlPullParser xpp, String tagName, int failurePriority )
        throws XmlPullParserException, IOException
    {
        int eventType = xpp.getEventType();

        List<Map<String, String>> failures = new ArrayList<Map<String, String>>();
        List<Map<String, String>> warnings = new ArrayList<Map<String, String>>();

        String fullpath = null;

        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG && "file".equals( xpp.getName() ) )
            {
                fullpath = xpp.getAttributeValue( "", "name" );
            }
            if ( eventType == XmlPullParser.START_TAG && tagName.equals( xpp.getName() ) )
            {
                Map<String, String> details = getErrorDetails( xpp );

                if ( fullpath != null )
                {
                    details.put( "filename", getFilename( fullpath, details.get( "package" ) ) );
                }

                try
                {
                    int priority = Integer.parseInt( details.get( "priority" ) );
                    if ( priority <= failurePriority )
                    {
                        failures.add( details );
                    }
                    else
                    {
                        warnings.add( details );
                    }
                }
                catch ( NumberFormatException e )
                {
                    // I don't know what priority this is. Treat it like a
                    // failure
                    failures.add( details );
                }
                catch ( NullPointerException e )
                {
                    // I don't know what priority this is. Treat it like a
                    // failure
                    failures.add( details );
                }

            }

            eventType = xpp.next();
        }

        Map<Boolean, List<Map<String, String>>> map = new HashMap<Boolean, List<Map<String, String>>>( 2 );
        map.put( FAILURES_KEY, failures );
        map.put( WARNINGS_KEY, warnings );
        return map;
    }

    private String getFilename( String fullpath, String pkg )
    {
        int index = fullpath.lastIndexOf( File.separatorChar );

        while ( StringUtils.isNotEmpty( pkg ) )
        {
            index = fullpath.substring( 0, index ).lastIndexOf( File.separatorChar );

            int dot = pkg.indexOf( '.' );

            if ( dot < 0 )
            {
                break;
            }
            pkg = pkg.substring( dot + 1 );
        }

        return fullpath.substring( index + 1 );
    }

    /**
     * Prints the warnings and failures
     *
     * @param failures
     *            list of failures
     * @param warnings
     *            list of warnings
     */
    protected void printErrors( List<Map<String, String>> failures, List<Map<String, String>> warnings )
    {
        for ( Map<String, String> warning :  warnings )
        {
            printError( warning, "Warning" );
        }

        for ( Map<String, String> failure : failures )
        {
            printError( failure, "Failure" );
        }
    }

    /**
     * Gets the output message
     *
     * @param failureCount
     * @param warningCount
     * @param key
     * @param outputFile
     * @return
     */
    private String getMessage( int failureCount, int warningCount, String key, File outputFile )
    {
        StringBuffer message = new StringBuffer();
        if ( failureCount > 0 || warningCount > 0 )
        {
            if ( failureCount > 0 )
            {
                message.append( "You have " + failureCount + " " + key + ( failureCount > 1 ? "s" : "" ) );
            }

            if ( warningCount > 0 )
            {
                if ( failureCount > 0 )
                {
                    message.append( " and " );
                }
                else
                {
                    message.append( "You have " );
                }
                message.append( warningCount + " warning" + ( warningCount > 1 ? "s" : "" ) );
            }

            message.append( ". For more details see:" ).append( outputFile.getAbsolutePath() );
        }
        return message.toString();
    }

    /**
     * Formats the failure details and prints them as an INFO message
     *
     * @param item
     */
    protected abstract void printError( Map<String, String> item, String severity );

    /**
     * Gets the attributes and text for the violation tag and puts them in a
     * HashMap
     *
     * @param xpp
     * @throws XmlPullParserException
     * @throws IOException
     */
    protected abstract Map<String, String> getErrorDetails( XmlPullParser xpp )
        throws XmlPullParserException, IOException;
}