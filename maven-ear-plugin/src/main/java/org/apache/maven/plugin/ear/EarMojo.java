package org.apache.maven.plugin.ear;

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

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.ear.util.EarMavenArchiver;
import org.apache.maven.plugin.ear.util.JavaEEVersion;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.Manifest.Attribute;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipException;

/**
 * Builds J2EE Enterprise Archive (EAR) files.
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
@Mojo( name = "ear", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST )
public class EarMojo
    extends AbstractEarMojo
{
    private static final String[] EMPTY_STRING_ARRAY = {};

    /**
     * Single directory for extra files to include in the EAR.
     */
    @Parameter( defaultValue = "${basedir}/src/main/application", required = true )
    private File earSourceDirectory;

    /**
     * The comma separated list of tokens to include in the EAR.
     */
    @Parameter( alias = "includes", defaultValue = "**" )
    private String earSourceIncludes;

    /**
     * The comma separated list of tokens to exclude from the EAR.
     */
    @Parameter( alias = "excludes" )
    private String earSourceExcludes;

    /**
     * Specify that the EAR sources should be filtered.
     * 
     * @since 2.3.2
     */
    @Parameter( defaultValue = "false" )
    private boolean filtering;

    /**
     * Filters (property files) to include during the interpolation of the pom.xml.
     * 
     * @since 2.3.2
     */
    @Parameter
    private List filters;

    /**
     * A list of file extensions that should not be filtered if filtering is enabled.
     * 
     * @since 2.3.2
     */
    @Parameter
    private List nonFilteredFileExtensions;

    /**
     * To escape interpolated value with Windows path c:\foo\bar will be replaced with c:\\foo\\bar.
     * 
     * @since 2.3.2
     */
    @Parameter( property = "maven.ear.escapedBackslashesInFilePath", defaultValue = "false" )
    private boolean escapedBackslashesInFilePath;

    /**
     * Expression preceded with this String won't be interpolated \${foo} will be replaced with ${foo}.
     * 
     * @since 2.3.2
     */
    @Parameter( property = "maven.ear.escapeString" )
    protected String escapeString;

    /**
     * The location of the manifest file to be used within the EAR file. If no value if specified, the default location
     * in the workDirectory is taken. If the file does not exist, a manifest will be generated automatically.
     */
    @Parameter
    private File manifestFile;

    /**
     * The location of a custom application.xml file to be used within the EAR file.
     */
    @Parameter
    private String applicationXml;

    /**
     * The directory for the generated EAR.
     */
    @Parameter( defaultValue = "${project.build.directory}", required = true )
    private String outputDirectory;

    /**
     * The name of the EAR file to generate.
     */
    @Parameter( alias = "earName", defaultValue = "${project.build.finalName}", required = true )
    private String finalName;

    /**
     * The comma separated list of artifact's type(s) to unpack by default.
     */
    @Parameter
    private String unpackTypes;

    /**
     * Classifier to add to the artifact generated. If given, the artifact will be an attachment instead.
     */
    @Parameter
    private String classifier;

    /**
     * A comma separated list of tokens to exclude when packaging the EAR. By default nothing is excluded. Note that you
     * can use the Java Regular Expressions engine to include and exclude specific pattern using the expression
     * %regex[]. Hint: read the about (?!Pattern).
     * 
     * @since 2.7
     */
    @Parameter
    private String packagingExcludes;

    /**
     * A comma separated list of tokens to include when packaging the EAR. By default everything is included. Note that
     * you can use the Java Regular Expressions engine to include and exclude specific pattern using the expression
     * %regex[].
     * 
     * @since 2.7
     */
    @Parameter
    private String packagingIncludes;

    /**
     * Whether to create skinny WARs or not. A skinny WAR is a WAR that does not have all of its dependencies in
     * WEB-INF/lib. Instead those dependencies are shared between the WARs through the EAR.
     * 
     * @since 2.7
     */
    @Parameter( property = "maven.ear.skinnyWars", defaultValue = "false" )
    private boolean skinnyWars;

    /**
     * The Jar archiver.
     */
    @Component( role = Archiver.class, hint = "jar" )
    private JarArchiver jarArchiver;

    /**
     * The Zip archiver.
     */
    @Component( role = Archiver.class, hint = "zip" )
    private ZipArchiver zipArchiver;

    /**
     * The Zip Un archiver.
     */
    @Component( role = UnArchiver.class, hint = "zip" )
    private ZipUnArchiver zipUnArchiver;

    /**
     * The archive configuration to use. See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven
     * Archiver Reference</a>.
     */
    @Parameter
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * The archive manager.
     */
    @Component
    private ArchiverManager archiverManager;

    /**
     */
    @Component( role = MavenFileFilter.class, hint = "default" )
    private MavenFileFilter mavenFileFilter;

    /**
     */
    @Component( role = MavenResourcesFiltering.class, hint = "default" )
    private MavenResourcesFiltering mavenResourcesFiltering;

    /**
     * @since 2.3.2
     */
    @Component
    private MavenSession session;

    private List filterWrappers;

    /**
     * @since 2.9
     */
    @Parameter( property = "maven.ear.useJvmChmod", defaultValue = "true" )
    private boolean useJvmChmod = true;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        // Initializes ear modules
        super.execute();

        zipArchiver.setUseJvmChmod( useJvmChmod );
        zipUnArchiver.setUseJvmChmod( useJvmChmod );

        final JavaEEVersion javaEEVersion = JavaEEVersion.getJavaEEVersion( version );

        // Initializes unpack types
        List<String> unpackTypesList = new ArrayList<String>();
        if ( unpackTypes != null )
        {
            unpackTypesList = Arrays.asList( unpackTypes.split( "," ) );
            for ( String type : unpackTypesList )
            {
                if ( !EarModuleFactory.standardArtifactTypes.contains( type ) )
                {
                    throw new MojoExecutionException( "Invalid type [" + type + "] supported types are "
                        + EarModuleFactory.standardArtifactTypes );
                }
            }
            getLog().debug( "Initialized unpack types " + unpackTypesList );
        }

        // Copy modules
        try
        {
            for ( EarModule module : getModules() )
            {
                if ( module instanceof JavaModule )
                {
                    getLog().warn( "JavaModule is deprecated (" + module + "), please use JarModule instead." );
                }
                if ( module instanceof Ejb3Module )
                {
                    getLog().warn( "Ejb3Module is deprecated (" + module + "), please use EjbModule instead." );
                }
                final File sourceFile = module.getArtifact().getFile();
                final File destinationFile = buildDestinationFile( getWorkDirectory(), module.getUri() );
                if ( !sourceFile.isFile() )
                {
                    throw new MojoExecutionException( "Cannot copy a directory: " + sourceFile.getAbsolutePath()
                        + "; Did you package/install " + module.getArtifact() + "?" );
                }

                if ( destinationFile.getCanonicalPath().equals( sourceFile.getCanonicalPath() ) )
                {
                    getLog().info( "Skipping artifact [" + module + "], as it already exists at [" + module.getUri()
                                       + "]" );
                    continue;
                }

                // If the module is within the unpack list, make sure that no unpack wasn't forced (null or true)
                // If the module is not in the unpack list, it should be true
                if ( ( unpackTypesList.contains( module.getType() ) && ( module.shouldUnpack() == null || module.shouldUnpack() ) )
                    || ( module.shouldUnpack() != null && module.shouldUnpack() ) )
                {
                    getLog().info( "Copying artifact [" + module + "] to [" + module.getUri() + "] (unpacked)" );
                    // Make sure that the destination is a directory to avoid plexus nasty stuff :)
                    destinationFile.mkdirs();
                    unpack( sourceFile, destinationFile );

                    if ( skinnyWars && module.changeManifestClasspath() )
                    {
                        changeManifestClasspath( module, destinationFile );
                    }
                }
                else
                {
                    if ( sourceFile.lastModified() > destinationFile.lastModified() )
                    {
                        getLog().info( "Copying artifact [" + module + "] to [" + module.getUri() + "]" );
                        FileUtils.copyFile( sourceFile, destinationFile );

                        if ( skinnyWars && module.changeManifestClasspath() )
                        {
                            changeManifestClasspath( module, destinationFile );
                        }
                    }
                    else
                    {
                        getLog().debug( "Skipping artifact [" + module + "], as it is already up to date at ["
                                            + module.getUri() + "]" );
                    }
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying EAR modules", e );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Error unpacking EAR modules", e );
        }
        catch ( NoSuchArchiverException e )
        {
            throw new MojoExecutionException( "No Archiver found for EAR modules", e );
        }

        // Copy source files
        try
        {
            File earSourceDir = earSourceDirectory;
            if ( earSourceDir.exists() )
            {
                getLog().info( "Copy ear sources to " + getWorkDirectory().getAbsolutePath() );
                String[] fileNames = getEarFiles( earSourceDir );
                for ( String fileName : fileNames )
                {
                    copyFile( new File( earSourceDir, fileName ), new File( getWorkDirectory(), fileName ) );
                }
            }

            if ( applicationXml != null && !"".equals( applicationXml ) )
            {
                // rename to application.xml
                getLog().info( "Including custom application.xml[" + applicationXml + "]" );
                File metaInfDir = new File( getWorkDirectory(), META_INF );
                copyFile( new File( applicationXml ), new File( metaInfDir, "/application.xml" ) );
            }

        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying EAR sources", e );
        }
        catch ( MavenFilteringException e )
        {
            throw new MojoExecutionException( "Error filtering EAR sources", e );
        }

        // Check if deployment descriptor is there
        File ddFile = new File( getWorkDirectory(), APPLICATION_XML_URI );
        if ( !ddFile.exists() && ( javaEEVersion.lt( JavaEEVersion.Five ) ) )
        {
            throw new MojoExecutionException( "Deployment descriptor: " + ddFile.getAbsolutePath() + " does not exist." );
        }

        try
        {
            File earFile = getEarFile( outputDirectory, finalName, classifier );
            final MavenArchiver archiver = new EarMavenArchiver( getModules() );
            final JarArchiver jarArchiver = getJarArchiver();
            getLog().debug( "Jar archiver implementation [" + jarArchiver.getClass().getName() + "]" );
            archiver.setArchiver( jarArchiver );
            archiver.setOutputFile( earFile );

            // Include custom manifest if necessary
            includeCustomManifestFile();

            getLog().debug( "Excluding " + Arrays.asList( getPackagingExcludes() ) + " from the generated EAR." );
            getLog().debug( "Including " + Arrays.asList( getPackagingIncludes() ) + " in the generated EAR." );

            archiver.getArchiver().addDirectory( getWorkDirectory(), getPackagingIncludes(), getPackagingExcludes() );
            archiver.createArchive( session, getProject(), archive );

            if ( classifier != null )
            {
                projectHelper.attachArtifact( getProject(), "ear", classifier, earFile );
            }
            else
            {
                getProject().getArtifact().setFile( earFile );
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error assembling EAR", e );
        }
    }

    public String getApplicationXml()
    {
        return applicationXml;
    }

    public void setApplicationXml( String applicationXml )
    {
        this.applicationXml = applicationXml;
    }

    /**
     * Returns a string array of the excludes to be used when assembling/copying the ear.
     * 
     * @return an array of tokens to exclude
     */
    protected String[] getExcludes()
    {
        List<String> excludeList = new ArrayList<String>( FileUtils.getDefaultExcludesAsList() );
        if ( earSourceExcludes != null && !"".equals( earSourceExcludes ) )
        {
            excludeList.addAll( Arrays.asList( StringUtils.split( earSourceExcludes, "," ) ) );
        }

        // if applicationXml is specified, omit the one in the source directory
        if ( getApplicationXml() != null && !"".equals( getApplicationXml() ) )
        {
            excludeList.add( "**/" + META_INF + "/application.xml" );
        }

        return excludeList.toArray( new String[excludeList.size()] );
    }

    /**
     * Returns a string array of the includes to be used when assembling/copying the ear.
     * 
     * @return an array of tokens to include
     */
    protected String[] getIncludes()
    {
        return StringUtils.split( StringUtils.defaultString( earSourceIncludes ), "," );
    }

    public String[] getPackagingExcludes()
    {
        if ( StringUtils.isEmpty( packagingExcludes ) )
        {
            return new String[0];
        }
        else
        {
            return StringUtils.split( packagingExcludes, "," );
        }
    }

    public void setPackagingExcludes( String packagingExcludes )
    {
        this.packagingExcludes = packagingExcludes;
    }

    public String[] getPackagingIncludes()
    {
        if ( StringUtils.isEmpty( packagingIncludes ) )
        {
            return new String[] { "**" };
        }
        else
        {
            return StringUtils.split( packagingIncludes, "," );
        }
    }

    public void setPackagingIncludes( String packagingIncludes )
    {
        this.packagingIncludes = packagingIncludes;
    }

    private static File buildDestinationFile( File buildDir, String uri )
    {
        return new File( buildDir, uri );
    }

    private void includeCustomManifestFile()
    {
        if ( manifestFile == null )
        {
            manifestFile = new File( getWorkDirectory(), "META-INF/MANIFEST.MF" );
        }

        if ( !manifestFile.exists() )
        {
            getLog().info( "Could not find manifest file: " + manifestFile + " - Generating one" );
        }
        else
        {
            getLog().info( "Including custom manifest file [" + manifestFile + "]" );
            archive.setManifestFile( manifestFile );
        }
    }

    /**
     * Returns the EAR file to generate, based on an optional classifier.
     * 
     * @param basedir the output directory
     * @param finalName the name of the ear file
     * @param classifier an optional classifier
     * @return the EAR file to generate
     */
    private static File getEarFile( String basedir, String finalName, String classifier )
    {
        if ( classifier == null )
        {
            classifier = "";
        }
        else if ( classifier.trim().length() > 0 && !classifier.startsWith( "-" ) )
        {
            classifier = "-" + classifier;
        }

        return new File( basedir, finalName + classifier + ".ear" );
    }

    /**
     * Returns a list of filenames that should be copied over to the destination directory.
     * 
     * @param sourceDir the directory to be scanned
     * @return the array of filenames, relative to the sourceDir
     */
    private String[] getEarFiles( File sourceDir )
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( sourceDir );
        scanner.setExcludes( getExcludes() );
        scanner.addDefaultExcludes();

        scanner.setIncludes( getIncludes() );

        scanner.scan();

        return scanner.getIncludedFiles();
    }

    /**
     * Unpacks the module into the EAR structure.
     * 
     * @param source File to be unpacked.
     * @param destDir Location where to put the unpacked files.
     */
    public void unpack( File source, File destDir )
        throws NoSuchArchiverException, IOException
    {
        UnArchiver unArchiver = archiverManager.getUnArchiver( "zip" );
        unArchiver.setSourceFile( source );
        unArchiver.setDestDirectory( destDir );

        // Extract the module
        unArchiver.extract();
    }

    /**
     * Returns the {@link JarArchiver} implementation used to package the EAR file.
     * <p/>
     * By default the archiver is obtained from the Plexus container.
     * 
     * @return the archiver
     */
    protected JarArchiver getJarArchiver()
    {
        return jarArchiver;
    }

    private void copyFile( File source, File target )
        throws MavenFilteringException, IOException, MojoExecutionException
    {
        if ( filtering && !isNonFilteredExtension( source.getName() ) )
        {
            // Silly that we have to do this ourselves
            if ( target.getParentFile() != null && !target.getParentFile().exists() )
            {
                target.getParentFile().mkdirs();
            }
            mavenFileFilter.copyFile( source, target, true, getFilterWrappers(), null );
        }
        else
        {
            FileUtils.copyFile( source, target );
        }
    }

    public boolean isNonFilteredExtension( String fileName )
    {
        return !mavenResourcesFiltering.filteredFileExtension( fileName, nonFilteredFileExtensions );
    }

    private List getFilterWrappers()
        throws MojoExecutionException
    {
        if ( filterWrappers == null )
        {
            try
            {
                MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution();
                mavenResourcesExecution.setEscapeString( escapeString );
                filterWrappers =
                    mavenFileFilter.getDefaultFilterWrappers( project, filters, escapedBackslashesInFilePath,
                                                              this.session, mavenResourcesExecution );
            }
            catch ( MavenFilteringException e )
            {
                getLog().error( "Fail to build filtering wrappers " + e.getMessage() );
                throw new MojoExecutionException( e.getMessage(), e );
            }
        }
        return filterWrappers;
    }

    private void changeManifestClasspath( EarModule module, File original )
        throws MojoFailureException
    {
        try
        {
            File workDirectory;

            // Handle the case that the destination might be a directory (project-038)
            if ( original.isFile() )
            {
                // Create a temporary work directory
                // MEAR-167 use uri as directory to prevent merging of artifacts with the same artifactId
                workDirectory = new File( new File( generatedDescriptorLocation, "temp" ), module.getUri() );
                workDirectory.mkdirs();
                getLog().debug( "Created a temporary work directory: " + workDirectory.getAbsolutePath() );

                // Unpack the archive to a temporary work directory
                zipUnArchiver.setSourceFile( original );
                zipUnArchiver.setDestDirectory( workDirectory );
                zipUnArchiver.extract();
            }
            else
            {
                workDirectory = original;
            }

            // Create a META-INF/MANIFEST.MF file if it doesn't exist (project-038)
            File metaInfDirectory = new File( workDirectory, "META-INF" );
            boolean newMetaInfCreated = metaInfDirectory.mkdirs();
            if ( newMetaInfCreated )
            {
                getLog().debug( "This project did not have a META-INF directory before, so a new directory was created." );
            }
            File manifestFile = new File( metaInfDirectory, "MANIFEST.MF" );
            boolean newManifestCreated = manifestFile.createNewFile();
            if ( newManifestCreated )
            {
                getLog().debug( "This project did not have a META-INF/MANIFEST.MF file before, so a new file was created." );
            }

            // Read the manifest from disk
            Manifest mf = new Manifest( new FileReader( manifestFile ) );
            Attribute classPath = mf.getMainSection().getAttribute( "Class-Path" );
            List<String> classPathElements = new ArrayList<String>();

            if ( classPath != null )
            {
                classPathElements.addAll( Arrays.asList( classPath.getValue().split( " " ) ) );
            }
            else
            {
                classPath = new Attribute( "Class-Path", "" );
            }

            // Modify the classpath entries in the manifest
            for ( EarModule o : getModules() )
            {
                if ( o instanceof JarModule )
                {
                    JarModule jm = (JarModule) o;

                    if ( module.getLibDir() != null )
                    {
                        //MEAR-189:
                        //We use the original name, cause in case of fileNameMapping to no-version/full 
                        //we coulnd not not delete it and it will end up in the resulting EAR and the WAR 
                        //will not be cleaned up.
                        File artifact =
                            new File( new File( workDirectory, module.getLibDir() ), jm.getOriginalBundleFileName() );

                        if ( artifact.exists() )
                        {
                            getLog().debug( " -> Artifact to delete: " + artifact );
                            if ( !artifact.delete() )
                            {
                                getLog().error( "Could not delete '" + artifact + "'" );
                            }
                        }
                    }

                    if ( classPathElements.contains( jm.getBundleFileName() ) )
                    {
                        classPathElements.set( classPathElements.indexOf( jm.getBundleFileName() ), jm.getUri() );
                    }
                    else
                    {
                        classPathElements.add( jm.getUri() );
                    }
                }
            }
            classPath.setValue( StringUtils.join( classPathElements.iterator(), " " ) );
            mf.getMainSection().addConfiguredAttribute( classPath );

            // Write the manifest to disk
            PrintWriter pw = new PrintWriter( manifestFile );
            mf.write( pw );
            pw.close();

            if ( original.isFile() )
            {
                // Pack up the archive again from the work directory
                if ( !original.delete() )
                {
                    getLog().error( "Could not delete original artifact file " + original );
                }

                getLog().debug( "Zipping module" );
                zipArchiver.setDestFile( original );
                zipArchiver.addDirectory( workDirectory );
                zipArchiver.createArchive();
            }
        }
        catch ( ManifestException e )
        {
            throw new MojoFailureException( e.getMessage() );
        }
        catch ( ZipException e )
        {
            throw new MojoFailureException( e.getMessage() );
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( e.getMessage() );
        }
        catch ( ArchiverException e )
        {
            throw new MojoFailureException( e.getMessage() );
        }
    }
}
