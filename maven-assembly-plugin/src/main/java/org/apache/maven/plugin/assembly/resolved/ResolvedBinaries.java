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

import org.apache.maven.plugin.assembly.model.ModuleBinaries;

import java.util.List;

public class ResolvedBinaries
{
    private final ModuleBinaries binaries;

    private final List<ResolvedDependencySet> resolvedDependencySets;

    public ResolvedBinaries( ModuleBinaries binaries, List<ResolvedDependencySet> resolvedDependencySets )
    {
        this.binaries = binaries;
        this.resolvedDependencySets = resolvedDependencySets;
    }

    public static ResolvedBinaries createResolvedBinaries( ModuleBinaries moduleBinaries )
    {
        return new ResolvedBinaries( moduleBinaries, null );
    }

    public ResolvedBinaries withDependencySet(List<ResolvedDependencySet> resolvedDependencySets){
        return new ResolvedBinaries( binaries, resolvedDependencySets );
    }

    public List<ResolvedDependencySet> getResolvedDependencySets()
    {
        return resolvedDependencySets;
    }

    public ModuleBinaries getBinaries()
    {
        return binaries;
    }
}
