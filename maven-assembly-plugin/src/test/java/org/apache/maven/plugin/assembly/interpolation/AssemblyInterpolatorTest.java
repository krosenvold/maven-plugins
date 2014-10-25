package org.apache.maven.plugin.assembly.interpolation;

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

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.plugin.assembly.testutils.ConfigSourceStub;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.classextension.EasyMockSupport;

import static org.easymock.EasyMock.expect;

public class AssemblyInterpolatorTest
    extends TestCase
{

    private AssemblyInterpolator interpolator;

    private final AssemblerConfigurationSource configSourceStub = new ConfigSourceStub();

    @Override
    public void setUp()
        throws IOException
    {
        interpolator = new AssemblyInterpolator();

        interpolator.enableLogging( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
    }

    public void testDependencySetOutputFileNameMappingsAreNotInterpolated()
        throws IOException, AssemblyInterpolationException
    {
        final Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        final MavenProject project = new MavenProject( model );

        final Assembly assembly = new Assembly();

        // artifactId is blacklisted, but packaging is not.
        final String outputFileNameMapping = "${artifactId}.${packaging}";

        final DependencySet set = new DependencySet();
        set.setOutputFileNameMapping( outputFileNameMapping );

        assembly.addDependencySet( set );

        final Assembly outputAssembly = interpolator.interpolate( assembly, project, configSourceStub );

        final List<DependencySet> outputDependencySets = outputAssembly.getDependencySets();
        assertEquals( 1, outputDependencySets.size() );

        final DependencySet outputSet = outputDependencySets.get( 0 );

        assertEquals( "${artifactId}.${packaging}", outputSet.getOutputFileNameMapping() );
    }

    public void testDependencySetOutputDirectoryIsNotInterpolated()
        throws IOException, AssemblyInterpolationException
    {
        final Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        final Assembly assembly = new Assembly();

        final String outputDirectory = "${artifactId}.${packaging}";

        final DependencySet set = new DependencySet();
        set.setOutputDirectory( outputDirectory );

        assembly.addDependencySet( set );

        final Assembly outputAssembly =
            interpolator.interpolate( assembly, new MavenProject( model ), configSourceStub );

        final List<DependencySet> outputDependencySets = outputAssembly.getDependencySets();
        assertEquals( 1, outputDependencySets.size() );

        final DependencySet outputSet = outputDependencySets.get( 0 );

        assertEquals( "${artifactId}.${packaging}", outputSet.getOutputDirectory() );
    }

    public void testShouldResolveModelGroupIdInAssemblyId()
        throws AssemblyInterpolationException
    {
        final Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        final Assembly assembly = new Assembly();

        assembly.setId( "assembly.${groupId}" );

        final Assembly result = interpolator.interpolate( assembly, new MavenProject( model ), configSourceStub );

        assertEquals( "assembly.group.id", result.getId() );
    }

    public void testShouldResolveModelPropertyBeforeModelGroupIdInAssemblyId()
        throws AssemblyInterpolationException
    {
        final Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        final Properties props = new Properties();
        props.setProperty( "groupId", "other.id" );

        model.setProperties( props );

        final Assembly assembly = new Assembly();

        assembly.setId( "assembly.${groupId}" );

        final Assembly result = interpolator.interpolate( assembly, new MavenProject( model ), configSourceStub );

        assertEquals( "assembly.other.id", result.getId() );
    }

    public void testShouldResolveContextValueBeforeModelPropertyOrModelGroupIdInAssemblyId()
        throws AssemblyInterpolationException
    {
        final Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        final Properties props = new Properties();
        props.setProperty( "groupId", "other.id" );

        model.setProperties( props );

        final Assembly assembly = new Assembly();

        assembly.setId( "assembly.${groupId}" );

        final EasyMockSupport mm = new EasyMockSupport();

        final MavenSession session = mm.createMock( MavenSession.class );

        final Properties execProps = new Properties();
        execProps.setProperty( "groupId", "still.another.id" );

        expect( session.getExecutionProperties()).andReturn( execProps ).anyTimes();

        expect( session.getUserProperties()).andReturn(  new Properties()).anyTimes();

        final AssemblerConfigurationSource cs = mm.createMock( AssemblerConfigurationSource.class );

        final ArtifactRepository lr =  mm.createMock( ArtifactRepository.class );

        expect( lr.getBasedir()).andReturn(  "/path/to/local/repo").anyTimes();

        expect(cs.getLocalRepository()).andReturn( lr ).anyTimes();

        expect( cs.getMavenSession()).andReturn( session ).anyTimes();

        mm.replayAll();

        final Assembly result = interpolator.interpolate( assembly, new MavenProject( model ), cs );

        assertEquals( "assembly.still.another.id", result.getId() );

        mm.verifyAll();
        mm.resetAll();
    }

    public void testShouldNotTouchUnresolvedExpression()
        throws AssemblyInterpolationException
    {
        final Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        final Assembly assembly = new Assembly();

        assembly.setId( "assembly.${unresolved}" );

        final Assembly result = interpolator.interpolate( assembly, new MavenProject( model ), configSourceStub );

        assertEquals( "assembly.${unresolved}", result.getId() );
    }

    public void testShouldInterpolateMultiDotProjectExpression()
        throws AssemblyInterpolationException
    {
        final Build build = new Build();
        build.setFinalName( "final-name" );

        final Model model = new Model();
        model.setBuild( build );

        final Assembly assembly = new Assembly();

        assembly.setId( "assembly.${project.build.finalName}" );

        final Assembly result = interpolator.interpolate( assembly, new MavenProject( model ), configSourceStub );

        assertEquals( "assembly.final-name", result.getId() );
    }

}
