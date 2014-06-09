package org.apache.maven.plugins.site;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import java.io.InputStream;

import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;

/**
 * Base class for site mojos.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractSiteMojo
    extends AbstractMojo
{
    /**
     * A comma separated list of locales supported by Maven. The first valid token will be the default Locale
     * for this instance of the Java Virtual Machine.
     *
     * @since 2.3
     */
    @Parameter( property = "locales" )
    protected String locales;

    /**
     * SiteTool.
     */
    @Component
    protected SiteTool siteTool;

    /**
     * Internationalization.
     */
    @Component
    protected I18N i18n;

    /**
     * Directory containing the site.xml file and the source for apt, fml and xdoc docs.
     *
     * @since 2.3
     */
    @Parameter( defaultValue = "${basedir}/src/site" )
    protected File siteDirectory;

    /**
     * The maven project.
     */
    @Parameter( defaultValue = "${project}", readonly = true )
    protected MavenProject project;

    /**
     * The local repository.
     */
    @Parameter( defaultValue = "${localRepository}", readonly = true )
    protected ArtifactRepository localRepository;

    /**
     * The reactor projects.
     */
    @Parameter( defaultValue = "${reactorProjects}", required = true, readonly = true )
    protected List<MavenProject> reactorProjects;

    /**
     * Specifies the input encoding.
     *
     * @since 2.3
     */
    @Parameter( property = "encoding", defaultValue = "${project.build.sourceEncoding}" )
    private String inputEncoding;

    /**
     * Specifies the output encoding.
     *
     * @since 2.3
     */
    @Parameter( property = "outputEncoding", defaultValue = "${project.reporting.outputEncoding}" )
    private String outputEncoding;

    /**
     * Gets the input files encoding.
     *
     * @return The input files encoding, never <code>null</code>.
     */
    protected String getInputEncoding()
    {
        return ( inputEncoding == null ) ? ReaderFactory.ISO_8859_1 : inputEncoding;
    }

    /**
     * Gets the effective reporting output files encoding.
     *
     * @return The effective reporting output file encoding, never <code>null</code>.
     */
    protected String getOutputEncoding()
    {
        return ( outputEncoding == null ) ? ReaderFactory.UTF_8 : outputEncoding;
    }

    /**
     * Check the current Maven version to see if it's Maven 3.0 or newer.
     */
    protected static boolean isMaven3OrMore()
    {
        return new ComparableVersion( getMavenVersion() ).compareTo( new ComparableVersion( "3.0" ) ) >= 0;
    }

    protected static String getMavenVersion()
    {
        // This relies on the fact that MavenProject is the in core classloader
        // and that the core classloader is for the maven-core artifact
        // and that should have a pom.properties file
        // if this ever changes, we will have to revisit this code.
        final Properties properties = new Properties();
        final InputStream in =
            MavenProject.class.getClassLoader().getResourceAsStream( "META-INF/maven/org.apache.maven/maven-core/pom.properties" );
        try
        {
            properties.load( in );
        }
        catch ( IOException ioe )
        {
            return "";
        }
        finally
        {
            IOUtil.close( in );
        }

        return properties.getProperty( "version" ).trim();
    }
}
