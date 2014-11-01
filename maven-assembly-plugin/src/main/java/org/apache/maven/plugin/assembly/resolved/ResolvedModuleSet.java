package org.apache.maven.plugin.assembly.resolved;

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
import org.apache.maven.plugin.assembly.model.ModuleSet;

import java.util.Set;

public class ResolvedModuleSet
{
    private final ModuleSet moduleSet;

    private final Set<Artifact> artifacts;

    private final ResolvedBinaries resolvedBinaries;

    private ResolvedModuleSet( ModuleSet moduleSet, Set<Artifact> artifacts,
                               ResolvedBinaries resolvedBinaries )
    {
        this.moduleSet = moduleSet;
        this.artifacts = artifacts;
        this.resolvedBinaries = resolvedBinaries;
    }

    public static ResolvedModuleSet createResolvedModuleSet( ModuleSet moduleSet )
    {
        return new ResolvedModuleSet( moduleSet, null, null );
    }

    public static ResolvedModuleSet empty()
    {
        return new ResolvedModuleSet( null, null, null );
    }

    public ModuleSet getModuleSet()
    {
        return moduleSet;
    }

    public ResolvedBinaries getResolvedBinaries()
    {
        return resolvedBinaries;
    }

    public ResolvedModuleSet withArtifacts( Set<Artifact> artifacts )
    {
        return new ResolvedModuleSet( moduleSet, artifacts, resolvedBinaries );
    }

    public ResolvedModuleSet withResolvedDependencySets(  ResolvedBinaries resolvedBinaries )
    {
        return new ResolvedModuleSet( moduleSet, artifacts, resolvedBinaries );
    }

    public Set<Artifact> getArtifacts()
    {
        return artifacts;
    }
}
