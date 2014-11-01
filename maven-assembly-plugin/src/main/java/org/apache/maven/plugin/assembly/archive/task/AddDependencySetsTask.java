package org.apache.maven.plugin.assembly.archive.task;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.format.ReaderFormatter;
import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.plugin.assembly.model.UnpackOptions;
import org.apache.maven.plugin.assembly.resolved.ResolvedDependencySet;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugin.assembly.utils.FilterUtils;
import org.apache.maven.plugin.assembly.utils.TypeConversionUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.shared.artifact.filter.ScopeArtifactFilter;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @version $Id$
 */
public class AddDependencySetsTask
{

    private static final List<String> NON_ARCHIVE_DEPENDENCY_TYPES;

    static
    {
        final List<String> nonArch = new ArrayList<String>();

        nonArch.add( "pom" );

        NON_ARCHIVE_DEPENDENCY_TYPES = Collections.unmodifiableList( nonArch );
    }

    private final List<ResolvedDependencySet> dependencySets;

    private final Logger logger;

    private final MavenProject project;

    private MavenProject moduleProject;

    private final MavenProjectBuilder projectBuilder;

    private String defaultOutputDirectory;

    private String defaultOutputFileNameMapping;

    private Artifact moduleArtifact;

    public AddDependencySetsTask( final List<ResolvedDependencySet> dependencySets, final MavenProject project, final MavenProjectBuilder projectBuilder,
                                  final Logger logger )
    {
        this.dependencySets = dependencySets;
        this.project = project;
        this.projectBuilder = projectBuilder;
        this.logger = logger;
    }

    public void execute( final Archiver archiver, final AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        if ( ( dependencySets == null ) || dependencySets.isEmpty() )
        {
            logger.debug( "No dependency sets specified." );
            return;
        }

        @SuppressWarnings( "unchecked" )
        final List<Dependency> deps = project.getDependencies();
        if ( ( deps == null ) || deps.isEmpty() )
        {
            logger.debug( "Project " + project.getId() + " has no dependencies. Skipping dependency set addition." );
        }

        for ( final ResolvedDependencySet dependencySet : dependencySets )
        {
            addDependencySet( dependencySet, archiver, configSource );
        }
    }

    void addDependencySet( final ResolvedDependencySet dependencySet, final Archiver archiver,
                           final AssemblerConfigurationSource configSource )
        throws AssemblyFormattingException, ArchiveCreationException, InvalidAssemblerConfigurationException
    {
        logger.debug( "Processing DependencySet (output=" + dependencySet.getDependencySet().getOutputDirectory() + ")" );

        if ( !dependencySet.getDependencySet().isUseTransitiveDependencies() && dependencySet.getDependencySet().isUseTransitiveFiltering() )
        {
            logger.warn( "DependencySet has nonsensical configuration: useTransitiveDependencies == false "
                + "AND useTransitiveFiltering == true. Transitive filtering flag will be ignored." );
        }

        final Set<Artifact> dependencyArtifacts = filterArtifacts( dependencySet.getDependencySet(),
                                                                   dependencySet.getArtifacts() );

        final UnpackOptions opts = dependencySet.getUnpackOptions();
        if ( dependencySet.isUnpack() && opts != null && ( opts.isFiltered() || opts.getLineEnding() != null ) )
        {
            // find out if we can just ditch this empty block
        }
        else if ( dependencyArtifacts.size() > 1 )
        {
            checkMultiArtifactOutputConfig( dependencySet.getDependencySet() );
        }

        logger.debug( "Adding " + dependencyArtifacts.size() + " dependency artifacts." );

        InputStreamTransformer fileSetTransformers =
            dependencySet.isUnpack() && dependencySet.getUnpackOptions() != null
                ? ReaderFormatter.getFileSetTransformers( configSource, dependencySet.getUnpackOptions().isFiltered(),
                                                          dependencySet.getUnpackOptions().getLineEnding() )
                : null;


        for ( final Artifact depArtifact : dependencyArtifacts )
        {
            MavenProject depProject;
            try
            {
                depProject =
                    projectBuilder.buildFromRepository( depArtifact, configSource.getRemoteRepositories(),
                                                        configSource.getLocalRepository() );
            }
            catch ( final ProjectBuildingException e )
            {
                logger.debug( "Error retrieving POM of module-dependency: " + depArtifact.getId() + "; Reason: "
                    + e.getMessage() + "\n\nBuilding stub project instance." );

                depProject = buildProjectStub( depArtifact );
            }

            if ( NON_ARCHIVE_DEPENDENCY_TYPES.contains( depArtifact.getType() ) )
            {
                addNonArchiveDependency( depArtifact, depProject, dependencySet.getDependencySet(), archiver, configSource );
            }
            else
            {
                addNormalArtifact( dependencySet.getDependencySet(), depArtifact, depProject, archiver, configSource, fileSetTransformers );
            }
        }
    }

    private void checkMultiArtifactOutputConfig( final DependencySet dependencySet )
    {
        String dir = dependencySet.getOutputDirectory();
        if ( dir == null )
        {
            dir = defaultOutputDirectory;
        }

        String mapping = dependencySet.getOutputFileNameMapping();
        if ( mapping == null )
        {
            mapping = defaultOutputFileNameMapping;
        }

        if ( ( dir == null || !dir.contains( "${" ) ) && ( mapping == null || !mapping.contains( "${" ) ) )
        {
            logger.warn( "NOTE: Your assembly specifies a dependencySet that matches multiple artifacts, but specifies a concrete output format. "
                + "THIS MAY RESULT IN ONE OR MORE ARTIFACTS BEING OBSCURED!\n\nOutput directory: '"
                + dir
                + "'\nOutput filename mapping: '" + mapping + "'" );
        }
    }

    private void addNormalArtifact( final DependencySet dependencySet, final Artifact depArtifact,
                                    final MavenProject depProject, final Archiver archiver,
                                    final AssemblerConfigurationSource configSource,
                                    InputStreamTransformer fileSetTransformers )
        throws AssemblyFormattingException, ArchiveCreationException
    {
        logger.debug( "Adding dependency artifact " + depArtifact.getId() + "." );

        final AddArtifactTask task = new AddArtifactTask( depArtifact, logger, fileSetTransformers );

        task.setProject( depProject );
        task.setModuleProject( moduleProject );
        task.setModuleArtifact( moduleArtifact );
        task.setOutputDirectory( dependencySet.getOutputDirectory(), defaultOutputDirectory );
        task.setFileNameMapping( dependencySet.getOutputFileNameMapping(), defaultOutputFileNameMapping );

        final int dirMode = TypeConversionUtils.modeToInt( dependencySet.getDirectoryMode(), logger );
        if ( dirMode != -1 )
        {
            task.setDirectoryMode( dirMode );
        }

        final int fileMode = TypeConversionUtils.modeToInt( dependencySet.getFileMode(), logger );
        if ( fileMode != -1 )
        {
            task.setFileMode( fileMode );
        }

        task.setUnpack( dependencySet.isUnpack() );

        final UnpackOptions opts = dependencySet.getUnpackOptions();
        if ( dependencySet.isUnpack() && ( opts != null ) )
        {
            task.setIncludes( opts.getIncludes() );
            task.setExcludes( opts.getExcludes() );
        }

        task.execute( archiver, configSource );

    }

    private MavenProject buildProjectStub( final Artifact depArtifact )
    {
        final Model model = new Model();
        model.setGroupId( depArtifact.getGroupId() );
        model.setArtifactId( depArtifact.getArtifactId() );
        model.setVersion( depArtifact.getBaseVersion() );
        model.setPackaging( depArtifact.getType() );

        model.setDescription( "Stub for " + depArtifact.getId() );

        final MavenProject project = new MavenProject( model );
        project.setArtifact( depArtifact );

        return project;
    }

    Set<Artifact> filterArtifacts( final DependencySet dependencySet, Set<Artifact> resolvedArtifacts )
        throws InvalidAssemblerConfigurationException
    {
        final Set<Artifact> dependencyArtifacts = new LinkedHashSet<Artifact>();
        if ( resolvedArtifacts != null )
        {
            dependencyArtifacts.addAll( resolvedArtifacts );
        }

        if ( dependencySet.isUseProjectArtifact() )
        {
            final Artifact projectArtifact = project.getArtifact();
            if ( ( projectArtifact != null ) && ( projectArtifact.getFile() != null ) )
            {
                dependencyArtifacts.add( projectArtifact );
            }
            else
            {
                logger.warn( "Cannot include project artifact: " + projectArtifact
                    + "; it doesn't have an associated file or directory." );
            }
        }

        if ( dependencySet.isUseProjectAttachments() )
        {
            @SuppressWarnings( "unchecked" )
            final List<Artifact> attachments = project.getAttachedArtifacts();
            if ( attachments != null )
            {
                for ( final Artifact attachment : attachments )
                {
                    if ( attachment.getFile() != null )
                    {
                        dependencyArtifacts.add( attachment );
                    }
                    else
                    {
                        logger.warn( "Cannot include attached artifact: " + project.getId() + " for project: "
                            + project.getId() + "; it doesn't have an associated file or directory." );
                    }
                }
            }
        }

        if ( dependencySet.isUseTransitiveFiltering() )
        {
            logger.debug( "Filtering dependency artifacts USING transitive dependency path information." );
        }
        else
        {
            logger.debug( "Filtering dependency artifacts WITHOUT transitive dependency path information." );
        }

        final ScopeArtifactFilter filter = new ScopeArtifactFilter( dependencySet.getScope() );

        FilterUtils.filterArtifacts( dependencyArtifacts, dependencySet.getIncludes(), dependencySet.getExcludes(),
                                     dependencySet.isUseStrictFiltering(), dependencySet.isUseTransitiveFiltering(),
                                     logger, filter );

        return dependencyArtifacts;
    }

    void addNonArchiveDependency( final Artifact depArtifact, final MavenProject depProject,
                                  final DependencySet dependencySet, final Archiver archiver,
                                  final AssemblerConfigurationSource configSource )
        throws AssemblyFormattingException, ArchiveCreationException
    {
        final File source = depArtifact.getFile();

        String outputDirectory = dependencySet.getOutputDirectory();

        outputDirectory =
            AssemblyFormatUtils.getOutputDirectory( outputDirectory, configSource.getProject(), moduleProject,
                                                    depProject, depProject.getBuild().getFinalName(), configSource );

        final String destName =
            AssemblyFormatUtils.evaluateFileNameMapping( dependencySet.getOutputFileNameMapping(), depArtifact,
                                                         configSource.getProject(), moduleProject, moduleArtifact,
                                                         depProject, configSource );

        String target;

        // omit the last char if ends with / or \\
        if ( outputDirectory.endsWith( "/" ) || outputDirectory.endsWith( "\\" ) )
        {
            target = outputDirectory + destName;
        }
        else
        {
            target = outputDirectory + "/" + destName;
        }

        try
        {
            final int mode = TypeConversionUtils.modeToInt( dependencySet.getFileMode(), logger );
            if ( mode > -1 )
            {
                archiver.addFile( source, target, mode );
            }
            else
            {
                archiver.addFile( source, target );
            }
        }
        catch ( final ArchiverException e )
        {
            throw new ArchiveCreationException( "Error adding file to archive: " + e.getMessage(), e );
        }
    }

    public List<DependencySet> getDependencySets()
    {
        List<DependencySet> result = new ArrayList<DependencySet>(  );
        for ( ResolvedDependencySet dependencySet : dependencySets )
        {
            result.add( dependencySet.getDependencySet());
        }
        return result;
    }

    public Logger getLogger()
    {
        return logger;
    }

    public void setDefaultOutputDirectory( final String defaultOutputDirectory )
    {
        this.defaultOutputDirectory = defaultOutputDirectory;
    }

    public void setDefaultOutputFileNameMapping( final String defaultOutputFileNameMapping )
    {
        this.defaultOutputFileNameMapping = defaultOutputFileNameMapping;
    }

    public void setModuleProject( final MavenProject moduleProject )
    {
        this.moduleProject = moduleProject;
    }

    public void setModuleArtifact( final Artifact moduleArtifact )
    {
        this.moduleArtifact = moduleArtifact;
    }
}
